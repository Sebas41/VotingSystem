package org.votaciones.connection;

import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectPrx;
import com.zeroc.Ice.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyCacheConnection {
    private static final Logger logger = LoggerFactory.getLogger(ProxyCacheConnection.class);
    private static ProxyCacheConnection instance;
    private ReportsServicePrx proxyCache;
    private Communicator communicator;

    private ProxyCacheConnection() {
    }

    public static synchronized ProxyCacheConnection getInstance() {
        if (instance == null) {
            instance = new ProxyCacheConnection();
        }
        return instance;
    }

    public void connect(String host, int port) {
        try {
            communicator = Util.initialize();
            ObjectPrx base = communicator.stringToProxy(
                    String.format("ProxyCacheReports:default -h %s -p %d", host, port));
            proxyCache = ReportsServicePrx.checkedCast(base);

            if (proxyCache == null) {
                throw new RuntimeException("No se pudo conectar al proxy cache");
            }

            logger.info("Conectado exitosamente al proxy cache en {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Error al conectar con el proxy cache: {}", e.getMessage());
            throw new RuntimeException("Error de conexión", e);
        }
    }

    public ReportsServicePrx getProxyCache() {
        if (proxyCache == null) {
            throw new RuntimeException("No hay conexión activa con el proxy cache");
        }
        return proxyCache;
    }

    public void disconnect() {
        if (communicator != null) {
            try {
                communicator.destroy();
            } catch (Exception e) {
                logger.error("Error al desconectar: {}", e.getMessage());
            }
        }
    }
}