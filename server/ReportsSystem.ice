module ReportsSystem {

    struct CitizenInfo {
        int id;
        string documento;
        string nombre;
        string apellido;
    };

    struct CandidateInfo {
        int id;
        string nombre;
        string partido;
        int voteCount;
        double percentage;
    };

    struct LocationInfo {
        int departamentoId;
        string departamentoNombre;
        int municipioId;
        string municipioNombre;
        int puestoId;
        string puestoNombre;
        string puestoDireccion;
        int puestoConsecutive;
        int mesaId;
        int mesaConsecutive;
    };

    struct ElectionInfo {
        int id;
        string nombre;
        string estado;
        long fechaInicio;
        long fechaFin;
    };

    struct CitizenVotingAssignment {
        CitizenInfo citizen;
        LocationInfo location;
        ElectionInfo election;
        long generationTimestamp;
    };

    sequence<CitizenInfo> CitizenSeq;
    sequence<CandidateInfo> CandidateSeq;
    sequence<ElectionInfo> ElectionSeq;
    sequence<CitizenVotingAssignment> CitizenAssignmentSeq;
    sequence<LocationInfo> LocationSeq;

    struct ElectionResults {
        ElectionInfo election;
        CandidateSeq candidateResults;
        int totalVotes;
        long totalEligibleVoters;
        double participationPercentage;
        long generationTimestamp;
    };

    struct GeographicStats {
        LocationInfo location;
        ElectionInfo election;
        int totalMesas;
        int totalPuestos;
        int totalMunicipios; // only for department level
        long totalCitizens;
        long totalVotes;
        double participationPercentage;
        long generationTimestamp;
    };

    class CitizenReportsConfiguration {
        CitizenVotingAssignment assignment;
        ElectionSeq availableElections;
        string packageVersion;
        long generationTimestamp;
    };

    class ElectionReportsConfiguration {
        ElectionResults results;
        GeographicStats nationalStats;
        LocationSeq availableLocations;
        string packageVersion;
        long generationTimestamp;
    };

    class GeographicReportsConfiguration {
        GeographicStats stats;
        LocationSeq subLocations;
        CitizenAssignmentSeq sampleAssignments;
        string packageVersion;
        long generationTimestamp;
    };


    interface ReportsCache {
        CitizenReportsConfiguration getCitizenReports(string documento, int electionId);
        ElectionReportsConfiguration getElectionReports(int electionId);
        GeographicReportsConfiguration getGeographicReports(int locationId, string locationType, int electionId);
        bool areReportsReady(int electionId);
        void preloadReports(int electionId);
    };
};
