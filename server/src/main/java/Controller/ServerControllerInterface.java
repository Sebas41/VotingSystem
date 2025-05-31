package Controller;

import model.ReliableMessage;
import Elections.models.Candidate;

import java.util.Date;
import java.util.List;

public interface ServerControllerInterface {

    // Voto
    void registerVote(ReliableMessage newVote);

    // Información de elección
    String getElectionInfo();

    // Crear elección
    void createElection(int id, String name, Date start, Date end);

    // Candidatos
    void addCandidate(int id, String name, String party);
    void editCandidate(int id, String newName, String newParty);
    void removeCandidate(int id);
    List<Candidate> getCandidates();

    // Estado de la elección
    void changeElectionStatus(Elections.models.ELECTION_STATUS status);

    // Cargar candidatos desde CSV (puede usarse más adelante)
    void loadCandidatesFromCSV(String filepath);


    void showVotesPerCandidateReport(int electionId);
    void showVotesPerCandidateByMachine(int electionId);
    void exportVotesPerMachineCSV(int electionId, String path);
    void exportElectionResultsCSV(int electionId, String path);
    String getTotalVotesPerCandidate(int electionId);
    String getVotesPerCandidateByMachine(int electionId);




}
