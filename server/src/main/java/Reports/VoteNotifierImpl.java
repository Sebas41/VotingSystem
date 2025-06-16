package Reports;

import VoteNotification.VoteNotifier;
import VoteNotification.VoteObserverPrx;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class VoteNotifierImpl implements VoteNotifier {

    private static final Logger logger = LoggerFactory.getLogger(VoteNotifierImpl.class);

    // Map: electionId -> Lista de observers registrados
    private final Map<Integer, List<VoteObserverPrx>> observers = new ConcurrentHashMap<>();

    public VoteNotifierImpl() {
        logger.info(" VoteNotifier inicializado en el servidor central");
    }

    @Override
    public void registerObserver(VoteObserverPrx observer, int electionId, Current current) {
        try {
            observers.computeIfAbsent(electionId, k -> new CopyOnWriteArrayList<>()).add(observer);

            logger.info(" Observer registrado para elección {}: {}", electionId, observer);
            System.out.println(" Nuevo proxy registrado como observer para elección " + electionId);

            int totalObservers = observers.get(electionId).size();
            System.out.println("📊 Total observers para elección " + electionId + ": " + totalObservers);

        } catch (Exception e) {
            logger.error(" Error registrando observer para elección {}: {}", electionId, e.getMessage());
        }
    }

    @Override
    public void unregisterObserver(VoteObserverPrx observer, int electionId, Current current) {
        try {
            List<VoteObserverPrx> electionObservers = observers.get(electionId);
            if (electionObservers != null) {
                boolean removed = electionObservers.remove(observer);
                if (removed) {
                    logger.info(" Observer desregistrado para elección {}", electionId);
                    System.out.println(" Proxy desregistrado de elección " + electionId);
                } else {
                    logger.warn(" Observer no encontrado para desregistrar en elección {}", electionId);
                }
            }
        } catch (Exception e) {
            logger.error(" Error desregistrando observer para elección {}: {}", electionId, e.getMessage());
        }
    }

    @Override
    public int getObserverCount(int electionId, Current current) {
        List<VoteObserverPrx> electionObservers = observers.get(electionId);
        int count = electionObservers != null ? electionObservers.size() : 0;
        logger.debug("📊 Conteo de observers para elección {}: {}", electionId, count);
        return count;
    }

    @Override
    public void forceResultsUpdate(int electionId, Current current) {



    }


    public void notifyVoteReceived(String voteInfo, int electionId) {
        List<VoteObserverPrx> electionObservers = observers.get(electionId);

        if (electionObservers == null || electionObservers.isEmpty()) {
            logger.debug("📭 No hay observers registrados para elección {}", electionId);
            System.out.println("📭 No hay proxys para notificar (elección " + electionId + ")");
            return;
        }

        logger.info(" Notificando voto a {} observers para elección {}: {}",
                electionObservers.size(), electionId, voteInfo);

        System.out.println(" Enviando notificación a " + electionObservers.size() + " proxy(s): " + voteInfo);

        // Usar una lista temporal para evitar ConcurrentModificationException
        List<VoteObserverPrx> observersToNotify = new ArrayList<>(electionObservers);
        List<VoteObserverPrx> failedObservers = new ArrayList<>();

        // Notificar a cada observer
        for (VoteObserverPrx observer : observersToNotify) {
            try {
                // Enviar notificación de voto
                observer.onVoteReceived(voteInfo);
                logger.debug(" Notificación enviada exitosamente a observer: {}", observer);

            } catch (Exception e) {
                logger.warn(" Error notificando a observer (será removido): {}", e.getMessage());
                failedObservers.add(observer);
            }
        }

        // Remover observers que fallaron (conexión perdida)
        if (!failedObservers.isEmpty()) {
            electionObservers.removeAll(failedObservers);
            System.out.println(" Removidos " + failedObservers.size() + " proxy(s) desconectado(s)");
        }
    }


    public String getObserverStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("ESTADÍSTICAS DE OBSERVERS \n");

        if (observers.isEmpty()) {
            stats.append(" No hay observers registrados\n");
        } else {
            int totalObservers = 0;
            for (Map.Entry<Integer, List<VoteObserverPrx>> entry : observers.entrySet()) {
                int electionId = entry.getKey();
                int count = entry.getValue().size();
                totalObservers += count;

                stats.append(String.format(" Elección %d: %d observer(s)\n", electionId, count));
            }

            stats.append(String.format("📈 Total observers: %d\n", totalObservers));
            stats.append(String.format(" Elecciones monitoreadas: %d\n", observers.size()));
        }

        stats.append("----\n");
        return stats.toString();
    }


    public void showStatistics() {
        System.out.println(getObserverStatistics());
    }


    public void cleanDisconnectedObservers() {
        int totalCleaned = 0;

        for (Map.Entry<Integer, List<VoteObserverPrx>> entry : observers.entrySet()) {
            int electionId = entry.getKey();
            List<VoteObserverPrx> electionObservers = entry.getValue();
            List<VoteObserverPrx> toRemove = new ArrayList<>();

            for (VoteObserverPrx observer : electionObservers) {
                try {
                    // Hacer ping para verificar conexión
                    observer.ping();
                } catch (Exception e) {
                    toRemove.add(observer);
                }
            }

            if (!toRemove.isEmpty()) {
                electionObservers.removeAll(toRemove);
                totalCleaned += toRemove.size();
                logger.info(" Limpiados {} observers desconectados de elección {}",
                        toRemove.size(), electionId);
            }
        }

        if (totalCleaned > 0) {
            System.out.println(" Total observers desconectados removidos: " + totalCleaned);
        }
    }
}