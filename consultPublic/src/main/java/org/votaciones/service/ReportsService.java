package org.votaciones.service;

import ReportsSystem.ReportsServicePrx;
import org.votaciones.connection.ProxyCacheConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportsService {
    private static final Logger logger = LoggerFactory.getLogger(ReportsService.class);
    private final ProxyCacheConnection connection;

    public ReportsService() {
        this.connection = ProxyCacheConnection.getInstance();
    }

    public void connect(String host, int port) {
        connection.connect(host, port);
    }

    public String getCitizenReports(String documento, int electionId) {
        try {
            logger.info("Consultando reporte del ciudadano {} para elección {}", documento, electionId);
            return connection.getProxyCache().getCitizenReports(documento, electionId);
        } catch (Exception e) {
            logger.error("Error al obtener reporte del ciudadano: {}", e.getMessage());
            throw new RuntimeException("Error al consultar reporte", e);
        }
    }

    public String[] searchCitizenReports(String nombre, String apellido, int electionId, int limit) {
        try {
            logger.info("Buscando ciudadanos con nombre={}, apellido={}, elección={}, límite={}",
                    nombre, apellido, electionId, limit);
            return connection.getProxyCache().searchCitizenReports(nombre, apellido, electionId, limit);
        } catch (Exception e) {
            logger.error("Error al buscar ciudadanos: {}", e.getMessage());
            throw new RuntimeException("Error en búsqueda", e);
        }
    }

    public String getElectionReports(int electionId) {
        try {
            logger.info("Consultando reporte de elección {}", electionId);
            return connection.getProxyCache().getElectionReports(electionId);
        } catch (Exception e) {
            logger.error("Error al obtener reporte de elección: {}", e.getMessage());
            throw new RuntimeException("Error al consultar reporte", e);
        }
    }

    public String getGeographicReports(int locationId, String locationType, int electionId) {
        try {
            logger.info("Consultando reporte geográfico: tipo={}, id={}, elección={}",
                    locationType, locationId, electionId);
            return connection.getProxyCache().getGeographicReports(locationId, locationType, electionId);
        } catch (Exception e) {
            logger.error("Error al obtener reporte geográfico: {}", e.getMessage());
            throw new RuntimeException("Error al consultar reporte", e);
        }
    }

    public String[] getAvailableElections() {
        try {
            logger.info("Consultando elecciones disponibles");
            return connection.getProxyCache().getAvailableElections();
        } catch (Exception e) {
            logger.error("Error al obtener elecciones disponibles: {}", e.getMessage());
            throw new RuntimeException("Error al consultar elecciones", e);
        }
    }

    public boolean validateCitizenEligibility(String documento) {
        try {
            logger.info("Validando elegibilidad del ciudadano {}", documento);
            return connection.getProxyCache().validateCitizenEligibility(documento);
        } catch (Exception e) {
            logger.error("Error al validar elegibilidad: {}", e.getMessage());
            throw new RuntimeException("Error en validación", e);
        }
    }

    public String[] getMesaCitizenReports(int mesaId, int electionId) {
        try {
            logger.info("Consultando ciudadanos de mesa {} para elección {}", mesaId, electionId);
            return connection.getProxyCache().getMesaCitizenReports(mesaId, electionId);
        } catch (Exception e) {
            logger.error("Error al obtener reporte de mesa: {}", e.getMessage());
            throw new RuntimeException("Error al consultar reporte", e);
        }
    }

    public String getCacheStats() {
        try {
            logger.info("Consultando estadísticas del cache");
            return connection.getProxyCache().getCacheStats();
        } catch (Exception e) {
            logger.error("Error al obtener estadísticas: {}", e.getMessage());
            throw new RuntimeException("Error al consultar estadísticas", e);
        }
    }

    public void disconnect() {
        connection.disconnect();
    }
}