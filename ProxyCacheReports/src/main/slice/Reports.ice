#pragma once

// =================== STANDALONE REPORTS MODULE ===================
// Versión completamente independiente sin dependencias externas

module Reports {

    // =================== BASIC ICE TYPES ===================

    sequence<byte> ByteSeq;
    sequence<string> StringSeq;
    dictionary<string, string> PropertyDict;
    dictionary<string, ByteSeq> FilesDict;  // ✅ FIXED: Definir el dictionary como tipo

    // =================== BASIC DATA STRUCTURES ===================

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

    // =================== SEQUENCES ===================

    sequence<CitizenInfo> CitizenSeq;
    sequence<CandidateInfo> CandidateSeq;
    sequence<ElectionInfo> ElectionSeq;
    sequence<CitizenVotingAssignment> CitizenAssignmentSeq;
    sequence<LocationInfo> LocationSeq;

    // =================== COMPLEX STRUCTURES ===================

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

    // =================== CONFIGURATION CLASSES ===================

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

    // =================== BASE CACHE INTERFACE ===================

    /**
     * Base interface for reports caching
     */
    interface ReportsCache {
        CitizenReportsConfiguration getCitizenReports(string documento, int electionId);
        ElectionReportsConfiguration getElectionReports(int electionId);
        GeographicReportsConfiguration getGeographicReports(int locationId, string locationType, int electionId);
        bool areReportsReady(int electionId);
        void preloadReports(int electionId);
        void syncWithMainServer();
    };

    // =================== EXTENDED MANAGER INTERFACE ===================

    /**
     * Extended interface for the main server Reports Manager
     * Adds file serving capabilities to the base ReportsCache interface
     */
    interface ReportsManagerInterface extends ReportsCache {

        // =================== FILE SERVING METHODS ===================

        /**
         * Get existing citizen report file as binary data
         */
        ByteSeq getCitizenReportFile(string documento, int electionId);

        /**
         * Get existing election report file as binary data
         */
        ByteSeq getElectionReportFile(int electionId);

        /**
         * Get existing geographic report file as binary data
         */
        ByteSeq getGeographicReportFile(int locationId, string locationType, int electionId);

        /**
         * List available report files for synchronization
         */
        StringSeq getAvailableReportFiles(string reportType, int electionId);

        /**
         * Get file metadata for synchronization
         */
        PropertyDict getReportFileMetadata(string fileName);

        /**
         * Check if specific report file exists
         */
        bool reportFileExists(string fileName);

        /**
         * Get bulk report files for initial synchronization
         * Returns a dictionary where key=filename, value=file_data_bytes
         */
        FilesDict getBulkReportFiles(StringSeq fileNames);

        // =================== CACHE MANAGEMENT ===================

        /**
         * Force refresh of cached reports
         */
        void refreshCache(int electionId);

        /**
         * Get cache statistics
         */
        PropertyDict getCacheStats();

        /**
         * Clear cache for specific election
         */
        void clearCache(int electionId);
    };

};