package ConnectionDB;

import model.Vote;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complete optimized ConnectionDB implementation using HikariCP
 * Designed for high-performance operations with 100M citizen records
 * MAINTAINS ALL EXISTING FUNCTIONALITY with enhanced performance
 */
public class ConnectionDB implements ConnectionDBinterface {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionDB.class);
    private static HikariDataSource dataSource;

    // Static initialization of connection pool
    static {
        try {
            HikariConfig config = new HikariConfig();

            // Database connection settings
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/votaciones");
            config.setUsername("postgres");
            config.setPassword("postgres");
            config.setDriverClassName("org.postgresql.Driver");

            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);


            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("defaultRowFetchSize", "1000");


            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public ConnectionDB() {
        logger.info("ConnectionDB instance created using HikariCP pool");
    }

    /**
     * Get a connection from the pool
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // =================== EXISTING METHODS - YOUR ORIGINAL FUNCTIONALITY ===================

    @Override
    public void storeElection(int id, String name, Date start, Date end, String status) {
        String sql = "INSERT INTO elecciones (id, nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setTimestamp(3, new Timestamp(start.getTime()));
            stmt.setTimestamp(4, new Timestamp(end.getTime()));
            stmt.setString(5, status);

            int rowsAffected = stmt.executeUpdate();
            logger.debug("Election stored: {} (rows affected: {})", name, rowsAffected);

        } catch (SQLException e) {
            logger.error("Error storing election: {}", name, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error guardando la elección: " + e.getMessage());
        }
    }

    @Override
    public void storeCandidate(int id, String name, String party, int electionId) {
        String sql = "INSERT INTO candidatos (id, nombre, partido, eleccion_id) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, party);
            stmt.setInt(4, electionId);

            int rowsAffected = stmt.executeUpdate();
            logger.debug("Candidate stored: {} (rows affected: {})", name, rowsAffected);

        } catch (SQLException e) {
            logger.error("Error storing candidate: {}", name, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error guardando el candidato: " + e.getMessage());
        }
    }

    @Override
    public Map<Integer, Integer> getVotesPerCandidate(int electionId) {
        Map<Integer, Integer> result = new HashMap<>();
        String sql = "SELECT candidato_id, COUNT(*) AS total_votos " +
                "FROM votos WHERE election_id = ? GROUP BY candidato_id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int candidateId = rs.getInt("candidato_id");
                    int totalVotes = rs.getInt("total_votos");
                    result.put(candidateId, totalVotes);
                }
            }

            logger.info("Retrieved votes for {} candidates in election {}", result.size(), electionId);

        } catch (SQLException e) {
            logger.error("Error getting votes per candidate for election: {}", electionId, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error obteniendo votos por candidato: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Map<Integer, Integer>> getVotesPerCandidateGroupedByMachine(int electionId) {
        Map<String, Map<Integer, Integer>> result = new HashMap<>();
        String sql = "SELECT machine_id, candidato_id, COUNT(*) AS total_votos " +
                "FROM votos WHERE election_id = ? GROUP BY machine_id, candidato_id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String machineId = rs.getString("machine_id");
                    int candidateId = rs.getInt("candidato_id");
                    int totalVotes = rs.getInt("total_votos");

                    result.putIfAbsent(machineId, new HashMap<>());
                    result.get(machineId).put(candidateId, totalVotes);
                }
            }

            logger.info("Retrieved votes for {} machines in election {}", result.size(), electionId);

        } catch (SQLException e) {
            logger.error("Error getting votes per candidate grouped by machine for election: {}", electionId, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error obteniendo votos por candidato y máquina: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getCandidateNameById(Integer key) {
        String sql = "SELECT nombre FROM candidatos WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, key);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }

        } catch (SQLException e) {
            logger.error("Error getting candidate name for ID: {}", key, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error obteniendo nombre del candidato con ID " + key + ": " + e.getMessage());
        }

        return "Candidato desconocido (ID: " + key + ")";
    }

    @Override
    public void storeVote(Vote vote) {
        String sql = "INSERT INTO votos (machine_id, candidato_id, fecha, election_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, vote.machineId);
            stmt.setInt(2, Integer.parseInt(vote.vote));  // FK al candidato
            stmt.setTimestamp(3, new Timestamp(vote.date));
            stmt.setInt(4, vote.electionId);

            int rowsAffected = stmt.executeUpdate();
            logger.debug("Vote stored for machine {} (rows affected: {})", vote.machineId, rowsAffected);

        } catch (SQLException e) {
            logger.error("Error storing vote for machine: {}", vote.machineId, e);
            // Keep original behavior - don't throw exception, just log
            System.err.println("Error al guardar el voto en la base de datos: " + e.getMessage());
        }
    }

    // =================== NEW METHODS FOR VOTING MACHINE CONFIGURATION ===================

    @Override
    public List<Map<String, Object>> getCitizensByMesa(int mesaId) {
        List<Map<String, Object>> citizens = new ArrayList<>();
        String sql = "SELECT id, documento, nombre, apellido FROM ciudadano WHERE mesa_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("id", rs.getInt("id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));
                    citizens.add(citizen);
                }
            }

            logger.debug("Retrieved {} citizens for mesa {}", citizens.size(), mesaId);

        } catch (SQLException e) {
            logger.error("Error getting citizens for mesa: {}", mesaId, e);
            System.err.println("Error obteniendo ciudadanos para mesa " + mesaId + ": " + e.getMessage());
        }

        return citizens;
    }

    @Override
    public Map<String, Object> getMesaConfiguration(int mesaId) {
        String sql = "SELECT * FROM vista_configuracion_mesa WHERE mesa_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> config = new HashMap<>();
                    config.put("mesa_id", rs.getInt("mesa_id"));
                    config.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    config.put("puesto_id", rs.getInt("puesto_id"));
                    config.put("puesto_nombre", rs.getString("puesto_nombre"));
                    config.put("puesto_direccion", rs.getString("puesto_direccion"));
                    config.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    config.put("municipio_id", rs.getInt("municipio_id"));
                    config.put("municipio_nombre", rs.getString("municipio_nombre"));
                    config.put("departamento_id", rs.getInt("departamento_id"));
                    config.put("departamento_nombre", rs.getString("departamento_nombre"));
                    config.put("total_ciudadanos", rs.getInt("total_ciudadanos"));

                    logger.debug("Retrieved configuration for mesa {}", mesaId);
                    return config;
                }
            }

        } catch (SQLException e) {
            logger.error("Error getting mesa configuration for: {}", mesaId, e);
            System.err.println("Error obteniendo configuración para mesa " + mesaId + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    public Map<Integer, List<Map<String, Object>>> getCitizensByMesaBatch(List<Integer> mesaIds) {
        if (mesaIds == null || mesaIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Integer, List<Map<String, Object>>> result = new ConcurrentHashMap<>();

        // Initialize empty lists for all mesa IDs
        for (Integer mesaId : mesaIds) {
            result.put(mesaId, new ArrayList<>());
        }

        String sql = "SELECT mesa_id, id, documento, nombre, apellido " +
                "FROM ciudadano WHERE mesa_id = ANY(?) ORDER BY mesa_id, id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Array mesaIdsArray = conn.createArrayOf("integer", mesaIds.toArray());
            stmt.setArray(1, mesaIdsArray);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int mesaId = rs.getInt("mesa_id");

                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("id", rs.getInt("id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));

                    result.get(mesaId).add(citizen);
                }
            }

            int totalCitizens = result.values().stream().mapToInt(List::size).sum();
            logger.info("Retrieved {} citizens for {} mesas in batch", totalCitizens, mesaIds.size());

        } catch (SQLException e) {
            logger.error("Error getting citizens for mesa batch: {}", mesaIds, e);
            System.err.println("Error obteniendo ciudadanos para lote de mesas: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Integer> getAllMesaIds() {
        List<Integer> mesaIds = new ArrayList<>();
        String sql = "SELECT id FROM mesa_votacion ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                mesaIds.add(rs.getInt("id"));
            }

            logger.info("Retrieved {} mesa IDs", mesaIds.size());

        } catch (SQLException e) {
            logger.error("Error getting all mesa IDs", e);
            System.err.println("Error obteniendo todos los IDs de mesa: " + e.getMessage());
        }

        return mesaIds;
    }

    @Override
    public List<Integer> getMesaIdsByDepartment(int departmentId) {
        List<Integer> mesaIds = new ArrayList<>();
        String sql = """
            SELECT mv.id
            FROM mesa_votacion mv
            JOIN puesto_votacion pv ON mv.puesto_id = pv.id
            JOIN municipio m ON pv.municipio_id = m.id
            WHERE m.departamento_id = ?
            ORDER BY mv.id
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, departmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mesaIds.add(rs.getInt("id"));
                }
            }

            logger.info("Retrieved {} mesa IDs for department {}", mesaIds.size(), departmentId);

        } catch (SQLException e) {
            logger.error("Error getting mesa IDs for department: {}", departmentId, e);
            System.err.println("Error obteniendo IDs de mesa para departamento " + departmentId + ": " + e.getMessage());
        }

        return mesaIds;
    }

    @Override
    public Map<String, Object> getElectionInfo(int electionId) {
        String sql = "SELECT id, nombre, fecha_inicio, fecha_fin, estado FROM elecciones WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> election = new HashMap<>();
                    election.put("id", rs.getInt("id"));
                    election.put("nombre", rs.getString("nombre"));
                    election.put("fecha_inicio", rs.getTimestamp("fecha_inicio"));
                    election.put("fecha_fin", rs.getTimestamp("fecha_fin"));
                    election.put("estado", rs.getString("estado"));

                    logger.debug("Retrieved election info for ID: {}", electionId);
                    return election;
                }
            }

        } catch (SQLException e) {
            logger.error("Error getting election info for ID: {}", electionId, e);
            System.err.println("Error obteniendo información de elección " + electionId + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Map<String, Object>> getCandidatesByElection(int electionId) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        String sql = "SELECT id, nombre, partido FROM candidatos WHERE eleccion_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> candidate = new HashMap<>();
                    candidate.put("id", rs.getInt("id"));
                    candidate.put("nombre", rs.getString("nombre"));
                    candidate.put("partido", rs.getString("partido"));
                    candidates.add(candidate);
                }
            }

            logger.debug("Retrieved {} candidates for election {}", candidates.size(), electionId);

        } catch (SQLException e) {
            logger.error("Error getting candidates for election: {}", electionId, e);
            System.err.println("Error obteniendo candidatos para elección " + electionId + ": " + e.getMessage());
        }

        return candidates;
    }

    // =================== MONITORING AND PERFORMANCE METHODS ===================

    @Override
    public String getPoolStats() {
        if (dataSource != null) {
            return String.format(
                    "Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                    dataSource.getHikariPoolMXBean().getActiveConnections(),
                    dataSource.getHikariPoolMXBean().getIdleConnections(),
                    dataSource.getHikariPoolMXBean().getTotalConnections(),
                    dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "Pool not initialized";
    }

    @Override
    public boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Get basic table counts
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM ciudadano")) {
                if (rs.next()) {
                    metrics.put("total_citizens", rs.getLong("count"));
                }
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM mesa_votacion")) {
                if (rs.next()) {
                    metrics.put("total_mesas", rs.getLong("count"));
                }
            }

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM puesto_votacion")) {
                if (rs.next()) {
                    metrics.put("total_puestos", rs.getLong("count"));
                }
            }

            // Add connection pool metrics
            if (dataSource != null) {
                metrics.put("pool_active_connections", dataSource.getHikariPoolMXBean().getActiveConnections());
                metrics.put("pool_idle_connections", dataSource.getHikariPoolMXBean().getIdleConnections());
                metrics.put("pool_total_connections", dataSource.getHikariPoolMXBean().getTotalConnections());
                metrics.put("pool_threads_awaiting", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }

            // Add timestamp
            metrics.put("timestamp", new Date());

            logger.debug("Performance metrics collected: {}", metrics);

        } catch (SQLException e) {
            logger.error("Error collecting performance metrics", e);
            metrics.put("error", "Failed to collect metrics: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Shutdown the connection pool - call this when application closes
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }
}