# 🗳️ VotingSystem – Sistema de Votación Distribuido

## 👥 Equipo de Desarrollo
- **Luis Manuel Rojas Correa**
- **Ricardo Andrés Chamorro**
- **Oscar Stiven Muñoz**
- **Sebastian Erazo Ochoa**

## 📘 Contexto

En el marco del sistema de votaciones desarrollado para la **Registraduría Nacional**, la empresa **XYZ** ha encomendado la implementación de los **módulos responsables de transmitir y recibir los votos** generados en cada estación de votación, con destino al servidor central encargado de su consolidación y almacenamiento.

Dado que se trata de una **funcionalidad crítica**, el sistema debe garantizar de manera estricta que:

- **El 100% de los votos emitidos sean registrados correctamente**
- **Ningún voto sea contado más de una vez**

Para cumplir con estos requisitos, se diseñó e implementó una **solución robusta, segura y tolerante a fallos** basada en el patrón arquitectónico **Reliable Messaging**. Esta solución permite que los votos enviados desde estaciones de votación remotas lleguen al servidor incluso en escenarios con caídas de red, interrupciones del servidor o apagados inesperados, manteniendo siempre la **consistencia y unicidad del voto**.

Mediante mecanismos de **almacenamiento local, reintentos automáticos y confirmación de recepción**, el sistema asegura una comunicación confiable entre los nodos distribuidos, cumpliendo con principios sólidos de diseño de sistemas distribuidos seguros.


## 🧱 Estructura del Proyecto

```bash
VotingSystem/
├── client/                # Cliente que envía votos
├── server/                # Servidor principal que gestiona elecciones
├── reliableServer/        # Módulo que implementa Reliable Messaging
├── votationPoint/         # Configura y lanza votaciones
├── data/                  # Archivos persistentes (JSON, Kryo)
├── build.gradle           # Script de construcción
├── *.ice                  # Archivos de definición de interfaces ICE
``` 

## 🧩 Arquitectura y Funcionamiento

El flujo de votación se realiza en tres capas:

1. **Cliente (`client`)**  
   Autentica al votante, permite seleccionar un candidato y emitir un voto.

2. **Reliable Server (`reliableServer`)**  
   Intermediario que asegura que el voto llegue al servidor:
   - Si el servidor está caído, guarda el voto localmente.
   - Reintenta periódicamente hasta que el servidor confirme su recepción.
   - Usa ICE para enviar y recibir confirmaciones (`ack`).

3. **Servidor de votación (`server`)**  
   Recibe el voto, lo valida y lo almacena en la base de datos o en memoria.

---

## 🔄 Patrón Reliable Messaging

El módulo `reliableServer` implementa el patrón de **Reliable Messaging** con los siguientes componentes:

### 🧠 Estrategia implementada

1. **El cliente envía el voto al proxy confiable (`RMSourcePrx`)**
2. **Si el servidor está inactivo, el voto se guarda localmente en disco**
3. **Un hilo (`RMJob`) reintenta enviar todos los votos pendientes cada segundo**
4. **Cuando el servidor recibe el voto, responde con una confirmación (`ack`)**
5. **El cliente elimina de su almacenamiento el voto confirmado**

### 🧱 Clases principales del patrón

| Clase                         | Rol                                                                 |
|------------------------------|----------------------------------------------------------------------|
| `RMJob`                      | Hilo que maneja reintentos y persistencia de votos no confirmados.   |
| `PendingMessageStoreMap`     | Almacena mensajes no entregados en archivo `.kryo`.                  |
| `Notification`               | Encapsula el envío de mensajes con ICE y manejo de ACK.              |
| `RMSender`                   | Recibe votos nuevos desde el cliente y los pasa a `RMJob`.           |
| `RMReciever`                 | Recibe confirmaciones (`ack`) del servidor y actualiza el estado.    |
| `ReliableServer`             | Orquesta toda la lógica: inicia proxies, adaptadores y el hilo RM.   |


## 🛠️ Ejecución del proyecto

### 1. Compilar el proyecto

Para compilar el proyecto usa el siguiente comando:

```bash
./gradlew clean build
```

Despues de compilar el proyecto ejecuta el server para recibir y validar los votos:

### 2. Compilar el server
```bash
java -jar server/build/libs/server.jar
```

Ya iniciado el server, ejecuta el client o maquina de votación para autenticarte y registrar tu voto:

### 3. Compilar el client (maquina de votación)
```bash
java -jar client/build/libs/client.jar
``` 



