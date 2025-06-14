package org.votaciones;

import java.util.ArrayList;
import java.util.List;
import com.zeroc.Ice.*;
import ConnectionDB.ConnectionDB;
import ConnectionDB.ConnectionDBinterface;

// =================== IMPORTS PARA REPORTS ===================
import Reports.ReportsManagerImpl;
import ReportsSystem.ReportsService;

// =================== IMPORTS PARA VOTING ===================
import VotingMachineManager.VotingManagerImpl;
import VotingSystem.ConfigurationService;

import com.zeroc.Ice.Exception;

/**
 * Servidor Electoral completo que maneja tanto Reports como Voting
 * Patrón máquina de café con strings formateados
 */
public class Server {

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "electoralserver.cfg", params)) {

            System.out.println("🏛️  Iniciando Servidor Electoral...");
            System.out.println("📊 Configurando servicios Reports y Voting...");

            // =================== CONFIGURACIÓN DE ADAPTERS ===================

            // 1. Crear adapters para cada servicio
            ObjectAdapter reportsAdapter = communicator.createObjectAdapter("ReportsServer");
            ObjectAdapter votingAdapter = communicator.createObjectAdapter("VotingServer");

            // =================== INICIALIZACIÓN DE DATABASE ===================

            // 2. Inicializar la conexión a la base de datos (compartida)
            System.out.println("🔌 Conectando a la base de datos...");
            ConnectionDBinterface connectionDB = new ConnectionDB();

            // =================== SERVICIO DE REPORTES ===================

            // 3. Crear e registrar el servicio de reportes
            System.out.println("📈 Configurando servicio de Reports...");
            ReportsManagerImpl reportsManager = new ReportsManagerImpl(connectionDB);
            reportsAdapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // =================== SERVICIO DE VOTACIÓN ===================

            // 4. Crear e registrar el servicio de configuración de votación
            System.out.println("🗳️  Configurando servicio de Voting...");
            VotingManagerImpl votingManager = new VotingManagerImpl(connectionDB);
            votingAdapter.add((ConfigurationService) votingManager, Util.stringToIdentity("ConfigurationManager"));

            // =================== ACTIVACIÓN DE SERVICIOS ===================

            // 5. Activar ambos adapters
            System.out.println("🚀 Activando servicios...");
            reportsAdapter.activate();
            votingAdapter.activate();

            // =================== INFORMACIÓN DEL SERVIDOR ===================

            System.out.println();
            System.out.println("✅ ========== SERVIDOR ELECTORAL INICIADO ==========");
            System.out.println("📊 Servicio Reports: ACTIVO");
            System.out.println("   - Identity: ReportsManager");
            System.out.println("   - Formato: Strings formateados (patrón máquina de café)");
            System.out.println();
            System.out.println("🗳️  Servicio Voting: ACTIVO");
            System.out.println("   - Identity: ConfigurationManager");
            System.out.println("   - Formato: Strings formateados (patrón máquina de café)");
            System.out.println();
            System.out.println("🔌 Base de datos: CONECTADA");
            System.out.println("⏳ Esperando solicitudes de clientes...");
            System.out.println("====================================================");
            System.out.println();

            // =================== ESPERA Y SHUTDOWN ===================

            // 6. Esperar hasta que el servidor se cierre
            communicator.waitForShutdown();

            System.out.println(" Cerrando Servidor Electoral...");

        } catch (LocalException e) {
            System.err.println("❌ Error de Ice en el servidor electoral: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error general en el servidor electoral: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}