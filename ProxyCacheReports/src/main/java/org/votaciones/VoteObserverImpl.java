package org.votaciones;

import VoteNotification.VoteObserver;
import com.zeroc.Ice.Current;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

public class VoteObserverImpl implements VoteObserver {

    private static final Logger logger = LoggerFactory.getLogger(VoteObserverImpl.class);

    private final Map<String, AtomicInteger> voteCount = new ConcurrentHashMap<>();
    private final ProxyCacheReports proxyCache;
    private final AtomicInteger totalVotesReceived = new AtomicInteger(0);
    private volatile long lastVoteTimestamp = 0;

    public VoteObserverImpl(ProxyCacheReports proxyCache) {
        this.proxyCache = proxyCache;
        logger.info(" VoteObserver inicializado - MODO TIEMPO REAL");
    }

    @Override
    public void onVoteReceived(String voteInfo, Current current) {
        try {
            String[] parts = voteInfo.split("-");

            if (parts.length >= 3) {
                String candidateName = parts[0];
                long timestamp = Long.parseLong(parts[1]);
                int electionId = Integer.parseInt(parts[2]);

                voteCount.computeIfAbsent(candidateName, k -> new AtomicInteger(0)).incrementAndGet();
                totalVotesReceived.incrementAndGet();
                lastVoteTimestamp = timestamp;

                int candidateVotes = voteCount.get(candidateName).get();
                int totalVotes = totalVotesReceived.get();

                System.out.println(String.format(
                        "\n VOTO #%d RECIBIDO %s → %d votos (Elección: %d) [%s]",
                        totalVotes, candidateName, candidateVotes, electionId,
                        getCurrentTimeString()
                ));

                showCurrentDistribution();

                logger.info("VOTO #{}: {} → {} votos totales de {}",
                        totalVotes, candidateName, candidateVotes, electionId);

            } else {
                logger.warn("Formato de voto inválido: {}", voteInfo);
            }

        } catch (Exception e) {
            logger.error("Error procesando notificación de voto: {}", e.getMessage());
        }
    }

    private void showCurrentDistribution() {
        if (voteCount.isEmpty()) {
            return;
        }

        System.out.println("Distribución actual:");

        voteCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .forEach(entry -> {
                    String candidateName = entry.getKey();
                    int votes = entry.getValue().get();
                    double percentage = (votes * 100.0) / totalVotesReceived.get();

                    String bar = createProgressBar(percentage);

                    System.out.printf("   %-15s: %3d votos (%.1f%%) %s\n",
                            candidateName, votes, percentage, bar);
                });

        System.out.println("   " + "─".repeat(50));
    }

 
    private String createProgressBar(double percentage) {
        int barLength = 20;
        int filled = (int) (percentage * barLength / 100.0);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");

        return bar.toString();
    }

 
    private String getCurrentTimeString() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
    }

    @Override
    public void onElectionResultsUpdated(String resultsData, Current current) {
        try {
            logger.info("Resultados de elección actualizados: {}", resultsData);
            System.out.println("\n ========== RESULTADOS OFICIALES ACTUALIZADOS ==========");
            System.out.println("<" + resultsData + ">");
            System.out.println("===========================================================\n");
        } catch (Exception e) {
            logger.error("Error procesando actualización de resultados: {}", e.getMessage());
        }
    }

    @Override
    public boolean ping(Current current) {
        logger.debug("Ping recibido - Observer activo");
        return true;
    }

    public void showVoteSummary() {
        System.out.println("\n========== RESUMEN COMPLETO DE VOTOS ==========");

        if (voteCount.isEmpty()) {
            System.out.println("No se han recibido votos aún");
        } else {
            voteCount.entrySet().stream()
                    .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                    .forEach(entry -> {
                        String candidateName = entry.getKey();
                        int votes = entry.getValue().get();
                        double percentage = (votes * 100.0) / totalVotesReceived.get();
                        String bar = createProgressBar(percentage);

                        System.out.printf("%-15s: %3d votos (%.1f%%) %s\n",
                                candidateName, votes, percentage, bar);
                    });
        }

        System.out.printf("Total votos recibidos: %d\n", totalVotesReceived.get());
        if (lastVoteTimestamp > 0) {
            System.out.printf("Último voto: %s\n", new java.util.Date(lastVoteTimestamp));
        }
        System.out.println("=================================================\n");
    }


    public String getVoteStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("========== ESTADÍSTICAS DE VOTOS (TIEMPO REAL) ==========\n");

        stats.append(String.format("Total votos recibidos: %d\n", totalVotesReceived.get()));
        stats.append(String.format("Candidatos activos: %d\n", voteCount.size()));

        if (lastVoteTimestamp > 0) {
            stats.append(String.format("Último voto: %s\n", new java.util.Date(lastVoteTimestamp)));
        }

        stats.append("\nDistribución de votos:\n");
        voteCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().get() - a.getValue().get())
                .forEach(entry -> {
                    String name = entry.getKey();
                    int votes = entry.getValue().get();
                    double percentage = totalVotesReceived.get() > 0 ?
                            (votes * 100.0) / totalVotesReceived.get() : 0;
                    String bar = createProgressBar(percentage);

                    stats.append(String.format("   %-15s: %3d votos (%.1f%%) %s\n",
                            name, votes, percentage, bar));
                });

        return stats.toString();
    }


    public void resetCounters() {
        voteCount.clear();
        totalVotesReceived.set(0);
        lastVoteTimestamp = 0;
        logger.info(" Contadores de votos reiniciados");
        System.out.println("\n ========== CONTADORES REINICIADOS ==========\n");
    }

    public Map<String, AtomicInteger> getVoteCount() {
        return new ConcurrentHashMap<>(voteCount);
    }

    public int getTotalVotesReceived() {
        return totalVotesReceived.get();
    }
}