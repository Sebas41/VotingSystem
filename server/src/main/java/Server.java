package org.votaciones;

import java.util.ArrayList;
import java.util.List;
import com.zeroc.Ice.*;
import ConnectionDB.ConnectionDB;
import ConnectionDB.ConnectionDBinterface;
import Reports.ReportsManagerImpl;
import ReportsSystem.ReportsService;
import com.zeroc.Ice.Exception;

public class Server{

    public static void main(String[] args) {
        List<String> params = new ArrayList<>();

        try (Communicator communicator = Util.initialize(args, "reportsserver.cfg", params)) {

            // 1. Crear el adapter del servidor
            ObjectAdapter adapter = communicator.createObjectAdapter("ReportsServer");

            // 2. Inicializar la conexión a la base de datos
            ConnectionDBinterface connectionDB = new ConnectionDB();

            // 3. Crear la implementación del servicio de reportes
            ReportsManagerImpl reportsManager = new ReportsManagerImpl(connectionDB);

            // 4. Registrar el servicio en el adapter con su identity
            adapter.add((ReportsService) reportsManager, Util.stringToIdentity("ReportsManager"));

            // 5. Activar el adapter
            adapter.activate();

            System.out.println("Reports Server iniciado correctamente...");
            System.out.println("Esperando solicitudes de clientes...");

            // 6. Esperar hasta que el servidor se cierre
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("Error en el servidor de reportes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}