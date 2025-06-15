// VotingSystem.ice - Versión simplificada con strings (patrón máquina de café)
#pragma once

module VotingsSystem {
    
    sequence<string> StringArray;
    sequence<int> IntSeq;
    
    interface ConfigurationService {
        

        string getConfiguration(int mesaId, int electionId);
        

        bool isConfigurationReady(int mesaId, int electionId);
        

        void preloadConfigurations(IntSeq mesaIds, int electionId);
        

        StringArray getBatchConfigurations(IntSeq mesaIds, int electionId);
        

        StringArray getDepartmentConfigurations(int departmentId, int electionId);

        StringArray getPuestoConfigurations(int puestoId, int electionId);
        

        string getConfigurationStatistics(int electionId);
        

        bool isElectionReadyForConfiguration(int electionId);
    };
    
    exception ConfigurationException {
        string reason;
        int errorCode;
    };
};