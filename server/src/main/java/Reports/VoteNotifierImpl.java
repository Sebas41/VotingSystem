package Reports;

import VoteNotification.VoteNotifier;
import VoteNotification.VoteObserverPrx;
import com.zeroc.Ice.Current;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class VoteNotifierImpl implements VoteNotifier {


    // Map: electionId -> Lista de observers registrados
    private final Map<Integer, List<VoteObserverPrx>> observers = new ConcurrentHashMap<>();


    @Override
    public void registerObserver(VoteObserverPrx observer, int electionId, Current current) {
        try {
            // Agregar observer a la lista de la elección
            observers.computeIfAbsent(electionId, k -> new CopyOnWriteArrayList<>()).add(observer);

            System.out.println("🔔 Nuevo proxy registrado como observer para elección " + electionId);

            // Mostrar estadísticas actuales
            int totalObservers = observers.get(electionId).size();
            System.out.println("📊 Total observers para elección " + electionId + ": " + totalObservers);

        } catch (Exception e) {
        }
    }

    @Override
    public void unregisterObserver(VoteObserverPrx observer, int electionId, Current current) {
        try {
            List<VoteObserverPrx> electionObservers = observers.get(electionId);
            if (electionObservers != null) {
                boolean removed = electionObservers.remove(observer);
                if (removed) {
                    System.out.println("👋 Proxy desregistrado de elección " + electionId);
                } else {
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public int getObserverCount(int electionId, Current current) {
        List<VoteObserverPrx> electionObservers = observers.get(electionId);
        int count = electionObservers != null ? electionObservers.size() : 0;
        return count;
    }

    @Override
    public void forceResultsUpdate(int electionId, Current current) {
        System.out.println("🔄 Actualizando resultados para elección " + electionId);

        // TODO: Implementar si necesitas forzar actualización de resultados completos
        // Por ahora solo notificamos que se solicitó la actualización
    }

    /**
     * MÉTODO PRINCIPAL: Notifica un nuevo voto a todos los observers registrados
     * Este método es llamado desde ServerControllerImpl cuando se registra un voto
     */
    public void notifyVoteReceived(String voteInfo, int electionId) {
        List<VoteObserverPrx> electionObservers = observers.get(electionId);

        if (electionObservers == null || electionObservers.isEmpty()) {
            System.out.println("📭 No hay proxys para notificar (elección " + electionId + ")");
            return;
        }

                electionObservers.size(), electionId, voteInfo);

        System.out.println("📢 Enviando notificación a " + electionObservers.size() + " proxy(s): " + voteInfo);

        // Usar una lista temporal para evitar ConcurrentModificationException
        List<VoteObserverPrx> observersToNotify = new ArrayList<>(electionObservers);
        List<VoteObserverPrx> failedObservers = new ArrayList<>();

        // Notificar a cada observer
        for (VoteObserverPrx observer : observersToNotify) {
            try {
                // Enviar notificación de voto
                observer.onVoteReceived(voteInfo);

            } catch (Exception e) {
                failedObservers.add(observer);
            }
        }

        // Remover observers que fallaron (conexión perdida)
        if (!failedObservers.isEmpty()) {
            electionObservers.removeAll(failedObservers);
            System.out.println("🧹 Removidos " + failedObservers.size() + " proxy(s) desconectado(s)");
        }
    }

    /**
     * Obtiene estadísticas detalladas de todos los observers
     */
    public String getObserverStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 ========== ESTADÍSTICAS DE OBSERVERS ==========\n");

        if (observers.isEmpty()) {
            stats.append("📭 No hay observers registrados\n");
        } else {
            int totalObservers = 0;
            for (Map.Entry<Integer, List<VoteObserverPrx>> entry : observers.entrySet()) {
                int electionId = entry.getKey();
                int count = entry.getValue().size();
                totalObservers += count;

                stats.append(String.format("🗳️ Elección %d: %d observer(s)\n", electionId, count));
            }

            stats.append(String.format("📈 Total observers: %d\n", totalObservers));
            stats.append(String.format("🎯 Elecciones monitoreadas: %d\n", observers.size()));
        }

        stats.append("====================================================\n");
        return stats.toString();
    }

    /**
     * Muestra estadísticas en consola
     */
    public void showStatistics() {
        System.out.println(getObserverStatistics());
    }

    /**
     * Limpia observers desconectados de todas las elecciones
     */
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
                        toRemove.size(), electionId);
            }
        }

        if (totalCleaned > 0) {
            System.out.println("🧹 Total observers desconectados removidos: " + totalCleaned);
        }
    }
}