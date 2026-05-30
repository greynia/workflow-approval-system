-- V19: Policy RAG v2 chunking metadata.
-- The chunking strategy changed (section-aware recursive splitting with size cap + overlap),
-- so old V18-format rows are obsolete. They are dropped here and re-ingested at startup by
-- PolicyIngestionService with the new boundaries. New columns:
--   source      -- origin document filename, returned as part of a citation
--   chunk_index -- 0-based position of the chunk within its section

TRUNCATE TABLE policy_chunks;

ALTER TABLE policy_chunks ADD COLUMN source VARCHAR(200) NOT NULL;
ALTER TABLE policy_chunks ADD COLUMN chunk_index INTEGER NOT NULL;
