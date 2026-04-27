# Microservicio Check-In
### Sistema de Gestión de Equipajes Aeroportuario

Parte del proyecto de **Programación Funcional y Sistemas Distribuidos**. Este microservicio es el punto de entrada del flujo de equipajes: recibe solicitudes de check-in vía HTTP REST, registra pasajeros y maletas, y publica eventos `PASAJERO_REGISTRADO` al tópico `registro.pasajero` de Kafka para que los consuman los demás microservicios del sistema (Seguridad, Bodega, Distribución) y Spark Streaming.

---

## Documentos del proyecto

| Archivo | Contenido |
|---|---|
| **[COMO_CORRER.md](./COMO_CORRER.md)** | Guía paso a paso para correr todo desde cero |
| **[INTEGRACION.md](./INTEGRACION.md)** | Contrato HTTP + Kafka para integración con el equipo |
| **README.md** (este archivo) | Arquitectura y estructura del código |

---

## 1. Arquitectura — Clean Architecture / Hexagonal

Tres capas concéntricas. Regla de oro: las dependencias apuntan siempre hacia adentro.

```
┌─────────────────────────────────────────────────────┐
│  INFRASTRUCTURE                                     │
│  HTTP (http4s) · Kafka · Persistence · Config       │
│  (Adaptadores concretos de entrada y salida)        │
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

### Flujo de una petición real

```
 ┌──────────┐   POST /api/v1/checkin   ┌──────────────────┐
 │  Cliente │ ───────────────────────► │ CheckInRoutes    │  (infrastructure/http)
 └──────────┘                          │  (http4s)        │
                                       └────────┬─────────┘
                                                │ CheckInCommand
                                                ▼
                                       ┌──────────────────┐
                                       │ CheckInUseCase   │  (application)
                                       │  · validaciones  │
                                       │  · reglas        │
                                       └──┬────────────┬──┘
                                          │            │
                                ┌─────────▼──┐   ┌────▼──────────────┐
                                │ Repository │   │ EventPublisher    │  (domain/ports)
                                │   trait    │   │   trait           │
                                └─────▲──────┘   └────▲──────────────┘
                                      │               │
                             ┌────────┴──────┐   ┌────┴──────────────┐
                             │ InMemoryRepo  │   │ KafkaPublisher    │  (infrastructure)
                             └───────────────┘   └───────┬───────────┘
                                                         │
                                                         ▼
                                                   [ Kafka Topic:
                                                     registro.pasajero ]
                                                         │
                                                         ▼
                                         (Consumers: Spark, otros microservicios)
```

---

## 2. Estructura de carpetas

```
checkin-service/
├── build.sbt                          ← Dependencias + config Docker
├── project/
│   ├── build.properties               ← Versión SBT
│   └── plugins.sbt                    ← Plugin sbt-native-packager
├── docker-compose.yml                 ← Kafka + Zookeeper + UI + servicio
├── crear-topico.sh / .bat             ← Script para crear el tópico
├── examples/                          ← Payloads JSON + scripts de prueba
│   ├── checkin-exitoso.json
│   ├── checkin-peso-excedido.json
│   ├── checkin-sin-equipajes.json
│   ├── probar-api.sh                  (Mac/Linux)
│   └── probar-api.ps1                 (Windows)
├── src/
│   ├── main/
│   │   ├── resources/application.conf ← Config externa
│   │   └── scala/checkin/
│   │       ├── Main.scala             ← IOApp que arranca servidor HTTP
│   │       │
│   │       ├── domain/                ← CAPA 1 — sin dependencias externas
│   │       │   ├── model/
│   │       │   │   ├── Pasajero.scala
│   │       │   │   ├── Equipaje.scala
│   │       │   │   ├── Vuelo.scala
│   │       │   │   ├── Aerolinea.scala
│   │       │   │   └── EstadoEquipaje.scala
│   │       │   ├── event/
│   │       │   │   ├── TipoEvento.scala
│   │       │   │   └── EventoEquipaje.scala
│   │       │   └── ports/                   (traits = interfaces)
│   │       │       ├── PasajeroRepository.scala
│   │       │       ├── EquipajeRepository.scala
│   │       │       └── EventPublisher.scala
│   │       │
│   │       ├── application/           ← CAPA 2
│   │       │   ├── CheckInCommand.scala
│   │       │   ├── CheckInResponse.scala
│   │       │   ├── CheckInError.scala
│   │       │   └── CheckInUseCase.scala
│   │       │
│   │       └── infrastructure/        ← CAPA 3
│   │           ├── config/AppConfig.scala
│   │           ├── http/                    (adapter entrada: REST)
│   │           │   ├── HttpServer.scala
│   │           │   ├── CheckInRoutes.scala
│   │           │   └── JsonCodecs.scala
│   │           ├── kafka/
│   │           │   └── KafkaEventPublisher.scala
│   │           └── persistence/
│   │               ├── InMemoryPasajeroRepository.scala
│   │               └── InMemoryEquipajeRepository.scala
│   └── test/scala/checkin/application/
│       └── CheckInUseCaseSpec.scala
└── README.md, COMO_CORRER.md, INTEGRACION.md
```

---

## 3. Stack técnico

| Tecnología | Para qué |
|---|---|
| **Scala 2.13** | Lenguaje principal |
| **http4s + Ember** | Servidor HTTP funcional |
| **cats-effect 3** | Efectos funcionales (IO) |
| **Circe** | Serialización JSON inmutable |
| **Apache Kafka 3.6** | Event bus |
| **Typesafe Config** | Configuración tipada |
| **ScalaTest** | Framework de testing |
| **sbt-native-packager** | Generación de imágenes Docker |

---

## 4. Endpoints expuestos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/health` | Health check |
| POST | `/api/v1/checkin` | Registrar un check-in |

Contrato completo con ejemplos de request/response en **[INTEGRACION.md](./INTEGRACION.md)**.

---

## 5. Cómo correr (versión rápida)

```
# 1. Arrancar Kafka
docker compose up -d

# 2. Crear tópico
.\crear-topico.bat            # Windows
bash crear-topico.sh          # Mac/Linux

# 3. Arrancar el microservicio
sbt run

# 4. Probar (en otra terminal)
.\examples\probar-api.ps1     # Windows
bash examples/probar-api.sh   # Mac/Linux
```


---

## 6. Convenciones del código

- **Inmutabilidad total**: `case class` + `.copy()`. Nada de `var`.
- **Errores como tipos, no excepciones**: `Either[CheckInError, T]` y ADTs.
- **Dominio puro**: si ves `import org.http4s` o `import org.apache.kafka` en `domain/`, está mal.
- **Todo en español** (nombres, comentarios, logs): consistencia con el proyecto.
- **Puertos = traits**: el dominio define contratos, la infraestructura los implementa.