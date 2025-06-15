// ReportsSystem.ice - Versión corregida con sintaxis consistente
#pragma once

module ReportsSystem {

    sequence<string> StringArray;

    interface ReportsService {

        // ========== MÉTODOS PRINCIPALES ==========

        /**
         * Obtiene el reporte completo de un ciudadano
         */
        string getCitizenReports(string documento, int electionId);

        /**
         * Busca ciudadanos por nombre y apellido
         */
        StringArray searchCitizenReports(string nombre, string apellido, int electionId, int limit);

        /**
         * Obtiene todos los ciudadanos de una mesa
         */
        StringArray getMesaCitizenReports(int mesaId, int electionId);

        /**
         * Valida si un ciudadano es elegible para votar
         */
        bool validateCitizenEligibility(string documento);

        /**
         * Obtiene el reporte de resultados de una elección
         */
        string getElectionReports(int electionId);

        /**
         * Obtiene la lista de elecciones disponibles
         */
        StringArray getAvailableElections();

        /**
         * Verifica si los reportes están listos para una elección
         */
        bool areReportsReady(int electionId);

        /**
         * Obtiene reportes geográficos (departamento, municipio, puesto)
         */
        string getGeographicReports(int locationId, string locationType, int electionId);

        // ========== MÉTODOS DE PRECARGA ==========

        /**
         * Precarga reportes de manera inteligente según el tipo
         */
        string preloadReports(int electionId, string locationType, int locationId);

        /**
         * Obtiene estadísticas del cache
         */
        string getCacheStats();

        // ========== MÉTODOS AUXILIARES PARA PRECARGA GEOGRÁFICA ==========

        /**
         * Obtiene los documentos de todos los ciudadanos de un departamento
         */
        StringArray getDepartmentCitizenDocuments(int departmentId, int electionId);

        /**
         * Obtiene los documentos de todos los ciudadanos de un municipio
         */
        StringArray getMunicipalityCitizenDocuments(int municipalityId, int electionId);

        /**
         * Obtiene los documentos de todos los ciudadanos de un puesto
         */
        StringArray getPuestoCitizenDocuments(int puestoId, int electionId);

        /**
         * Obtiene los documentos de ciudadanos de una mesa específica
         */
        StringArray getMesaCitizenDocuments(int mesaId, int electionId);
    };

    exception ReportsException {
        string reason;
        int errorCode;
    };
}