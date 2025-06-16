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

        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public ConnectionDB() {
    }

    /**
     * Get a connection from the pool
     */
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


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

        } catch (SQLException e) {
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

        } catch (SQLException e) {
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


        } catch (SQLException e) {
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


        } catch (SQLException e) {
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
            stmt.setInt(2, Integer.parseInt(vote.vote));
            stmt.setTimestamp(3, new Timestamp(vote.date));
            stmt.setInt(4, vote.electionId);

            int rowsAffected = stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error al guardar el voto en la base de datos: " + e.getMessage());
        }
    }


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


        } catch (SQLException e) {
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

                    return config;
                }
            }

        } catch (SQLException e) {
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

        } catch (SQLException e) {
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


        } catch (SQLException e) {
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


        } catch (SQLException e) {
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

                    return election;
                }
            }

        } catch (SQLException e) {
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


        } catch (SQLException e) {
            System.err.println("Error obteniendo candidatos para elección " + electionId + ": " + e.getMessage());
        }

        return candidates;
    }

    @Override
    public List<Map<String, Object>> getAllDepartments() {
        List<Map<String, Object>> departments = new ArrayList<>();
        String sql = "SELECT id, nombre FROM departamento ORDER BY nombre";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> department = new HashMap<>();
                department.put("id", rs.getInt("id"));
                department.put("nombre", rs.getString("nombre"));
                departments.add(department);
            }


        } catch (SQLException e) {
            System.err.println("Error obteniendo departamentos: " + e.getMessage());
        }

        return departments;
    }

    @Override
    public List<Map<String, Object>> getMunicipalitiesByDepartment(int departmentId) {
        List<Map<String, Object>> municipalities = new ArrayList<>();
        String sql = "SELECT id, nombre FROM municipio WHERE departamento_id = ? ORDER BY nombre";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, departmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> municipality = new HashMap<>();
                    municipality.put("id", rs.getInt("id"));
                    municipality.put("nombre", rs.getString("nombre"));
                    municipality.put("departamento_id", departmentId);
                    municipalities.add(municipality);
                }
            }


        } catch (SQLException e) {
            System.err.println("Error obteniendo municipios para departamento " + departmentId + ": " + e.getMessage());
        }

        return municipalities;
    }

    @Override
    public List<Map<String, Object>> getPuestosByMunicipality(int municipalityId) {
        List<Map<String, Object>> puestos = new ArrayList<>();
        String sql = "SELECT id, nombre, direccion, consecutive FROM puesto_votacion WHERE municipio_id = ? ORDER BY consecutive";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, municipalityId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> puesto = new HashMap<>();
                    puesto.put("id", rs.getInt("id"));
                    puesto.put("nombre", rs.getString("nombre"));
                    puesto.put("direccion", rs.getString("direccion"));
                    puesto.put("consecutive", rs.getInt("consecutive"));
                    puesto.put("municipio_id", municipalityId);
                    puestos.add(puesto);
                }
            }


        } catch (SQLException e) {
            System.err.println("Error obteniendo puestos para municipio " + municipalityId + ": " + e.getMessage());
        }

        return puestos;
    }

    @Override
    public Map<String, Object> getElectionConfigurationStats(int electionId) {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = getConnection()) {

            // Get election info
            Map<String, Object> electionInfo = getElectionInfo(electionId);
            if (electionInfo != null) {
                stats.put("electionName", electionInfo.get("nombre"));
                stats.put("electionStatus", electionInfo.get("estado"));
                stats.put("fechaInicio", electionInfo.get("fecha_inicio"));
                stats.put("fechaFin", electionInfo.get("fecha_fin"));
            }

            // Get candidate count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM candidatos WHERE eleccion_id = ?")) {
                stmt.setInt(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("totalCandidates", rs.getInt("count"));
                    }
                }
            }

            // Get total mesas
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM mesa_votacion")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("totalMesas", rs.getInt("count"));
                    }
                }
            }

            // Get total citizens
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM ciudadano")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("totalCitizens", rs.getLong("count"));
                    }
                }
            }

            // Get average citizens per mesa
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT AVG(citizen_count) as avg_citizens FROM (" +
                            "SELECT mesa_id, COUNT(*) as citizen_count FROM ciudadano GROUP BY mesa_id" +
                            ") as mesa_stats")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("avgCitizensPerMesa", rs.getDouble("avg_citizens"));
                    }
                }
            }

            // Add timestamp
            stats.put("timestamp", new Date());


        } catch (SQLException e) {
            stats.put("error", "Failed to collect stats: " + e.getMessage());
        }

        return stats;
    }

    @Override
    public boolean validateElectionDataCompleteness(int electionId) {
        try (Connection conn = getConnection()) {

            // Check if election exists
            Map<String, Object> electionInfo = getElectionInfo(electionId);
            if (electionInfo == null) {
                return false;
            }

            // Check if election has candidates
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM candidatos WHERE eleccion_id = ?")) {
                stmt.setInt(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int candidateCount = rs.getInt("count");
                        if (candidateCount == 0) {
                            return false;
                        }
                    }
                }
            }

            // Check if there are voting tables (mesas)
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM mesa_votacion")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int mesaCount = rs.getInt("count");
                        if (mesaCount == 0) {
                            return false;
                        }
                    }
                }
            }

            // Check if there are citizens assigned to mesas
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) as count FROM ciudadano")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long citizenCount = rs.getLong("count");
                        if (citizenCount == 0) {
                            return false;
                        }
                    }
                }
            }

            // Check election status
            String status = (String) electionInfo.get("estado");
            if (status == null) {
                return false;
            }

            return true;

        } catch (SQLException e) {
            return false;
        }
    }


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


        } catch (SQLException e) {
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
        }
    }



// Add these methods to your ConnectionDB.java class

    @Override
    public Map<String, Object> getCitizenVotingAssignment(String documento) {
        String sql = """
        SELECT 
            c.id as ciudadano_id,
            c.documento,
            c.nombre,
            c.apellido,
            c.mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.id as puesto_id,
            pv.nombre as puesto_nombre,
            pv.direccion as puesto_direccion,
            pv.consecutive as puesto_consecutive,
            m.id as municipio_id,
            m.nombre as municipio_nombre,
            d.id as departamento_id,
            d.nombre as departamento_nombre
        FROM ciudadano c
        JOIN mesa_votacion mv ON c.mesa_id = mv.id
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE c.documento = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, documento);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> assignment = new HashMap<>();
                    assignment.put("ciudadano_id", rs.getInt("ciudadano_id"));
                    assignment.put("documento", rs.getString("documento"));
                    assignment.put("nombre", rs.getString("nombre"));
                    assignment.put("apellido", rs.getString("apellido"));
                    assignment.put("mesa_id", rs.getInt("mesa_id"));
                    assignment.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    assignment.put("puesto_id", rs.getInt("puesto_id"));
                    assignment.put("puesto_nombre", rs.getString("puesto_nombre"));
                    assignment.put("puesto_direccion", rs.getString("puesto_direccion"));
                    assignment.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    assignment.put("municipio_id", rs.getInt("municipio_id"));
                    assignment.put("municipio_nombre", rs.getString("municipio_nombre"));
                    assignment.put("departamento_id", rs.getInt("departamento_id"));
                    assignment.put("departamento_nombre", rs.getString("departamento_nombre"));

                    return assignment;
                }
            }

        } catch (SQLException e) {
        }

        return null;
    }

    @Override
    public List<Map<String, Object>> searchCitizensByName(String nombre, String apellido, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Build dynamic query based on provided parameters
        StringBuilder sqlBuilder = new StringBuilder("""
        SELECT 
            c.id as ciudadano_id,
            c.documento,
            c.nombre,
            c.apellido,
            c.mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.nombre as puesto_nombre,
            m.nombre as municipio_nombre,
            d.nombre as departamento_nombre
        FROM ciudadano c
        JOIN mesa_votacion mv ON c.mesa_id = mv.id
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE 1=1
        """);

        List<Object> parameters = new ArrayList<>();

        if (nombre != null && !nombre.trim().isEmpty()) {
            sqlBuilder.append(" AND UPPER(c.nombre) LIKE UPPER(?)");
            parameters.add("%" + nombre.trim() + "%");
        }

        if (apellido != null && !apellido.trim().isEmpty()) {
            sqlBuilder.append(" AND UPPER(c.apellido) LIKE UPPER(?)");
            parameters.add("%" + apellido.trim() + "%");
        }

        sqlBuilder.append(" ORDER BY c.apellido, c.nombre LIMIT ?");
        parameters.add(limit);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("ciudadano_id", rs.getInt("ciudadano_id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));
                    citizen.put("mesa_id", rs.getInt("mesa_id"));
                    citizen.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    citizen.put("puesto_nombre", rs.getString("puesto_nombre"));
                    citizen.put("municipio_nombre", rs.getString("municipio_nombre"));
                    citizen.put("departamento_nombre", rs.getString("departamento_nombre"));
                    results.add(citizen);
                }
            }


        } catch (SQLException e) {
        }

        return results;
    }

    @Override
    public List<Map<String, Object>> getMesasByPuesto(int puestoId) {
        List<Map<String, Object>> mesas = new ArrayList<>();
        String sql = """
        SELECT 
            mv.id as mesa_id,
            mv.consecutive as mesa_consecutive,
            COUNT(c.id) as total_ciudadanos
        FROM mesa_votacion mv
        LEFT JOIN ciudadano c ON mv.id = c.mesa_id
        WHERE mv.puesto_id = ?
        GROUP BY mv.id, mv.consecutive
        ORDER BY mv.consecutive
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, puestoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> mesa = new HashMap<>();
                    mesa.put("mesa_id", rs.getInt("mesa_id"));
                    mesa.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    mesa.put("total_ciudadanos", rs.getInt("total_ciudadanos"));
                    mesa.put("puesto_id", puestoId);
                    mesas.add(mesa);
                }
            }


        } catch (SQLException e) {
        }

        return mesas;
    }

    @Override
    public Map<String, Object> getVotingStatsByDepartment(int electionId, int departmentId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
        SELECT 
            COUNT(DISTINCT mv.id) as total_mesas,
            COUNT(DISTINCT pv.id) as total_puestos,
            COUNT(DISTINCT m.id) as total_municipios,
            COUNT(DISTINCT c.id) as total_ciudadanos,
            COUNT(DISTINCT v.id) as total_votos
        FROM departamento d
        LEFT JOIN municipio m ON d.id = m.departamento_id
        LEFT JOIN puesto_votacion pv ON m.id = pv.municipio_id
        LEFT JOIN mesa_votacion mv ON pv.id = mv.puesto_id
        LEFT JOIN ciudadano c ON mv.id = c.mesa_id
        LEFT JOIN votos v ON mv.id::text = v.machine_id AND v.election_id = ?
        WHERE d.id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, departmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("departamento_id", departmentId);
                    stats.put("election_id", electionId);
                    stats.put("total_mesas", rs.getInt("total_mesas"));
                    stats.put("total_puestos", rs.getInt("total_puestos"));
                    stats.put("total_municipios", rs.getInt("total_municipios"));
                    stats.put("total_ciudadanos", rs.getLong("total_ciudadanos"));
                    stats.put("total_votos", rs.getLong("total_votos"));

                    long totalCitizens = rs.getLong("total_ciudadanos");
                    long totalVotes = rs.getLong("total_votos");
                    double participation = totalCitizens > 0 ? (double) totalVotes / totalCitizens * 100 : 0.0;
                    stats.put("participation_percentage", participation);
                }
            }


        } catch (SQLException e) {
        }

        return stats;
    }

    @Override
    public Map<String, Object> getVotingStatsByMunicipality(int electionId, int municipalityId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
        SELECT 
            COUNT(DISTINCT mv.id) as total_mesas,
            COUNT(DISTINCT pv.id) as total_puestos,
            COUNT(DISTINCT c.id) as total_ciudadanos,
            COUNT(DISTINCT v.id) as total_votos
        FROM municipio m
        LEFT JOIN puesto_votacion pv ON m.id = pv.municipio_id
        LEFT JOIN mesa_votacion mv ON pv.id = mv.puesto_id
        LEFT JOIN ciudadano c ON mv.id = c.mesa_id
        LEFT JOIN votos v ON mv.id::text = v.machine_id AND v.election_id = ?
        WHERE m.id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, municipalityId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("municipio_id", municipalityId);
                    stats.put("election_id", electionId);
                    stats.put("total_mesas", rs.getInt("total_mesas"));
                    stats.put("total_puestos", rs.getInt("total_puestos"));
                    stats.put("total_ciudadanos", rs.getLong("total_ciudadanos"));
                    stats.put("total_votos", rs.getLong("total_votos"));

                    long totalCitizens = rs.getLong("total_ciudadanos");
                    long totalVotes = rs.getLong("total_votos");
                    double participation = totalCitizens > 0 ? (double) totalVotes / totalCitizens * 100 : 0.0;
                    stats.put("participation_percentage", participation);
                }
            }


        } catch (SQLException e) {
        }

        return stats;
    }

    @Override
    public Map<String, Object> getVotingStatsByPuesto(int electionId, int puestoId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
        SELECT 
            COUNT(DISTINCT mv.id) as total_mesas,
            COUNT(DISTINCT c.id) as total_ciudadanos,
            COUNT(DISTINCT v.id) as total_votos
        FROM puesto_votacion pv
        LEFT JOIN mesa_votacion mv ON pv.id = mv.puesto_id
        LEFT JOIN ciudadano c ON mv.id = c.mesa_id
        LEFT JOIN votos v ON mv.id::text = v.machine_id AND v.election_id = ?
        WHERE pv.id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, puestoId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("puesto_id", puestoId);
                    stats.put("election_id", electionId);
                    stats.put("total_mesas", rs.getInt("total_mesas"));
                    stats.put("total_ciudadanos", rs.getLong("total_ciudadanos"));
                    stats.put("total_votos", rs.getLong("total_votos"));

                    long totalCitizens = rs.getLong("total_ciudadanos");
                    long totalVotes = rs.getLong("total_votos");
                    double participation = totalCitizens > 0 ? (double) totalVotes / totalCitizens * 100 : 0.0;
                    stats.put("participation_percentage", participation);
                }
            }


        } catch (SQLException e) {
        }

        return stats;
    }

    @Override
    public Map<String, Object> getElectionResultsSummary(int electionId) {
        Map<String, Object> summary = new HashMap<>();

        try (Connection conn = getConnection()) {

            // Get election info
            Map<String, Object> electionInfo = getElectionInfo(electionId);
            if (electionInfo != null) {
                summary.put("election_name", electionInfo.get("nombre"));
                summary.put("election_status", electionInfo.get("estado"));
                summary.put("fecha_inicio", electionInfo.get("fecha_inicio"));
                summary.put("fecha_fin", electionInfo.get("fecha_fin"));
            }

            // Get candidate results
            Map<Integer, Integer> votes = getVotesPerCandidate(electionId);
            List<Map<String, Object>> candidateResults = new ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
                int candidateId = entry.getKey();
                int voteCount = entry.getValue();
                String candidateName = getCandidateNameById(candidateId);

                Map<String, Object> candidateResult = new HashMap<>();
                candidateResult.put("candidate_id", candidateId);
                candidateResult.put("candidate_name", candidateName);
                candidateResult.put("vote_count", voteCount);
                candidateResults.add(candidateResult);
            }

            // Sort by vote count (descending)
            candidateResults.sort((a, b) ->
                    Integer.compare((Integer) b.get("vote_count"), (Integer) a.get("vote_count")));

            summary.put("candidate_results", candidateResults);

            // Calculate total votes
            int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
            summary.put("total_votes", totalVotes);

            // Add percentages
            for (Map<String, Object> result : candidateResults) {
                int voteCount = (Integer) result.get("vote_count");
                double percentage = totalVotes > 0 ? (double) voteCount / totalVotes * 100 : 0.0;
                result.put("percentage", percentage);
            }

            summary.put("timestamp", new Date());


        } catch (Exception e) {
            summary.put("error", "Failed to generate summary: " + e.getMessage());
        }

        return summary;
    }

    @Override
    public List<Map<String, Object>> getAllActiveElections() {
        List<Map<String, Object>> elections = new ArrayList<>();
        String sql = "SELECT id, nombre, fecha_inicio, fecha_fin, estado FROM elecciones WHERE estado IN ('ACTIVE', 'OPEN', 'RUNNING') ORDER BY fecha_inicio DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> election = new HashMap<>();
                election.put("id", rs.getInt("id"));
                election.put("nombre", rs.getString("nombre"));
                election.put("fecha_inicio", rs.getTimestamp("fecha_inicio"));
                election.put("fecha_fin", rs.getTimestamp("fecha_fin"));
                election.put("estado", rs.getString("estado"));
                elections.add(election);
            }


        } catch (SQLException e) {
        }

        return elections;
    }

    @Override
    public boolean validateCitizenDocument(String documento) {
        String sql = "SELECT 1 FROM ciudadano WHERE documento = ? AND mesa_id IS NOT NULL";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, documento);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                return exists;
            }

        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getLocationHierarchyByMesa(int mesaId) {
        String sql = """
        SELECT 
            mv.id as mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.id as puesto_id,
            pv.nombre as puesto_nombre,
            pv.direccion as puesto_direccion,
            pv.consecutive as puesto_consecutive,
            m.id as municipio_id,
            m.nombre as municipio_nombre,
            d.id as departamento_id,
            d.nombre as departamento_nombre
        FROM mesa_votacion mv
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE mv.id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, mesaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> hierarchy = new HashMap<>();
                    hierarchy.put("mesa_id", rs.getInt("mesa_id"));
                    hierarchy.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    hierarchy.put("puesto_id", rs.getInt("puesto_id"));
                    hierarchy.put("puesto_nombre", rs.getString("puesto_nombre"));
                    hierarchy.put("puesto_direccion", rs.getString("puesto_direccion"));
                    hierarchy.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    hierarchy.put("municipio_id", rs.getInt("municipio_id"));
                    hierarchy.put("municipio_nombre", rs.getString("municipio_nombre"));
                    hierarchy.put("departamento_id", rs.getInt("departamento_id"));
                    hierarchy.put("departamento_nombre", rs.getString("departamento_nombre"));

                    return hierarchy;
                }
            }

        } catch (SQLException e) {
        }

        return null;
    }

    // =================== MÉTODOS PARA FULL CITIZEN REPORTS ===================

    /**
     * Obtiene todos los ciudadanos de un departamento específico
     */
    @Override
    public List<Map<String, Object>> getCitizensByDepartment(int departmentId) {
        List<Map<String, Object>> citizens = new ArrayList<>();

        String sql = """
        SELECT DISTINCT
            c.id as ciudadano_id,
            c.documento,
            c.nombre,
            c.apellido,
            c.mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.id as puesto_id,
            pv.nombre as puesto_nombre,
            pv.direccion as puesto_direccion,
            pv.consecutive as puesto_consecutive,
            m.id as municipio_id,
            m.nombre as municipio_nombre,
            d.id as departamento_id,
            d.nombre as departamento_nombre
        FROM ciudadano c
        JOIN mesa_votacion mv ON c.mesa_id = mv.id
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE d.id = ?
        ORDER BY c.apellido, c.nombre, c.documento
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, departmentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("ciudadano_id", rs.getInt("ciudadano_id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));
                    citizen.put("mesa_id", rs.getInt("mesa_id"));
                    citizen.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    citizen.put("puesto_id", rs.getInt("puesto_id"));
                    citizen.put("puesto_nombre", rs.getString("puesto_nombre"));
                    citizen.put("puesto_direccion", rs.getString("puesto_direccion"));
                    citizen.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    citizen.put("municipio_id", rs.getInt("municipio_id"));
                    citizen.put("municipio_nombre", rs.getString("municipio_nombre"));
                    citizen.put("departamento_id", rs.getInt("departamento_id"));
                    citizen.put("departamento_nombre", rs.getString("departamento_nombre"));
                    citizens.add(citizen);
                }
            }


        } catch (SQLException e) {
        }

        return citizens;
    }

    /**
     * Obtiene todos los ciudadanos de un municipio específico
     */
    @Override
    public List<Map<String, Object>> getCitizensByMunicipality(int municipalityId) {
        List<Map<String, Object>> citizens = new ArrayList<>();

        String sql = """
        SELECT DISTINCT
            c.id as ciudadano_id,
            c.documento,
            c.nombre,
            c.apellido,
            c.mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.id as puesto_id,
            pv.nombre as puesto_nombre,
            pv.direccion as puesto_direccion,
            pv.consecutive as puesto_consecutive,
            m.id as municipio_id,
            m.nombre as municipio_nombre,
            d.id as departamento_id,
            d.nombre as departamento_nombre
        FROM ciudadano c
        JOIN mesa_votacion mv ON c.mesa_id = mv.id
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE m.id = ?
        ORDER BY c.apellido, c.nombre, c.documento
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, municipalityId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("ciudadano_id", rs.getInt("ciudadano_id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));
                    citizen.put("mesa_id", rs.getInt("mesa_id"));
                    citizen.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    citizen.put("puesto_id", rs.getInt("puesto_id"));
                    citizen.put("puesto_nombre", rs.getString("puesto_nombre"));
                    citizen.put("puesto_direccion", rs.getString("puesto_direccion"));
                    citizen.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    citizen.put("municipio_id", rs.getInt("municipio_id"));
                    citizen.put("municipio_nombre", rs.getString("municipio_nombre"));
                    citizen.put("departamento_id", rs.getInt("departamento_id"));
                    citizen.put("departamento_nombre", rs.getString("departamento_nombre"));
                    citizens.add(citizen);
                }
            }


        } catch (SQLException e) {
        }

        return citizens;
    }

    /**
     * Obtiene todos los ciudadanos de un puesto de votación específico
     */
    @Override
    public List<Map<String, Object>> getCitizensByPuesto(int puestoId) {
        List<Map<String, Object>> citizens = new ArrayList<>();

        String sql = """
        SELECT DISTINCT
            c.id as ciudadano_id,
            c.documento,
            c.nombre,
            c.apellido,
            c.mesa_id,
            mv.consecutive as mesa_consecutive,
            pv.id as puesto_id,
            pv.nombre as puesto_nombre,
            pv.direccion as puesto_direccion,
            pv.consecutive as puesto_consecutive,
            m.id as municipio_id,
            m.nombre as municipio_nombre,
            d.id as departamento_id,
            d.nombre as departamento_nombre
        FROM ciudadano c
        JOIN mesa_votacion mv ON c.mesa_id = mv.id
        JOIN puesto_votacion pv ON mv.puesto_id = pv.id
        JOIN municipio m ON pv.municipio_id = m.id
        JOIN departamento d ON m.departamento_id = d.id
        WHERE pv.id = ?
        ORDER BY c.apellido, c.nombre, c.documento
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, puestoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> citizen = new HashMap<>();
                    citizen.put("ciudadano_id", rs.getInt("ciudadano_id"));
                    citizen.put("documento", rs.getString("documento"));
                    citizen.put("nombre", rs.getString("nombre"));
                    citizen.put("apellido", rs.getString("apellido"));
                    citizen.put("mesa_id", rs.getInt("mesa_id"));
                    citizen.put("mesa_consecutive", rs.getInt("mesa_consecutive"));
                    citizen.put("puesto_id", rs.getInt("puesto_id"));
                    citizen.put("puesto_nombre", rs.getString("puesto_nombre"));
                    citizen.put("puesto_direccion", rs.getString("puesto_direccion"));
                    citizen.put("puesto_consecutive", rs.getInt("puesto_consecutive"));
                    citizen.put("municipio_id", rs.getInt("municipio_id"));
                    citizen.put("municipio_nombre", rs.getString("municipio_nombre"));
                    citizen.put("departamento_id", rs.getInt("departamento_id"));
                    citizen.put("departamento_nombre", rs.getString("departamento_nombre"));
                    citizens.add(citizen);
                }
            }


        } catch (SQLException e) {
        }

        return citizens;
    }




}