package Controller;

import Elections.models.ELECTION_STATUS;
import configuration.ConfigurationSender;
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

    // Agregar a ServerControllerInterface.java
    public boolean changeElectionStatusInAllMachines(ELECTION_STATUS newStatus);
    public boolean startElectionInAllMachines();
    public boolean closeElectionInAllMachines();
    public boolean resetElectionInAllMachines();
    public void setConfigurationSender(ConfigurationSender configurationSender);





}