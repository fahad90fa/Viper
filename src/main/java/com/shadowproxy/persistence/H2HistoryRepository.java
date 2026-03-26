package com.shadowproxy.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowproxy.config.AppConfig;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class H2HistoryRepository implements HistoryRepository {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String jdbcUrl;
    private final CopyOnWriteArrayList<HistoryListener> listeners = new CopyOnWriteArrayList<>();

    public H2HistoryRepository(AppConfig appConfig) {
        try {
            Files.createDirectories(appConfig.dataDirectory());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create data directory", e);
        }
        this.jdbcUrl = "jdbc:h2:" + appConfig.dataDirectory().resolve("shadowproxy").toAbsolutePath();
        initializeSchema();
    }

    @Override
    public synchronized void save(HttpExchangeRecord exchange) {
        String sql = """
                MERGE INTO history (id, source_tool, req_method, req_url, req_headers_json, req_body_b64, req_captured_at,
                                    res_status, res_reason, res_headers_json, res_body_b64, res_captured_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, exchange.id().toString());
            ps.setString(2, exchange.sourceTool());
            ps.setString(3, exchange.request().method());
            ps.setString(4, exchange.request().url());
            ps.setString(5, toJson(exchange.request().headers()));
            ps.setString(6, encodeBody(exchange.request().body()));
            ps.setTimestamp(7, Timestamp.from(exchange.request().capturedAt()));
            ps.setInt(8, exchange.response() != null ? exchange.response().statusCode() : 0);
            ps.setString(9, exchange.response() != null ? exchange.response().reasonPhrase() : "");
            ps.setString(10, toJson(exchange.response() != null ? exchange.response().headers() : java.util.Map.of()));
            ps.setString(11, encodeBody(exchange.response() != null ? exchange.response().body() : new byte[0]));
            ps.setTimestamp(12, Timestamp.from(exchange.response() != null ? exchange.response().capturedAt() : Instant.now()));
            ps.executeUpdate();
            listeners.forEach(listener -> listener.onExchangeSaved(exchange));
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save exchange", e);
        }
    }

    @Override
    public synchronized List<HttpExchangeRecord> findAll() {
        String sql = "SELECT * FROM history ORDER BY req_captured_at ASC";
        try (Connection c = connection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            java.util.ArrayList<HttpExchangeRecord> out = new java.util.ArrayList<>();
            while (rs.next()) {
                out.add(fromRow(rs));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to query history", e);
        }
    }

    @Override
    public synchronized Optional<HttpExchangeRecord> findById(UUID id) {
        String sql = "SELECT * FROM history WHERE id = ?";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(fromRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to query history by id", e);
        }
    }

    @Override
    public synchronized void clear() {
        try (Connection c = connection(); Statement st = c.createStatement()) {
            st.execute("DELETE FROM history");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to clear history", e);
        }
    }

    @Override
    public void addListener(HistoryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(HistoryListener listener) {
        listeners.remove(listener);
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private void initializeSchema() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS history (
                    id VARCHAR(64) PRIMARY KEY,
                    source_tool VARCHAR(64) NOT NULL,
                    req_method VARCHAR(16) NOT NULL,
                    req_url CLOB NOT NULL,
                    req_headers_json CLOB NOT NULL,
                    req_body_b64 CLOB NOT NULL,
                    req_captured_at TIMESTAMP NOT NULL,
                    res_status INT NOT NULL,
                    res_reason VARCHAR(256) NOT NULL,
                    res_headers_json CLOB NOT NULL,
                    res_body_b64 CLOB NOT NULL,
                    res_captured_at TIMESTAMP NOT NULL
                )
                """;
        try (Connection c = connection(); Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize history schema", e);
        }
    }

    private HttpExchangeRecord fromRow(ResultSet rs) throws SQLException {
        HttpRequestRecord req = new HttpRequestRecord(
                rs.getString("req_method"),
                rs.getString("req_url"),
                readMap(rs.getString("req_headers_json")),
                decodeBody(rs.getString("req_body_b64")),
                rs.getTimestamp("req_captured_at").toInstant()
        );
        HttpResponseRecord res = new HttpResponseRecord(
                rs.getInt("res_status"),
                rs.getString("res_reason"),
                readMap(rs.getString("res_headers_json")),
                decodeBody(rs.getString("res_body_b64")),
                rs.getTimestamp("res_captured_at").toInstant()
        );
        return new HttpExchangeRecord(
                UUID.fromString(rs.getString("id")),
                req,
                res,
                rs.getString("source_tool")
        );
    }

    private String toJson(java.util.Map<String, String> headers) {
        try {
            return OBJECT_MAPPER.writeValueAsString(headers);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize headers", e);
        }
    }

    private java.util.Map<String, String> readMap(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to deserialize headers", e);
        }
    }

    private String encodeBody(byte[] body) {
        return Base64.getEncoder().encodeToString(body == null ? new byte[0] : body);
    }

    private byte[] decodeBody(String b64) {
        if (b64 == null || b64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(b64);
    }
}
