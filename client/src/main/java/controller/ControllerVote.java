package controller;

import java.net.InetAddress;

import Autentication.AutenticationVoterInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import Autentication.AutenticationVoter;
import Autentication.VoterRepository;
import model.Message;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import ui.View;
import votation.*;
import model.Vote;

public class ControllerVote {

    private VotationInterface voteRepo;
    private AutenticationVoterInterface authVoter;
    private View view;

    private ElectionInterface electionRepo;
    private Election election;

    private InetAddress ip;
    private Communicator com;
    private RMSourcePrx rm;
    private RMDestinationPrx dest;
    private ObjectMapper mapper;

    public ControllerVote() {

        voteRepo = new VoteRepository();
        authVoter = new AutenticationVoter();
        view = new View();

        electionRepo = new ElectionRepository();
        election = electionRepo.getElection();

        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));
        mapper = new ObjectMapper();
    }

    public void run() throws Exception {
        ip = InetAddress.getLocalHost();
        boolean continuar = true;

        try {
            do {
                boolean isValid = true;
                String[] credentials = view.showLoginPrompt();
                String id = credentials[0];
                String password = credentials[1];

                if (!authVoter.authenticate(id, password)) {
                    view.showError("Credenciales incorrectas. Terminando.");
                    isValid = false;
                }

                if (authVoter.hasAlreadyVoted(id)) {
                    view.showAlreadyVoted();
                    isValid = false;
                }

                if (isValid) {
                    voting();
                    authVoter.markAsVoted(id);
                }

            } while (continuar);
        } finally {
            com.shutdown();
            view.close();
        }
    }

    public void voting() throws Exception {
        String opcion = view.showCandidatesAndGetChoice(election.getCandidates());
        long timestamp = System.currentTimeMillis();
        Vote nuevoVote = new Vote(ip.getHostAddress(), opcion,timestamp, election.getElectionId());
        rm.setServerProxy(dest);
        rm.sendMessage(nuevoVote);
        voteRepo.save(nuevoVote);

        System.out.println("sended");
        view.showInfo("Gracias por votar. Su elección (" + opcion + ") ha sido registrada.");
    }
}
