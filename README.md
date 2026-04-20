# Microservicio Check-In
### Sistema de Gestión de Equipajes Aeroportuario

Parte del proyecto de **Programación Funcional y Sistemas Distribuidos**. Este microservicio es el punto de entrada del flujo de equipajes: registra pasajeros, crea las maletas con estado inicial `REGISTRADO`, y publica los eventos `PASAJERO_REGISTRADO` al tópico `registro.pasajero` de Kafka para que los consuman Spark Streaming y los demás microservicios.

---

## 1. Arquitectura — Clean Architecture / Hexagonal

El servicio está dividido en **tres capas concéntricas**. La regla de oro: las dependencias apuntan siempre hacia adentro (infrastructure depende de application, application depende de domain, domain no depende de NADA).

```
┌─────────────────────────────────────────────────────┐
│  INFRASTRUCTURE                                     │
│  Kafka · PostgreSQL · HTTP · Config                 │
│  (Adaptadores concretos)                            │
│   ┌─────────────────────────────────────────────┐   │
│   │  APPLICATION                                │   │
│   │  CheckInUseCase · Commands · Errors         │   │
│   │  (Orquesta el dominio)                      │   │
│   │   ┌───────────────────────────────────┐     │   │
│   │   │  DOMAIN                           │     │   │
│   │   │  Pasajero · Equipaje · Vuelo ·    │     │   │
│   │   │  EventoEquipaje · Ports (traits)  │     │   │
│   │   │  (Reglas de negocio puras)        │     │   │
│   │   └───────────────────────────────────┘     │   │
│   └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### ¿Por qué sirve esto?

1. **El dominio es testeable sin red, sin DB, sin Kafka.** Se pasan mocks de los traits y listo.
2. **Si cambia Kafka por RabbitMQ, solo cambia `KafkaEventPublisher`.** El resto del código ni se entera.
3. **La lógica de negocio queda en un solo lugar** (las reglas de peso, la máquina de estados, etc.).

---

## 2. Estructura de carpetas

```
checkin-service/
├── build.sbt                          ← Dependencias y configuración SBT
├── project/build.properties           ← Versión de SBT
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   └── application.conf       ← Config externa (Kafka, etc.)
│   │   └── scala/checkin/
│   │       ├── Main.scala             ← Composition Root (conecta capas)
│   │       │
│   │       ├── domain/                ← CAPA 1 — NO depende de nadie
│   │       │   ├── model/
│   │       │   │   ├── Pasajero.scala
│   │       │   │   ├── Equipaje.scala
│   │       │   │   ├── Vuelo.scala
│   │       │   │   ├── Aerolinea.scala
│   │       │   │   └── EstadoEquipaje.scala  (enum + transiciones)
│   │       │   ├── event/
│   │       │   │   ├── TipoEvento.scala       (enum de tipos)
│   │       │   │   └── EventoEquipaje.scala   (payload Kafka)
│   │       │   └── ports/                     (traits = interfaces)
│   │       │       ├── PasajeroRepository.scala
│   │       │       ├── EquipajeRepository.scala
│   │       │       └── EventPublisher.scala
│   │       │
│   │       ├── application/           ← CAPA 2 — depende de domain
│   │       │   ├── CheckInCommand.scala
│   │       │   ├── CheckInResponse.scala
│   │       │   ├── CheckInError.scala
│   │       │   └── CheckInUseCase.scala       ← EL CORAZÓN
│   │       │
│   │       └── infrastructure/        ← CAPA 3 — adaptadores concretos
│   │           ├── config/AppConfig.scala
│   │           ├── kafka/KafkaEventPublisher.scala
│   │           └── persistence/
│   │               ├── InMemoryPasajeroRepository.scala
│   │               └── InMemoryEquipajeRepository.scala
│   └── test/scala/checkin/application/
│       └── CheckInUseCaseSpec.scala   ← Tests con mocks manuales
└── README.md
```

---

## 3. Qué hace cada capa (explicado simple)

### Domain (`domain/`)
Es el **qué** — las reglas del negocio y las entidades. Un `Pasajero` tiene `nombre`, `documento`, `email`. Un `Equipaje` solo puede nacer en estado `REGISTRADO` (mirá el factory `Equipaje.registrar()`). La máquina de estados vive aquí.

Aquí también viven los **ports** (los traits `PasajeroRepository`, `EventPublisher`). Son "huecos" que dicen: "necesito algo que sepa guardar pasajeros, pero no me importa si es Postgres o memoria". El dominio define el contrato.

**Nada de `import` de Kafka o JDBC en esta capa — si lo ves, está mal.**

### Application (`application/`)
Es el **cómo** (a alto nivel). El `CheckInUseCase` es un paso a paso:

1. Valida el comando (peso, que haya equipajes)
2. Registra el pasajero
3. Crea y persiste los equipajes
4. Publica los eventos a Kafka

Usa `for-comprehension` sobre `Either[CheckInError, X]` — si cualquier paso falla, corta-circuita y devuelve el error. Es el equivalente funcional de un `try/catch` encadenado, pero type-safe.

### Infrastructure (`infrastructure/`)
Es el **dónde** — implementaciones concretas de los puertos:

- `KafkaEventPublisher` implementa `EventPublisher` usando el cliente de Kafka.
- `InMemoryPasajeroRepository` implementa `PasajeroRepository` usando un `TrieMap`. Cuando pasen a Postgres, crean `PostgresPasajeroRepository` y cambian una línea en `Main.scala`.
- `AppConfig` lee `application.conf`.

---

## 4. Cómo correr el proyecto

### Pre-requisitos
- JDK 11 o 17
- SBT 1.9+

### Comandos

```bash
# Compilar
sbt compile

# Correr tests unitarios (no requieren Kafka)
sbt test

# Correr el servicio (requiere Kafka corriendo en localhost:9092)
sbt run

# Con Kafka remoto
KAFKA_BOOTSTRAP=kafka:9092 sbt run
```

### Levantar Kafka localmente (para probar end-to-end)

Usen el `docker-compose.yml` que tiene el equipo (el del diagrama de despliegue). Lo mínimo sería:

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Creen el tópico:

```bash
docker exec -it kafka kafka-topics --create \
  --topic registro.pasajero --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1
```

Consuman para verificar:

```bash
docker exec -it kafka kafka-console-consumer \
  --topic registro.pasajero --bootstrap-server localhost:9092 --from-beginning
```

---

## 5. Flujo completo (lo que pasa cuando corre)

```
[Terminal Check-In]                                                    [Kafka]
       │                                                                  │
       │ 1. CheckInCommand(pasajero, equipajes, vuelo)                    │
       ▼                                                                  │
┌─────────────────┐                                                       │
│ CheckInUseCase  │                                                       │
│                 │ ─── valida peso, cantidad                             │
│                 │ ─── crea Pasajero + Equipajes (estado REGISTRADO)     │
│                 │ ─── guardar() en repos                                │
│                 │ ─── EventoEquipaje.pasajeroRegistrado() × N           │
│                 │ ─── publisher.publicar() ─────────────────────────────┼──► registro.pasajero
└─────────────────┘                                                       │
       │                                                                  │
       │ CheckInResponse(ok, 2 equipajes, 2 eventos)                      │
       ▼                                                                  │
  [caller]                                                                │
```

Los consumers (Spark Streaming, microservicio de Seguridad) toman esos eventos y continúan el flujo.

---

## 6. Próximos pasos (post-MVP)

Lo que NO está implementado y puede venir después sin cambiar dominio ni caso de uso:

- Adaptador REST con `http4s` o `akka-http` (hoy solo hay `Main` con un demo)
- Repositorio real con Postgres (Doobie o Slick)
- Serialización con Avro + Schema Registry
- Métricas Prometheus
- Dead Letter Queue cuando Kafka rechaza mensajes

---

## 7. Convenciones para el equipo

- **Todo en español** (nombres, comentarios, logs) — consistencia con el resto del proyecto.
- **Inmutabilidad total**: usen `case class` y métodos `.copy()`. Nada de `var`.
- **Errores como tipos, no excepciones**: devuelvan `Either[Error, T]`.
- **Dominio puro**: si necesitan `import org.apache.kafka` en `domain/`, está mal — eso va en `infrastructure/`.