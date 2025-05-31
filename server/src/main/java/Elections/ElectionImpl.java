package Elections;

import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import Elections.models.Vote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class ElectionImpl implements ElectionInterface {

    private int electionId;
    private Date startDate;
    private Date endDate;
    private String electionName;
    private List<Candidate> candidates;
    private ELECTION_STATUS status;

    public ElectionImpl(int electionId, Date startDate, Date endDate, String electionName) {
        this.electionId = electionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.electionName = electionName;
        this.candidates = new ArrayList<>();
        this.status = ELECTION_STATUS.PRE;
    }



    @Override
    public void registerElection(int electionId, String name, Date startDate, Date endDate) {
        this.electionId = electionId;
        this.electionName = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ELECTION_STATUS.PRE;
        this.candidates = new ArrayList<>();
    }

    @Override
    public String getElectionName() {
        return electionName;
    }

    @Override
    public int getElectionId() {
        return electionId;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public ELECTION_STATUS getElectionStatus() {
        return status;
    }

    @Override
    public void changeElectionStatus(ELECTION_STATUS newStatus) {
        this.status = newStatus;
    }

    @Override
    public void addCandidate(int id, String name, String party){
        this.candidates.add(new Candidate(id, name, party, this.electionId));
    }

    @Override
    public boolean editCandidate(int id, String newName, String newParty) {
        for (Candidate c : candidates) {
            if (c.getId() == id) {
                c.setName(newName);
                c.setPoliticalParty(newParty);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeCandidate(int id) {
        return candidates.removeIf(c -> c.getId() == id);
    }

    @Override
    public void loadCandidatesFromCSV(String filepath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 3) {
                    int id = Integer.parseInt(tokens[0].trim());
                    String name = tokens[1].trim();
                    String party = tokens[2].trim();
                    addCandidate(id, name, party);
                }
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo CSV: " + e.getMessage());
        }
    }

    @Override
    public List<Candidate> getCandidates() {
        return candidates;
    }

    @Override
    public boolean isElectionActive() {
        return status == ELECTION_STATUS.DURING;
    }

    @Override
    public boolean isElectionClosed() {
        return status == ELECTION_STATUS.CLOSED;
    }

    @Override
    public String getElectionInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Información de la Elección ===\n");
        sb.append("ID: ").append(electionId).append("\n");
        sb.append("Nombre: ").append(electionName).append("\n");
        sb.append("Inicio: ").append(startDate).append("\n");
        sb.append("Fin: ").append(endDate).append("\n");
        sb.append("Estado: ").append(status).append("\n");
        sb.append("Candidatos:\n");

        if (candidates.isEmpty()) {
            sb.append("  - No hay candidatos registrados.\n");
        } else {
            for (Candidate c : candidates) {
                sb.append("  - ID: ").append(c.getId())
                        .append(", Nombre: ").append(c.getName())
                        .append(", Partido: ").append(c.getPoliticalParty())
                        .append("\n");
            }
        }

        return sb.toString();
    }



    @Override
    public void addVoteToCandidate(int candidateId, Vote vote) {

        this.candidates.get(candidateId).addVote(vote);

    }

}
