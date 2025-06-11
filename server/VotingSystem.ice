module VotingSystem {

    struct Citizen {
        int id;
        string documento;
        string nombre;
        string apellido;
    };

    struct Candidate {
        int id;
        string nombre;
        string partido;
    };

    sequence<int> IntSeq;
    sequence<Citizen> CitizenSeq;
    sequence<Candidate> CandidateSeq;

    struct MesaInfo {
        int mesaId;
        int mesaConsecutive;
        int puestoId;
        string puestoNombre;
        string puestoDireccion;
        int municipioId;
        string municipioNombre;
        int departamentoId;
        string departamentoNombre;
        int totalCiudadanos;
    };

    struct ElectionInfo {
        int id;
        string nombre;
        string estado;
        long fechaInicio;
        long fechaFin;
    };

    // CHANGED: VotingConfiguration is now a class that extends Value for Ice serialization
    class VotingConfiguration {
        MesaInfo mesaInfo;
        ElectionInfo electionInfo;
        CandidateSeq candidates;
        CitizenSeq citizens;
        string packageVersion;
        long generationTimestamp;
    }

    interface ConfigurationCache {
        VotingConfiguration getConfiguration(int mesaId, int electionId);
        bool isConfigurationReady(int mesaId, int electionId);
        void preloadConfigurations(IntSeq mesaIds, int electionId);
    };
};