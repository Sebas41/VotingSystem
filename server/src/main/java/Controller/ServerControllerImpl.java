package Controller;

import ConnectionDB.ConnectionDB;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.Vote;
import Elections.models.ELECTION_STATUS;
import Reports.ReportsInterface;
import ServerUI.ServerUI;
import model.ReliableMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import Reports.ReportsImplementation;
import Reports.ReportsInterface;


public class ServerControllerImpl implements ServerControllerInterface {

    private ElectionInterface election;
    private ConnectionDB connectionDB;
    private ReportsInterface reports;

    public ServerControllerImpl() {
        this.connectionDB = new ConnectionDB();
        this.election = new ElectionImpl(0, new Date(), new Date(), "");
        this.reports = new ReportsImplementation(connectionDB);
        cargarDatosPrueba();  // Aquí inicializamos datos de ejemplo
    }

    private void cargarDatosPrueba() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            Date start = sdf.parse("30-05-2025 00:00");
            Date end = sdf.parse("31-12-2025 23:59");

            // Crear elección de prueba
            createElection(1, "Elección de Prueba", start, end);
            changeElectionStatus(ELECTION_STATUS.DURING);

            // Agregar candidatos
            addCandidate(1, "Candidato A", "Partido A");
            addCandidate(2, "Candidato B", "Partido B");
            addCandidate(3, "Candidato C", "Partido C");

            System.out.println("=== Datos de prueba cargados exitosamente ===");
        } catch (Exception e) {
            System.err.println("Error cargando datos de prueba: " + e.getMessage());
        }
    }

    @Override
    public void registerVote(ReliableMessage newVote) {
        try {
            String jsonPayload = newVote.getMessage().message;
            Vote vote = new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonPayload, Vote.class);

            int candidateId = Integer.parseInt(vote.getVote());

            if (!election.isElectionActive()) {
                System.out.println("La elección no está activa. No se puede registrar el voto.");
                return;
            }

            election.addVoteToCandidate(candidateId, vote);
            connectionDB.storeVote(vote);
            System.out.printf("VOTO GURADADO CON ID"+newVote.getUuid());
            System.out.println("Voto registrado exitosamente para candidato ID: " + candidateId);

            // Verifica que la UI esté inicializada antes de actualizar
            ServerUI ui = ServerUI.getInstance();
            if (ui != null) {
                ui.showVoteInfo("Voto recibido para candidato ID: " + candidateId);
            }

        } catch (Exception e) {
            System.err.println("Error al registrar el voto: " + e.getMessage());
        }
    }

    @Override
    public String getElectionInfo() {
        return election.getElectionInfo();
    }

    @Override
    public void createElection(int id, String name, Date start, Date end) {
        election.registerElection(id, name, start, end);
        connectionDB.storeElection(id, name, start, end, ELECTION_STATUS.PRE.name());
        System.out.println("Elección creada correctamente: " + name);
    }

    @Override
    public void changeElectionStatus(ELECTION_STATUS status) {
        election.changeElectionStatus(status);
        System.out.println("Estado de la elección cambiado a: " + status);
    }

    @Override
    public void addCandidate(int id, String name, String party) {
        election.addCandidate(id, name, party);
        connectionDB.storeCandidate(id, name, party, election.getElectionId());
        System.out.println("Candidato añadido: " + name);
    }

    @Override
    public void editCandidate(int id, String newName, String newParty) {
        boolean success = election.editCandidate(id, newName, newParty);
        if (success) {
            System.out.println("Candidato editado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    @Override
    public void removeCandidate(int id) {
        boolean success = election.removeCandidate(id);
        if (success) {
            System.out.println("Candidato eliminado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    @Override
    public void loadCandidatesFromCSV(String filepath) {
        election.loadCandidatesFromCSV(filepath);
        System.out.println("Candidatos cargados desde: " + filepath);
    }

    @Override
    public List<Candidate> getCandidates() {
        return election.getCandidates();
    }

    public void showVotesPerCandidateReport(int electionId) {
        var result = reports.getTotalVotesPerCandidate(electionId);
        System.out.println("=== Total de votos por candidato ===");
        result.forEach((candidate, votes) -> System.out.println(candidate + ": " + votes));
    }

    public void showVotesPerCandidateByMachine(int electionId) {
        var result = reports.getVotesPerCandidateByMachine(electionId);
        System.out.println("=== Votos por candidato por máquina ===");
        result.forEach((machineId, map) -> {
            System.out.println("Máquina: " + machineId);
            map.forEach((candidate, votes) -> System.out.println("  " + candidate + ": " + votes));
        });
    }

    public void exportVotesPerMachineCSV(int electionId, String path) {
        var file = reports.exportVotesPerMachineCSV(electionId, path);
        System.out.println("Reporte por mesa exportado en: " + file.getAbsolutePath());
    }

    public void exportElectionResultsCSV(int electionId, String path) {
        var file = reports.exportElectionResultsCSV(electionId, path);
        System.out.println("Resultados de elecciones exportados en: " + file.getAbsolutePath());
    }

    @Override
    public String getTotalVotesPerCandidate(int electionId) {
        var result = reports.getTotalVotesPerCandidate(electionId);
        StringBuilder sb = new StringBuilder("=== Total de votos por candidato ===\n");
        result.forEach((candidate, votes) -> sb.append(candidate).append(": ").append(votes).append("\n"));
        return sb.toString();
    }

    @Override
    public String getVotesPerCandidateByMachine(int electionId) {
        var result = reports.getVotesPerCandidateByMachine(electionId);
        StringBuilder sb = new StringBuilder("=== Votos por candidato por máquina ===\n");
        result.forEach((machineId, map) -> {
            sb.append("Máquina: ").append(machineId).append("\n");
            map.forEach((candidate, votes) -> sb.append("  ").append(candidate).append(": ").append(votes).append("\n"));
        });
        return sb.toString();
    }






}
