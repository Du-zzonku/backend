package com.test.dosa_backend.rag;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorStoreRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        // Register pgvector types once (best-effort)
        try (Connection conn = dataSource.getConnection()) {
            PGvector.registerTypes(conn);
        } catch (Exception ignored) {
            // If this fails (e.g., H2 tests), we still allow the app to start.
        }
    }

    public void upsertEmbedding(UUID chunkId, float[] embedding, String model) {
        jdbcTemplate.update(
                "INSERT INTO chunk_embeddings (chunk_id, embedding, model, created_at) VALUES (?,?,?,?) " +
                        "ON CONFLICT (chunk_id) DO UPDATE SET embedding = EXCLUDED.embedding, model = EXCLUDED.model",
                chunkId,
                new PGvector(embedding),
                model,
                Instant.now()
        );
    }

    public List<SearchHit> similaritySearch(float[] queryEmbedding, int topK, List<UUID> documentIds) {
        if (topK <= 0) topK = 5;

        String sql = "SELECT c.id AS chunk_id, c.document_id, c.chunk_index, c.content_text, c.meta, d.title AS doc_title, " +
                "(e.embedding <=> ?) AS distance " +
                "FROM document_chunks c " +
                "JOIN chunk_embeddings e ON e.chunk_id = c.id " +
                "JOIN documents d ON d.id = c.document_id ";

        // We intentionally avoid JDBC uuid[] binding quirks by generating an IN (...) clause.
        List<Object> params = new java.util.ArrayList<>();
        params.add(new PGvector(queryEmbedding));

        if (documentIds != null && !documentIds.isEmpty()) {
            sql += "WHERE c.document_id IN (";
            for (int i = 0; i < documentIds.size(); i++) {
                if (i > 0) sql += ",";
                sql += "?";
                params.add(documentIds.get(i));
            }
            sql += ") ";
        }

        sql += "ORDER BY e.embedding <=> ? LIMIT ?";
        params.add(new PGvector(queryEmbedding));
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
}
