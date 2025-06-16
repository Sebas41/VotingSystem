
module ConfigurationSystem {



    interface ConfigurationReceiver {


        bool updateConfiguration(int mesaId, string configurationData);
        bool isReady(int mesaId);
        string getConfigurationStatus(int mesaId);
               bool updateElectionStatus(int electionId, string status);
    };
};