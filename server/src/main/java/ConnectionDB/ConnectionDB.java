package ConnectionDB;

import Elections.models.Vote;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

public class ConnectionDB implements ConnectionDBinterface {

    private static final String URL = "jdbc:postgresql://localhost:5432/votaciones";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    private Connection connection;

    public ConnectionDB() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión a PostgreSQL exitosa.");
        } catch (Exception e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
        }
    }

    @Override
    public void storeElection(int id, String name, Date start, Date end, String status) {
        String sql = "INSERT INTO elecciones (id, nombre, fecha_inicio, fecha_fin, estado) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setTimestamp(3, new Timestamp(start.getTime()));
            stmt.setTimestamp(4, new Timestamp(end.getTime()));
            stmt.setString(5, status);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error guardando la elección: " + e.getMessage());
        }
    }

    @Override
    public void storeCandidate(int id, String name, String party, int electionId) {
        String sql = "INSERT INTO candidatos (id, nombre, partido, eleccion_id) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, party);
            stmt.setInt(4, electionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error guardando el candidato: " + e.getMessage());
        }
    }

    @Override
    public void storeVote(Vote vote) {
        String sql = "INSERT INTO votos (machine_id, candidato_id, fecha, election_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, vote.getmachineId());
            stmt.setInt(2, Integer.parseInt(vote.getVote()));  // FK al candidato
            stmt.setTimestamp(3, new Timestamp(vote.getDate().getTimeInMillis()));
            stmt.setInt(4, vote.getElection());
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error al guardar el voto en la base de datos: " + e.getMessage());
        }
    }

}
