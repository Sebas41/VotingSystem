package Controller;

import ConnectionDB.ConnectionDB;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.Vote;
import model.ReliableMessage;

import java.util.Date;
import java.util.List;

public class ServerControllerImpl implements ServerControllerInterface {

    private ElectionInterface election;
    private ConnectionDB connectionDB;

    public ServerControllerImpl() {
        this.election = new ElectionImpl(0, new Date(), new Date(), "");
        this.connectionDB = new ConnectionDB();
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
            System.out.println("Voto registrado exitosamente para candidato ID: " + candidateId);

        } catch (Exception e) {
            System.err.println("Error al registrar el voto: " + e.getMessage());
        }
    }

    @Override
    public String getElectionInfo() {
        return election.getElectionInfo();
    }


    public void createElection(int id, String name, Date start, Date end) {
        election.registerElection(id, name, start, end);
        System.out.println("Elección creada correctamente: " + name);
    }

    public void changeElectionStatus(Elections.models.ELECTION_STATUS status) {
        election.changeElectionStatus(status);
        System.out.println("Estado de la elección cambiado a: " + status);
    }

    public void addCandidate(int id, String name, String party) {
        election.addCandidate(id, name, party);
        System.out.println("Candidato añadido: " + name);
    }

    public void editCandidate(int id, String newName, String newParty) {
        boolean success = election.editCandidate(id, newName, newParty);
        if (success) {
            System.out.println("Candidato editado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    public void removeCandidate(int id) {
        boolean success = election.removeCandidate(id);
        if (success) {
            System.out.println("Candidato eliminado con éxito.");
        } else {
            System.out.println("No se encontró el candidato con ID: " + id);
        }
    }

    public void loadCandidatesFromCSV(String filepath) {
        election.loadCandidatesFromCSV(filepath);
        System.out.println("Candidatos cargados desde: " + filepath);
    }

    public String showElectionInfo() {
        return election.getElectionInfo();
    }

    public List<Candidate> getCandidates() {
        return election.getCandidates();
    }
}
