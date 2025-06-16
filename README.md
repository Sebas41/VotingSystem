#  VotingSystem – Sistema de Votación Distribuido

## Equipo de Desarrollo
- **Luis Manuel Rojas Correa**
- **Ricardo Andrés Chamorro**
- **Oscar Stiven Muñoz**
- **Sebastian Erazo Ochoa**

## Contexto

La Registraduría Nacional ha delegado a la empresa XYZ el desarrollo de un sistema de votación electrónica distribuido, capaz de gestionar elecciones nacionales con más de 100 millones de ciudadanos, distribuidos en miles de municipios y mesas de votación.

Este sistema debe garantizar:

- Integridad de la información electoral

- Seguridad y autenticidad del voto

- Disponibilidad continua ante fallos

- Trazabilidad completa de los procesos

Para lograrlo, se implementó un sistema modular basado en ZeroC ICE, con arquitectura distribuida, comunicación robusta entre componentes, y separación clara de responsabilidades para soportar escenarios reales de alta concurrencia y fallas parciales.

## Módulos Implementados

1. Registro y Recepción de Votos
Implementación del patrón Reliable Messaging, que garantiza la recepción única y confirmada de cada voto.

Persistencia local con reintentos automáticos ante fallas de red.

Confirmación vía ack del servidor central al proxy confiable.

2. Consulta de Lugar de Votación
Permite consultar en tiempo real el lugar de votación de un ciudadano según su documento y elección.

Implementado como una interfaz remota (ReportsService) que accede a un cache distribuido.

Incluye reportes geográficos, por ciudadano y por mesa.


## Estructura del Proyecto

```bash
VotingSystem/
├── client/                # Cliente que envía votos
├── server/                # Servidor principal que gestiona elecciones
├── reliableServer/        # Módulo que implementa Reliable Messaging
├── consultPublic/         # Consulta de reportes y mesas
├── ProxyCacheReports/     # Proxy de consultas
├── ProxyConfigMachine/    # Proxy de configuracion de mesas
├── build.gradle           # Script de construcción
├── *.ice                  # Archivos de definición de interfaces ICE
``` 

## Arquitectura y Funcionamiento

El sistema está dividido en capas funcionales y componentes distribuidos, que interactúan mediante interfaces definidas en ICE. Se destacan:

- Clientes (UI/CLI): interfaces de votación y consulta.

- Servidores confiables y centrales: para registrar y almacenar votos.

- Proxies de configuración y consulta: permiten desacoplar las consultas de datos de los servicios centrales.

La arquitectura garantiza:

- Alta disponibilidad: mediante reintentos, proxies y cacheo.

- Escalabilidad: mediante componentes distribuidos y separación de responsabilidades.

- Resiliencia: a través de mecanismos de persistencia temporal y recuperación de fallos.

---

## Patrón Reliable Messaging

El módulo `reliableServer` implementa el patrón de **Reliable Messaging** con los siguientes componentes:

### Estrategia implementada

1. **El cliente envía el voto al proxy confiable (`RMSourcePrx`)**
2. **Si el servidor está inactivo, el voto se guarda localmente en disco**
3. **Un hilo (`RMJob`) reintenta enviar todos los votos pendientes cada segundo**
4. **Cuando el servidor recibe el voto, responde con una confirmación (`ack`)**
5. **El cliente elimina de su almacenamiento el voto confirmado**

### Clases principales del patrón

| Clase                         | Rol                                                                 |
|------------------------------|----------------------------------------------------------------------|
| `RMJob`                      | Hilo que maneja reintentos y persistencia de votos no confirmados.   |
| `PendingMessageStoreMap`     | Almacena mensajes no entregados en archivo `.kryo`.                  |
| `Notification`               | Encapsula el envío de mensajes con ICE y manejo de ACK.              |
| `RMSender`                   | Recibe votos nuevos desde el cliente y los pasa a `RMJob`.           |
| `RMReciever`                 | Recibe confirmaciones (`ack`) del servidor y actualiza el estado.    |
| `ReliableServer`             | Orquesta toda la lógica: inicia proxies, adaptadores y el hilo RM.   |

## Tecnologías Utilizadas

- ZeroC ICE y Java– Middleware distribuido

- PostgreSQL – Base de datos relacional

 - JSON - Persistencia de datos y mensajes

- Gradle – Gestión del ciclo de vida del proyecto

- Arquitectura distribuida en capas – Alta cohesión y bajo acoplamiento


## Ejecución del proyecto

### 1. Compilar el proyecto

Para compilar el proyecto usa el siguiente comando:

```bash
./gradlew clean build
```
ó con:
```bash
gradle build
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

## Conclusión

El sistema desarrollado cumple con los principios de resiliencia, seguridad y escalabilidad necesarios para enfrentar una jornada electoral real. Mediante la separación de responsabilidades, uso de proxies confiables, consultas desacopladas y recuperación ante fallos, se garantiza una experiencia robusta tanto para votantes como para operadores y auditores del sistema.


