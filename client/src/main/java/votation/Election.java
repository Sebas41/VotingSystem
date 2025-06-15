package votation;

import java.util.List;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore; // ✅ AÑADIDO

public class Election {

    private int electionId;
    private List<Candidate> candidates;

    // ✅ NUEVOS CAMPOS PARA HORARIOS DE JORNADA
    @JsonProperty("votingStartTime")
    private long votingStartTime;    // Timestamp inicio jornada

    @JsonProperty("votingEndTime")
    private long votingEndTime;      // Timestamp fin jornada

    // Constructor por defecto para Jackson
    public Election() {
    }

    // ✅ ÚNICO CONSTRUCTOR con 4 parámetros (con @JsonCreator)
    @JsonCreator
    public Election(
            @JsonProperty("electionId") int electionId,
            @JsonProperty("candidates") List<Candidate> candidates,
            @JsonProperty("votingStartTime") long votingStartTime,
            @JsonProperty("votingEndTime") long votingEndTime) {
        this.electionId = electionId;
        this.candidates = candidates;
        this.votingStartTime = votingStartTime;
        this.votingEndTime = votingEndTime;
    }

    // ✅ Constructor legacy (sin horarios) para compatibilidad
    public Election(int electionId, List<Candidate> candidates) {
        this.electionId = electionId;
        this.candidates = candidates;
        this.votingStartTime = 0;
        this.votingEndTime = 0;
    }

    // Getters originales
    public int getElectionId() {
        return electionId;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    // ✅ NUEVOS GETTERS PARA HORARIOS
    public long getVotingStartTime() {
        return votingStartTime;
    }

    public long getVotingEndTime() {
        return votingEndTime;
    }

    // ✅ SETTERS PARA JACKSON
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

    // ✅ MÉTODOS DE VALIDACIÓN DE HORARIO
    @JsonIgnore  // ✅ CRÍTICO: Evita que Jackson serialice esto como "votingOpen"
    public boolean isVotingOpen() {
        long now = System.currentTimeMillis();
        return now >= votingStartTime && now <= votingEndTime;
    }

    @JsonIgnore  // ✅ También ignorar este para consistencia
    public String getVotingStatus() {
        long now = System.currentTimeMillis();

        if (votingStartTime == 0 || votingEndTime == 0) {
            return "SIN_HORARIO"; // No se configuraron horarios
        }

        if (now < votingStartTime) {
            return "NO_INICIADA";
        } else if (now > votingEndTime) {
            return "CERRADA";
        } else {
            return "ABIERTA";
        }
    }

    @JsonIgnore  // ✅ También ignorar métodos de tiempo
    public long getTimeUntilClose() {
        long now = System.currentTimeMillis();
        return Math.max(0, votingEndTime - now);
    }

    @JsonIgnore  // ✅ También ignorar métodos de tiempo
    public long getTimeUntilOpen() {
        long now = System.currentTimeMillis();
        return Math.max(0, votingStartTime - now);
    }

    @JsonIgnore  // ✅ También ignorar método de formato
    public String getFormattedSchedule() {
        if (votingStartTime == 0 || votingEndTime == 0) {
            return "Horario no configurado";
        }

        Date startDate = new Date(votingStartTime);
        Date endDate = new Date(votingEndTime);

        return String.format("Jornada: %tH:%tM - %tH:%tM",
                startDate, startDate, endDate, endDate);
    }

    // Método original
    public void showCandidates() {
        System.out.println("Candidatos disponibles para elección #" + electionId + ":");
        for (Candidate c : candidates) {
            System.out.println(c.toString());
        }

        // ✅ MOSTRAR TAMBIÉN HORARIO
        System.out.println("Estado de votación: " + getVotingStatus());
        System.out.println(getFormattedSchedule());
    }
}