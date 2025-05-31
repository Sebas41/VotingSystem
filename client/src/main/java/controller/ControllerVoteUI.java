package controller;

import Autentication.AutenticationVoter;
import Autentication.VoterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;
import model.Message;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import ui.VotingMachineUI;
import votation.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

public class ControllerVoteUI {

    private VoterRepository voterRepo;
    private VoteRepository voteRepo;
    private AutenticationVoter authVoter;

    private ElectionRepository electionRepo;
    private Election election;

    private Communicator com;
    private RMSourcePrx rm;
    private RMDestinationPrx dest;
    private ObjectMapper mapper;

    private InetAddress ip;
    private VotingMachineUI ui;
    private String currentVoterId;

    public ControllerVoteUI() throws Exception {
        voterRepo = new VoterRepository();
        voteRepo = new VoteRepository();
        authVoter = new AutenticationVoter(voterRepo);

        electionRepo = new ElectionRepository();
        election = electionRepo.getElection();

        mapper = new ObjectMapper();
        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));
        ip = InetAddress.getLocalHost();

        initUI();
    }

    private void initUI() {
        ui = new VotingMachineUI();
        ui.setCandidates(election.getCandidates());

        // Acción de login
        ui.addLoginAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = ui.getVoterId();
                String password = ui.getPassword();

                if (!authVoter.authenticate(id, password)) {
                    ui.showLoginMessage("Credenciales incorrectas.", true);
                    return;
                }

                if (authVoter.hasAlreadyVoted(id)) {
                    ui.showLoginMessage("Ya has ejercido su voto. Gracias.", true);
                    return;
                }

                currentVoterId = id;
                ui.showLoginMessage("Autenticación exitosa. Redirigiendo a votación...", false);
                ui.showVotePanel();
            }
        });

        // Acción de votar
        ui.addVoteAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String candidateId = ui.getSelectedCandidateId();
                    if (candidateId == null) {
                        ui.showVoteMessage("Seleccione un candidato válido.", true);
                        return;
                    }

                    Vote vote = new Vote(ip.getHostAddress(), candidateId);
                    vote.setElectionId(election.getElectionId());
                    voteRepo.save(vote);

                    String payload = mapper.writeValueAsString(vote);
                    rm.setServerProxy(dest);
                    Message msg = new Message();
                    msg.message = payload;
                    rm.sendMessage(msg);

                    authVoter.markAsVoted(currentVoterId);
                    ui.showVoteMessage("Gracias por votar. Su elección ha sido registrada.", false);

                    // Regreso al login
                    ui.resetToLoginAfterVote();

                } catch (Exception ex) {
                    ui.showVoteMessage("Error al emitir el voto: " + ex.getMessage(), true);
                }
            }
        });

        ui.setVisible(true);
    }
}
