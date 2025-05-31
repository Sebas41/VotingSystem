package ConnectionDB;

import Elections.models.Vote;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

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
    public void storeVote(Vote vote) {
        String sql = "INSERT INTO votos (machine_id, voto, fecha, election_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, vote.getmachineId());
            stmt.setString(2, vote.getVote());
            stmt.setTimestamp(3, new Timestamp(vote.getDate().getTimeInMillis()));
            stmt.setInt(4, vote.getElection());
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error al guardar el voto en la base de datos: " + e.getMessage());
        }
    }
}
