// ConsultPublic.ice

module ConsultPublic {
    sequence<string> StringArray;

    interface ConsultService {
        // ========== MÉTODOS PRINCIPALES ==========

        /**
         * Obtiene el reporte de un ciudadano específico
         */
        string getCitizenReport(string documento, int electionId);

        /**
         * Busca ciudadanos por nombre y apellido
         */
        StringArray searchCitizens(string nombre, string apellido, int electionId, int limit);

        /**
         * Obtiene el reporte de una elección específica
         */
        string getElectionReport(int electionId);

        /**
         * Obtiene la lista de elecciones disponibles
         */
        StringArray getAvailableElections();

        /**
         * Obtiene el reporte geográfico de una ubicación específica
         */
        string getGeographicReport(int locationId, string locationType, int electionId);

        // ========== MÉTODOS DE VALIDACIÓN ==========

        /**
         * Valida si un ciudadano es elegible para votar
         */
        bool validateCitizen(string documento);

        /**
         * Verifica si los reportes están listos para una elección
         */
        bool areReportsReady(int electionId);

        // ========== MÉTODOS DE ESTADÍSTICAS ==========

        /**
         * Obtiene estadísticas del sistema
         */
        string getSystemStats();
    };

    exception ConsultException {
        string reason;
        int errorCode;
    };
}; 