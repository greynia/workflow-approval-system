package com.eva.workflow.approval.application.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.domain.policy.service.EmbeddingTaskType;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PolicyChunkEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkRepository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;

/**
 * Loads the company policy document into the pgvector store on startup. Each {@code ## }
 * heading becomes one retrievable chunk. Idempotent: skips chunks already present
 * (by locale + content hash), so restarts and shared test containers don't duplicate.
 *
 * <p>Only wired when a Gemini API key is configured (same condition as the embedding
 * adapter it depends on), so a LOCAL-only deployment without Gemini still boots — the
 * policy feature is simply absent. The {@code app.ai.policy.auto-ingest} flag controls
 * whether ingestion runs on startup. Embedding uses whichever {@link EmbeddingPort} is
 * wired (Gemini in prod, a deterministic fake in tests).
 *
 * <p>Chunking is section-aware first ({@code ## } headings), then any over-long section
 * body is recursively sub-split (char cap + overlap) so messy real-world documents don't
 * collapse into one giant, semantically-diluted chunk. Each run wipes the locale's chunks
 * and re-indexes, so editing the source document never leaves stale rows behind.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class PolicyIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);
    private static final String LOCALE = "zh";
    private static final String DOCUMENT = "policy/leave-policy.zh.md";
    private static final String SOURCE = DOCUMENT.substring(DOCUMENT.lastIndexOf('/') + 1);

    private final PolicyChunkRepository policyChunkRepository;
    private final EmbeddingPort embeddingPort;
    private final PolicyProperties properties;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.autoIngest()) {
            ingest();
        }
    }

    public void ingest() {
        List<Section> sections;
        try {
            sections = parse(readDocument());
        } catch (Exception e) {
            log.error("Policy ingestion skipped: cannot read {}: {}", DOCUMENT, e.getMessage());
            return;
        }

        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        for (Section section : sections) {
            List<String> pieces = splitBody(section.body());
            for (int i = 0; i < pieces.size(); i++) {
                String content = pieces.get(i);
                String hash = sha256(SOURCE + "\n" + section.name() + "\n" + i + "\n" + content);
                float[] embedding = embeddingPort.embed(
                        section.name() + "\n" + content, EmbeddingTaskType.RETRIEVAL_DOCUMENT);
                embeddedChunks.add(new EmbeddedChunk(section.name(), i, content, hash, embedding));
            }
        }

        Integer inserted = transactionTemplate.execute(status -> {
            policyChunkRepository.deleteByLocale(LOCALE);
            int count = 0;
            for (EmbeddedChunk chunk : embeddedChunks) {
                policyChunkRepository.save(PolicyChunkEntity.create(
                        LOCALE, chunk.section(), SOURCE, chunk.chunkIndex(), chunk.content(), chunk.contentHash(),
                        chunk.embedding(), embeddingPort.modelName()));
                count++;
            }
            return count;
        });
        log.info("Policy ingestion: re-indexed {} chunk(s) from {}", inserted, DOCUMENT);
    }

    /** One chunk per section if it fits the cap; otherwise recursive char-based sub-splitting. */
    private List<String> splitBody(String body) {
        if (body.length() <= properties.chunkMaxChars()) {
            return List.of(body);
        }
        DocumentSplitter splitter =
                DocumentSplitters.recursive(properties.chunkMaxChars(), properties.chunkOverlapChars());
        return splitter.split(Document.from(body)).stream()
                .map(TextSegment::text)
                .toList();
    }

    private String readDocument() throws Exception {
        try (var in = new ClassPathResource(DOCUMENT).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    /** Splits on {@code ## } headings; ignores the title and any text before the first heading. */
    private List<Section> parse(String markdown) {
        List<Section> sections = new ArrayList<>();
        String name = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\n")) {
            if (line.startsWith("## ")) {
                flush(sections, name, body);
                name = line.substring(3).trim();
                body.setLength(0);
            } else if (name != null && !line.startsWith(">")) {
                if (!line.isBlank()) {
                    body.append(line.trim()).append("\n");
                }
            }
        }
        flush(sections, name, body);
        return sections;
    }

    private void flush(List<Section> sections, String name, StringBuilder body) {
        if (name != null && !body.isEmpty()) {
            sections.add(new Section(name, body.toString().trim()));
        }
    }

    private String sha256(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record Section(String name, String body) {
    }

    private record EmbeddedChunk(
            String section,
            int chunkIndex,
            String content,
            String contentHash,
            float[] embedding
    ) {
    }
}
