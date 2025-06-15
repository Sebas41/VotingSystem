// ConfigurationService.ice
// Interfaz Ice para recibir configuraciones de votación en las mesas

module ConfigurationSystem {



    interface ConfigurationReceiver {


        bool updateConfiguration(int mesaId, string configurationData);


        bool isReady(int mesaId);


        string getConfigurationStatus(int mesaId);
    };
};