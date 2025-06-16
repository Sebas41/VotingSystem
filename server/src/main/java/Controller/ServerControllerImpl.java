package Controller;

import ConnectionDB.ConnectionDB;
import Elections.ElectionImpl;
import Elections.ElectionInterface;
import Elections.models.Candidate;
import Elections.models.ELECTION_STATUS;
import ServerUI.ServerUI;
import configuration.ConfigurationSender;
import model.ReliableMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import Reports.VoteNotifierImpl;
import model.Vote;
import java.util.ArrayList.*;
import java.util.logging.Logger;

import Reports.VoteNotifierImpl;

public class ServerControllerImpl implements ServerControllerInterface {

    private ElectionInterface election;
    private ConnectionDB connectionDB;
    private static VoteNotifierImpl voteNotifier;
    private ConfigurationSender configurationSender;

    public ServerControllerImpl() {
        this.connectionDB = new ConnectionDB();
        this.election = new ElectionImpl(0, new Date(), new Date(), "");
        cargarDatosPrueba();  // Aquí inicializamos datos de ejemplo
    }

    public boolean changeElectionStatusInAllMachines(ELECTION_STATUS newStatus) {
        try {
            System.out.println("🚀 Iniciando cambio de estado global a: " + newStatus);

            // 1. Cambiar estado local en el servidor
            election.changeElectionStatus(newStatus);
            System.out.println("✅ Estado local actualizado a: " + newStatus);

            // 2. Cambiar estado en todas las mesas remotas
            if (configurationSender != null) {
                boolean success = configurationSender.changeElectionStatusForAllMachines(
                        election.getElectionId(),
                        newStatus.name()
                );

                if (success) {
                    System.out.println("🎉 Estado cambiado exitosamente en todas las mesas");
                    return true;
                } else {
                    System.out.println("⚠️ Cambio de estado completado con algunos errores");
                    return false;
                }
            } else {
                System.out.println("❌ ConfigurationSender no está disponible");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error cambiando estado global: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean startElectionInAllMachines() {
        System.out.println("🗳️ Iniciando elección en todas las mesas...");
        return changeElectionStatusInAllMachines(ELECTION_STATUS.DURING);
    }

    /**
     * ✅ MÉTODO DE CONVENIENCIA: Cerrar elección en todas las mesas
     */
    public boolean closeElectionInAllMachines() {
        System.out.println("🔒 Cerrando elección en todas las mesas...");
        return changeElectionStatusInAllMachines(ELECTION_STATUS.CLOSED);
    }

    /**
     * ✅ MÉTODO DE CONVENIENCIA: Resetear elección a PRE en todas las mesas
     */
    public boolean resetElectionInAllMachines() {
        System.out.println("⏪ Reseteando elección a estado PRE en todas las mesas...");
        return changeElectionStatusInAllMachines(ELECTION_STATUS.PRE);
    }



    public void setConfigurationSender(ConfigurationSender configurationSender) {
        this.configurationSender = configurationSender;
        System.out.println("🔗 ServerController conectado con ConfigurationSender");
    }

    public static void setVoteNotifier(VoteNotifierImpl notifier) {
        voteNotifier = notifier;
        System.out.println("🔗 ServerController conectado con VoteNotifier");
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
        System.out.println("=== Registra votante de prueba ===");
        try {
            Vote vote = newVote.getMessage();
            int candidateId = Integer.parseInt(vote.vote);

            if (!election.isElectionActive()) {
                System.out.println("La elección no está activa. No se puede registrar el voto.");
                return;
            }
            election.addVoteToCandidate(candidateId, vote);
            connectionDB.storeVote(vote);
            System.out.println("VOTE HAS BEEN REGISTRED");

            if (voteNotifier != null) {
                try {

                    String candidateName = getCandidateName(candidateId);

                    String voteInfo = formatVoteNotification(candidateName, vote);

                    voteNotifier.notifyVoteReceived(voteInfo, vote.getElection());

                    System.out.println("📢 Notificación enviada: " + candidateName);

                } catch (Exception e) {
                    System.err.println("⚠️ Error enviando notificación de voto: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ VoteNotifier no está conectado");
            }

            System.out.println("Voto registrado exitosamente para candidato ID: " + candidateId);


        } catch (Exception e) {
            System.err.println("Error al registrar el voto: " + e.getMessage());
        }
    }

    private String getCandidateName(int candidateId) {
        try {
            List<Candidate> candidates = election.getCandidates();
            for (Candidate candidate : candidates) {
                if (candidate.getId() == candidateId) {
                    return candidate.getName();
                }
            }
            // Si no se encuentra, usar un nombre genérico
            return "Candidato " + candidateId;
        } catch (Exception e) {
            System.err.println("Error obteniendo nombre del candidato: " + e.getMessage());
            return "Candidato " + candidateId;
        }
    }

    private String formatVoteNotification(String candidateName, Vote vote) {
        try {
            long timestamp = vote.getDate();
            int electionId = vote.getElection();

            return candidateName + "-" + timestamp + "-" + electionId;

        } catch (Exception e) {
            System.err.println("Error formateando notificación: " + e.getMessage());
            return candidateName + "-" + System.currentTimeMillis() + "-" + vote.getElection();
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


    }




