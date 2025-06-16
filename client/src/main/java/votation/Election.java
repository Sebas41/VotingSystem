package votation;

import java.util.List;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;


public class Election {

    private int electionId;
    private List<Candidate> candidates;
    private ELECTION_STATUS status;

    @JsonProperty("votingStartTime")
    private long votingStartTime;    // Timestamp inicio jornada

    @JsonProperty("votingEndTime")
    private long votingEndTime;      // Timestamp fin jornada

    @JsonProperty("electionStatus")
    private ELECTION_STATUS electionStatus;

    // Constructor por defecto para Jackson
    public Election() {
    }

    @JsonCreator
    public Election(
            @JsonProperty("electionId") int electionId,
            @JsonProperty("candidates") List<Candidate> candidates,
            @JsonProperty("votingStartTime") long votingStartTime,
            @JsonProperty("votingEndTime") long votingEndTime,
            @JsonProperty("electionStatus") ELECTION_STATUS electionStatus) {
        this.electionId = electionId;
        this.candidates = candidates;
        this.votingStartTime = votingStartTime;
        this.votingEndTime = votingEndTime;
        this.electionStatus = electionStatus != null ? electionStatus : ELECTION_STATUS.PRE;
    }

    public Election(int electionId, List<Candidate> candidates, long votingStartTime, long votingEndTime) {
        this.electionId = electionId;
        this.candidates = candidates;
        this.votingStartTime = votingStartTime;
        this.votingEndTime = votingEndTime;
        this.electionStatus = ELECTION_STATUS.PRE;
    }

    public Election(int electionId, List<Candidate> candidates) {
        this.electionId = electionId;
        this.candidates = candidates;
        this.votingStartTime = 0;
        this.votingEndTime = 0;
        this.electionStatus = ELECTION_STATUS.PRE;
    }

    // =================== GETTERS Y SETTERS EXISTENTES ===================

    public int getElectionId() {
        return electionId;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public long getVotingStartTime() {
        return votingStartTime;
    }

    public long getVotingEndTime() {
        return votingEndTime;
    }

    public void setElectionId(int electionId) {
        this.electionId = electionId;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public void setVotingStartTime(long votingStartTime) {
        this.votingStartTime = votingStartTime;
    }

    public void setVotingEndTime(long votingEndTime) {
        this.votingEndTime = votingEndTime;
    }

    public ELECTION_STATUS getElectionStatus() {
        return electionStatus;
    }

    public void setElectionStatus(ELECTION_STATUS electionStatus) {
        this.electionStatus = electionStatus;
    }

    public void setElectionStatus(String statusString) {
        if (statusString != null) {
            try {
                this.electionStatus = ELECTION_STATUS.valueOf(statusString.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Estado inválido: " + statusString + ", usando PRE por defecto");
                this.electionStatus = ELECTION_STATUS.PRE;
            }
        }
    }

    // =================== MÉTODOS EXISTENTES DE VALIDACIÓN DE HORARIO ===================

    @JsonIgnore
    public boolean isVotingOpen() {
        long now = System.currentTimeMillis();
        return now >= votingStartTime && now <= votingEndTime;
    }

    @JsonIgnore
    public String getVotingStatus() {
        long now = System.currentTimeMillis();

        if (votingStartTime == 0 || votingEndTime == 0) {
            return "SIN_HORARIO";
        }

        if (now < votingStartTime) {
            return "NO_INICIADA";
        } else if (now > votingEndTime) {
            return "CERRADA";
        } else {
            return "ABIERTA";
        }
    }

    @JsonIgnore
    public long getTimeUntilClose() {
        long now = System.currentTimeMillis();
        return Math.max(0, votingEndTime - now);
    }

    @JsonIgnore
    public long getTimeUntilOpen() {
        long now = System.currentTimeMillis();
        return Math.max(0, votingStartTime - now);
    }

    @JsonIgnore
    public String getFormattedSchedule() {
        if (votingStartTime == 0 || votingEndTime == 0) {
            return "Horario no configurado";
        }

        Date startDate = new Date(votingStartTime);
        Date endDate = new Date(votingEndTime);

        return String.format("Jornada: %tH:%tM - %tH:%tM",
                startDate, startDate, endDate, endDate);
    }

    // =================== NUEVOS MÉTODOS PARA CONTROL DE ESTADO ===================

    @JsonIgnore
    public boolean canVote() {
        // Verificar estado de la elección
        if (electionStatus == ELECTION_STATUS.CLOSED || electionStatus == ELECTION_STATUS.PRE) {
            return false;
        }

        // Si está en DURING, verificar horarios también (si están configurados)
        if (votingStartTime > 0 && votingEndTime > 0) {
            return isVotingOpen();
        }

        // Si no hay horarios configurados pero está en DURING, permitir votar
        return electionStatus == ELECTION_STATUS.DURING;
    }

    @JsonIgnore
    public String getFullVotingStatus() {
        switch (electionStatus) {
            case PRE:
                return "La elección aún no ha iniciado";
            case CLOSED:
                return "La elección ha terminado";
            case DURING:
                // Si está en DURING, verificar horarios también
                if (votingStartTime > 0 && votingEndTime > 0) {
                    return getVotingStatus(); // Usa el método original para verificar horarios
                } else {
                    return "ABIERTA";
                }
            default:
                return "Estado desconocido";
        }
    }

    // =================== MÉTODOS EXISTENTES ===================

    public void showCandidates() {
        System.out.println("Candidatos disponibles para elección #" + electionId + ":");
        for (Candidate c : candidates) {
            System.out.println(c.toString());
        }

        System.out.println("Estado de elección: " + electionStatus);
        System.out.println("Estado de votación: " + getFullVotingStatus());
        System.out.println(getFormattedSchedule());
    }
}