package com.eva.workflow.approval.application.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.eva.workflow.approval.domain.policy.service.EmbeddingPort;
import com.eva.workflow.approval.infrastructure.persistence.jpa.entity.PolicyChunkEntity;
import com.eva.workflow.approval.infrastructure.persistence.jpa.repository.PolicyChunkRepository;

import lombok.RequiredArgsConstructor;

/**
 * Loads the company policy document into the pgvector store on startup. Each {@code ## }
 * heading becomes one retrievable chunk. Idempotent: skips chunks already present
 * (by locale + content hash), so restarts and shared test containers don't duplicate.
 *
 * <p>Only wired when a Gemini API key is configured (same condition as the embedding
 * adapter it depends on), so a LOCAL-only deployment without Gemini still boots — the
 * policy feature is simply absent. The {@code app.ai.policy.auto-ingest} flag (default
 * true) further controls whether ingestion runs on startup. Embedding uses whichever
 * {@link EmbeddingPort} is wired (Gemini in prod, a deterministic fake in tests).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ai.gemini", name = "api-key")
public class PolicyIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyIngestionService.class);
    private static final String LOCALE = "zh";
    private static final String DOCUMENT = "policy/leave-policy.zh.md";

    private final PolicyChunkRepository policyChunkRepository;
    private final EmbeddingPort embeddingPort;

    @Value("${app.ai.policy.auto-ingest:true}")
    private boolean autoIngest;

    @Override
    public void run(ApplicationArguments args) {
        if (autoIngest) {
            ingest();
        }
    }

    @Transactional
    public void ingest() {
        List<Chunk> chunks;
        try {
            chunks = parse(readDocument());
        } catch (Exception e) {
            log.error("Policy ingestion skipped: cannot read {}: {}", DOCUMENT, e.getMessage());
            return;
        }

        int inserted = 0;
        for (Chunk chunk : chunks) {
            String hash = sha256(chunk.content());
            if (policyChunkRepository.existsByLocaleAndContentHash(LOCALE, hash)) {
                continue;
            }
            float[] embedding = embeddingPort.embed(chunk.section() + "\n" + chunk.content());
            policyChunkRepository.save(PolicyChunkEntity.create(
                    LOCALE, chunk.section(), chunk.content(), hash, embedding, embeddingPort.modelName()));
            inserted++;
        }
        if (inserted > 0) {
            log.info("Policy ingestion: inserted {} new chunk(s) from {}", inserted, DOCUMENT);
        }
    }

    private String readDocument() throws Exception {
        try (var in = new ClassPathResource(DOCUMENT).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    /** Splits on {@code ## } headings; ignores the title and any text before the first heading. */
    private List<Chunk> parse(String markdown) {
        List<Chunk> chunks = new ArrayList<>();
        String section = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\n")) {
            if (line.startsWith("## ")) {
                flush(chunks, section, body);
                section = line.substring(3).trim();
                body.setLength(0);
            } else if (section != null && !line.startsWith(">")) {
                if (!line.isBlank()) {
                    body.append(line.trim()).append("\n");
                }
            }
        }
        flush(chunks, section, body);
        return chunks;
    }

    private void flush(List<Chunk> chunks, String section, StringBuilder body) {
        if (section != null && !body.isEmpty()) {
            chunks.add(new Chunk(section, body.toString().trim()));
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

    private record Chunk(String section, String content) {
    }
}
