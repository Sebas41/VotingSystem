package tools;

import model.Vote;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import votation.VoteRepository;

import java.net.InetAddress;

/**
 * Clase utilitaria para simular el envío masivo de votos.
 * Envía 1000 votos alternando entre los candidatos con ID 1, 2 y 3.
 * Mide y muestra el tiempo total de ejecución de la prueba.
 */
public class BulkVoteSender {

    public static void runTest() throws Exception {

        // Registrar tiempo de inicio
        long startTime = System.currentTimeMillis();

        VoteRepository repo = new VoteRepository();

        // Inicializa los proxies
        com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize();
        RMSourcePrx rm = RMSourcePrx.checkedCast(communicator.stringToProxy("Sender:tcp -h localhost -p 10010"));
        RMDestinationPrx dest = RMDestinationPrx.uncheckedCast(communicator.stringToProxy("Service:tcp -h localhost -p 10012"));
        rm.setServerProxy(dest);

        String machineIp = InetAddress.getLocalHost().getHostAddress();

        for (int i = 1; i <= 10000; i++) {
            int candidateId = 1;
            long timestamp = System.currentTimeMillis();
            Vote vote = new Vote(machineIp, String.valueOf(candidateId),timestamp, candidateId);

            rm.sendMessage(vote);
            //repo.save(vote);

        }

        communicator.shutdown();

        // Calcular y mostrar el tiempo total de ejecución
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("[BulkVoteSender] Prueba finalizada.");
        System.out.println("[BulkVoteSender] Tiempo total de ejecución: " + totalTime + " ms");
        System.out.println("[BulkVoteSender] Tiempo total de ejecución: " + (totalTime / 1000.0) + " segundos");
    }
}
