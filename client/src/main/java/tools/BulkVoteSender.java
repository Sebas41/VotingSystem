package tools;

import model.Vote;
import reliableMessage.RMDestinationPrx;
import reliableMessage.RMSourcePrx;
import votation.VoteRepository;

import java.net.InetAddress;

/**
 * Clase utilitaria para simular el envío masivo de votos.
 * Permite configurar el número de votos a enviar.
 * Mide y muestra el tiempo total de ejecución de la prueba.
 */
public class BulkVoteSender {

    public static void runTest(int numIterations) throws Exception {
        if (numIterations <= 0) {
            throw new IllegalArgumentException("El número de iteraciones debe ser mayor a 0");
        }

        long startTime = System.currentTimeMillis();

        VoteRepository repo = new VoteRepository();



        com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize();
        RMSourcePrx rm = RMSourcePrx.checkedCast(communicator.stringToProxy("Sender:tcp -h 192.168.131.104 -p 10010"));
        RMDestinationPrx dest = RMDestinationPrx
                .uncheckedCast(communicator.stringToProxy("Service:tcp -h 192.168.131.101 -p 10012"));
        rm.setServerProxy(dest);

        String machineIp = InetAddress.getLocalHost().getHostAddress();

        for (int i = 1; i <= numIterations; i++) {
            int candidateId = (i % 3) + 1; 
            long timestamp = System.currentTimeMillis();
            Vote vote = new Vote(machineIp, String.valueOf(candidateId), timestamp, candidateId);

            rm.sendMessage(vote);

        }

        communicator.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("[BulkVoteSender] Prueba finalizada.");
        System.out.println("[BulkVoteSender] Número de votos enviados: " + numIterations);
        System.out.println("[BulkVoteSender] Tiempo total de ejecución: " + totalTime + " ms");
        System.out.println("[BulkVoteSender] Tiempo total de ejecución: " + (totalTime / 1000.0) + " segundos");
        System.out.println(
                "[BulkVoteSender] Promedio de tiempo por voto: " + (totalTime / (double) numIterations) + " ms");
    }
}