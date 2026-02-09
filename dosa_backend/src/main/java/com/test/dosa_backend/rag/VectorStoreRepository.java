package com.test.dosa_backend.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class VectorStoreRepository {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public VectorStoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureSchema();
    }

    private void ensureSchema() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS chunk_embeddings (" +
                            "chunk_id UUID PRIMARY KEY REFERENCES document_chunks(id) ON DELETE CASCADE," +
                            "embedding vector NOT NULL," +
                            "model VARCHAR(255)," +
                            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                            ")"
            );
        } catch (Exception e) {
            log.warn("Failed to ensure vector schema; ingest may fail until schema is created manually.", e);
        }
    }

    public void upsertEmbedding(UUID chunkId, float[] embedding, String model) {
        String vectorLiteral = toVectorLiteral(embedding);
        String sql = "INSERT INTO chunk_embeddings (chunk_id, embedding, model, created_at) " +
                "VALUES (?, '" + vectorLiteral + "'::vector, ?, NOW()) " +
                "ON CONFLICT (chunk_id) DO UPDATE SET embedding = EXCLUDED.embedding, model = EXCLUDED.model";
        jdbcTemplate.update(
                sql,
                chunkId,
                model
        );
    }

    public List<SearchHit> similaritySearch(float[] queryEmbedding, int topK, List<UUID> documentIds) {
        if (topK <= 0) topK = 5;

        String queryVector = "'" + toVectorLiteral(queryEmbedding) + "'::vector";
        String sql = "SELECT c.id AS chunk_id, c.document_id, c.chunk_index, c.content_text, c.meta, d.title AS doc_title, " +
                "(e.embedding <=> " + queryVector + ") AS distance " +
                "FROM document_chunks c " +
                "JOIN chunk_embeddings e ON e.chunk_id = c.id " +
                "JOIN documents d ON d.id = c.document_id ";

        // We intentionally avoid JDBC uuid[] binding quirks by generating an IN (...) clause.
        List<Object> params = new java.util.ArrayList<>();

        if (documentIds != null && !documentIds.isEmpty()) {
            sql += "WHERE c.document_id IN (";
            for (int i = 0; i < documentIds.size(); i++) {
                if (i > 0) sql += ",";
                sql += "?";
                params.add(documentIds.get(i));
            }
            sql += ") ";
        }

        sql += "ORDER BY e.embedding <=> " + queryVector + " LIMIT ?";
        params.add(topK);

        return jdbcTemplate.query(sql, params.toArray(), new SearchHitRowMapper());
    }

    private static class SearchHitRowMapper implements RowMapper<SearchHit> {
        @Override
        public SearchHit mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SearchHit(
                    rs.getObject("chunk_id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getInt("chunk_index"),
                    rs.getString("content_text"),
                    rs.getString("meta"),
                    rs.getString("doc_title"),
                    rs.getDouble("distance")
            );
        }
    }

    public record SearchHit(
            UUID chunkId,
            UUID documentId,
            int chunkIndex,
            String contentText,
            String metaJson,
            String documentTitle,
            double distance
    ) {}

    private String toVectorLiteral(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding vector must not be empty.");
        }
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.US, "%.8f", embedding[i]));
        }
        sb.append(']');
        return sb.toString();
    }
}
