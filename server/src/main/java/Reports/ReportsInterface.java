package Reports;

import java.io.File;
import java.util.Map;

public interface ReportsInterface {

    Map<String, Integer> getTotalVotesPerCandidate(int electionId);

    Map<String, Map<String, Integer>> getVotesPerCandidateByMachine(int electionId);

    File exportVotesPerMachineCSV(int electionId, String outputPath);

    File exportElectionResultsCSV(int electionId, String outputPath);
}
