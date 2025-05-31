package controller;
import java.net.InetAddress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Util;

import Autentication.AutenticationVoter;
import model.Message;
import model.Vote;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import repository.VoteRepository;
import repository.VoterRepository;
import ui.View;

/**
 * Controlador principal que coordina la vista, la autenticación y los repositorios.
 * Flujo:
 *  1. Mostrar login (ID + password).
 *  2. Validar credenciales.
 *  3. Si no ha votado, mostrar candidatos (A/B/C).
 *  4. Grabar el voto en VoteRepository.
 *  5. Marcar en VoterRepository que el votante ya votó.
 *  6. Mensaje de agradecimiento y salir.
 */
public class ControllerVote {
    private VoterRepository voterRepo;
    private VoteRepository voteRepo;
    private AutenticationVoter authVoter;
    private View view;

    private InetAddress ip ;
    Communicator com;
    RMSourcePrx rm;
    RMDestinationPrx dest;
    ObjectMapper mapper;

    public ControllerVote() {
        // Inicializamos repositorios y autenticador
        voterRepo = new VoterRepository();
        voteRepo = new VoteRepository();
        authVoter = new AutenticationVoter(voterRepo);
        view = new View();

          
        com = Util.initialize();
        rm = RMSourcePrx.checkedCast(com.stringToProxy("Sender:tcp -h localhost -p 10010"));
        dest = RMDestinationPrx.uncheckedCast(com.stringToProxy("Service:tcp -h localhost -p 10012"));
        mapper = new ObjectMapper();
        
    }

    /**
     * Inicia el flujo completo de votación.
     */
    public void run() throws Exception{


        ip = InetAddress.getLocalHost();
        boolean continuar = true;
        boolean isValid;

        
        try {

            do {
                isValid = true;
                String[] credentials = view.showLoginPrompt();
                String id = credentials[0];
                String password = credentials[1];


                if (!authVoter.authenticate(id, password)) {
                    view.showError("Credenciales incorrectas. Terminando.");
                    isValid = false;
                }


                if ( authVoter.hasAlreadyVoted(id)  ) {
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

    public void voting() throws Exception{
        String opcion = view.showCandidatesAndGetChoice();
        Vote nuevoVote = new Vote(""+ip.getHostAddress(), opcion);
                
        voteRepo.save(nuevoVote);
        String payload = mapper.writeValueAsString(nuevoVote);
        rm.setServerProxy(dest);
        Message msg = new Message();

        msg.message = payload;
        rm.sendMessage(msg);
        System.out.println("sended");
        view.showInfo("Gracias por votar. Su elección (" + opcion + ") ha sido registrada.");
   
    }

 
}
