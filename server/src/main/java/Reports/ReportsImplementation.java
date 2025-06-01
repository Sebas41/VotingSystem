package Reports;

import ConnectionDB.ConnectionDBinterface;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ReportsImplementation implements ReportsInterface {

    private final ConnectionDBinterface db;

    public ReportsImplementation(ConnectionDBinterface db) {
        this.db = db;
    }

    @Override
    public Map<String, Integer> getTotalVotesPerCandidate(int electionId) {
        Map<Integer, Integer> rawData = db.getVotesPerCandidate(electionId);
        Map<String, Integer> result = new HashMap<>();

        for (Map.Entry<Integer, Integer> entry : rawData.entrySet()) {
            String candidateName = db.getCandidateNameById(entry.getKey());
            result.put(candidateName, entry.getValue());
        }

        return result;
    }

    @Override
    public Map<String, Map<String, Integer>> getVotesPerCandidateByMachine(int electionId) {
        Map<String, Map<Integer, Integer>> rawData = db.getVotesPerCandidateGroupedByMachine(electionId);
        Map<String, Map<String, Integer>> result = new HashMap<>();

        for (String machine : rawData.keySet()) {
            Map<Integer, Integer> candidateVotes = rawData.get(machine);
            Map<String, Integer> readableVotes = new HashMap<>();

            for (Map.Entry<Integer, Integer> entry : candidateVotes.entrySet()) {
                String candidateName = db.getCandidateNameById(entry.getKey());
                readableVotes.put(candidateName, entry.getValue());
            }

            result.put(machine, readableVotes);
        }

        return result;
    }

    @Override
    public File exportVotesPerMachineCSV(int electionId, String outputPath) {
        Map<String, Map<String, Integer>> data = getVotesPerCandidateByMachine(electionId);
        File file = new File(outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Mesa,Candidato,Votos");

            for (String machine : data.keySet()) {
                for (Map.Entry<String, Integer> entry : data.get(machine).entrySet()) {
                    writer.printf("%s,%s,%d%n", machine, entry.getKey(), entry.getValue());
                }
            }

        } catch (Exception e) {
            System.err.println("Error exportando CSV por mesa: " + e.getMessage());
        }

        return file;
    }

    @Override
    public File exportElectionResultsCSV(int electionId, String outputPath) {
        Map<String, Integer> data = getTotalVotesPerCandidate(electionId);
        File file = new File(outputPath);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Candidato,Votos");

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                writer.printf("%s,%d%n", entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            System.err.println("Error exportando CSV de resultados: " + e.getMessage());
        }

        return file;
    }
}
