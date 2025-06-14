// ReportsSystem.ice - Versión simplificada con strings (patrón máquina de café)
#pragma once

module ReportsSystem {
    
    sequence<string> StringArray;
    
    interface ReportsService {
        

        string getCitizenReports(string documento, int electionId);
        

        StringArray searchCitizenReports(string nombre, string apellido, int electionId, int limit);
        

        StringArray getMesaCitizenReports(int mesaId, int electionId);
        

        bool validateCitizenEligibility(string documento);
        

        string getElectionReports(int electionId);
        

        StringArray getAvailableElections();

        bool areReportsReady(int electionId);
        

        string getGeographicReports(int locationId, string locationType, int electionId);
        

        void preloadReports(int electionId);

        int preloadDepartmentCitizens(int departmentId, int electionId);


        int preloadMunicipalityCitizens(int municipalityId, int electionId);


        int preloadPuestoCitizens(int puestoId, int electionId);

        int preloadMesasCitizens(int* mesaIds, int electionId);













    };
    
    exception ReportsException {
        string reason;
        int errorCode;
    };
}