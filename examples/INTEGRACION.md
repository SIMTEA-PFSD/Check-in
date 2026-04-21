# Guía de integración · Microservicio Check-In

Este documento es **el contrato** del microservicio Check-In con el resto del sistema. Cualquier compañero de tu equipo (o consumer externo) que necesite integrarse debería leer este archivo.

---

## 1. ¿Qué hace y qué NO hace este microservicio?

**Sí hace:**
- Expone un endpoint HTTP REST para recibir solicitudes de check-in.
- Valida los datos (peso de maletas ≤ 23 kg, al menos 1 maleta por pasajero).
- Persiste el pasajero y las maletas (por ahora en memoria — MVP).
- **Publica un evento `PASAJERO_REGISTRADO` por cada maleta** en el tópico Kafka `registro.pasajero`.

**No hace (es responsabilidad de otros microservicios):**
- Escaneo RFID real → Microservicio de Seguridad.
- Asignación a bodega o vehículo → Microservicios de Aerolínea / Distribución.
- Detección de anomalías → Spark Streaming.
- Reportes → Spark Batch.

---

## 2. Contrato HTTP

Base URL en desarrollo: `http://localhost:8081`

### 2.1 · `GET /health`

Health check. Útil para orquestadores (Docker, K8s) y dashboards.

**Respuesta 200 OK:**
```json
{ "status": "UP", "service": "checkin" }
```

### 2.2 · `POST /api/v1/checkin`

Registra un check-in completo.

**Request body:**
```json
{
  "pasajeroId":     "p-001",
  "nombrePasajero": "Paula Valentina Lozano",
  "documento":      "1000123456",
  "email":          "vale@example.com",
  "vueloId":        "AV8321",
  "equipajes": [
    { "codigoRFID": "RFID-001", "peso": 18.5 },
    { "codigoRFID": "RFID-002", "peso": 21.0 }
  ]
}
```

**Respuesta 201 Created (éxito):**
```json
{
  "pasajeroId":           "p-001",
  "vueloId":              "AV8321",
  "equipajesRegistrados": ["b1f3-...", "c9a2-..."],
  "eventosPublicados":    2
}
```

**Respuesta 400 Bad Request (validación):**
```json
{
  "error":   "EquipajeExcedePeso",
  "mensaje": "Equipaje excede el peso máximo permitido: 30.5kg"
}
```

Posibles valores de `error`:

| error | Significado |
|---|---|
| `EquipajeExcedePeso` | Alguna maleta > 23 kg |
| `SinEquipajes` | Lista `equipajes` vacía |
| `PasajeroInvalido` | Datos del pasajero inválidos |
| `ErrorDeDeserializacion` | JSON malformado |

**Respuesta 500 Internal Server Error:**
```json
{
  "error":   "ErrorPublicacionEvento",
  "mensaje": "Fallo enviando a Kafka: ..."
}
```

---

## 3. Contrato Kafka

### 3.1 · Tópico

**Nombre:** `registro.pasajero`
**Particiones:** 3 (recomendado)
**Replication factor:** 1 en dev, 3 en producción
**Key:** `equipajeId` (UUID) — garantiza orden por maleta
**Value:** JSON (UTF-8)

### 3.2 · Schema del evento

```json
{
  "eventId":    "550e8400-e29b-41d4-a716-446655440000",
  "tipo":       "PasajeroRegistrado",
  "equipajeId": "b1f3c4d5-6789-4abc-8def-1234567890ab",
  "timestamp":  "2026-04-20T14:32:18.123Z",
  "topico":     "registro.pasajero",
  "payload": {
    "pasajeroId": "p-001",
    "vueloId":    "AV8321",
    "equipajeId": "b1f3c4d5-..."
  }
}
```

**Notas para los consumers:**
- Por cada maleta que el pasajero registra se emite **un evento separado**.
- El timestamp está en UTC, formato ISO-8601.
- `eventId` es único — úsalo como idempotency key si tu consumer repite.

### 3.3 · Otros tópicos (fuera del alcance de Check-In)

Referencia del diagrama del equipo:

| Tópico | Productor | Consumer |
|---|---|---|
| `registro.pasajero` | **Check-In (este)** | Seguridad, Spark Streaming |
| `equipaje.seguridad` | Seguridad | Aerolínea, Spark Streaming |
| `equipaje.bodega.*` | Aerolínea, Distribución | Spark Streaming |
| `vuelo.eventualidades` | Spark Streaming | Todos los microservicios |
| `kafka.logs` | Todos | Spark Batch |

---

## 4. Configuración — cómo apuntar a otro Kafka

Todos los valores son sobrescribibles por variables de entorno:

| Variable | Default | Descripción |
|---|---|---|
| `HTTP_PORT` | `8081` | Puerto donde escucha el REST |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Broker Kafka |
| `KAFKA_TOPIC` | `registro.pasajero` | Tópico de salida |

Ejemplo apuntando al Kafka compartido del equipo:

```bash
KAFKA_BOOTSTRAP=kafka-team.compartido:9092 sbt run
```

---

## 5. Dockerización del microservicio

### 5.1 · Generar la imagen

Desde la raíz del proyecto:

```bash
sbt Docker/publishLocal
```

Genera la imagen `checkin-service:0.1.0` (+ `latest`) en tu Docker local.

Verificar:
```bash
docker images | grep checkin-service
```

### 5.2 · Correrla sola

```bash
docker run -d --name checkin \
  -p 8081:8081 \
  -e KAFKA_BOOTSTRAP=host.docker.internal:9092 \
  checkin-service:0.1.0
```

`host.docker.internal` le permite al contenedor alcanzar el Kafka que corre en el host.

### 5.3 · Correrla con el docker-compose del proyecto

El `docker-compose.yml` tiene el servicio detrás del profile `app`:

```bash
# Levanta TODO: Kafka + UI + microservicio
docker compose --profile app up -d
```

### 5.4 · Integrar con el `docker-compose` del equipo

Si tu equipo tiene un `docker-compose.yml` global (con todos los microservicios), agregá este bloque:

```yaml
  checkin-service:
    image: checkin-service:0.1.0
    ports:
      - "8081:8081"
    environment:
      KAFKA_BOOTSTRAP: "kafka:29092"   # nombre del servicio Kafka en SU compose
      KAFKA_TOPIC: "registro.pasajero"
    depends_on:
      - kafka
```

Importante: cada compañero debe haber corrido `sbt Docker/publishLocal` en su equipo local para que la imagen exista. Más adelante pueden publicar a un registry (Docker Hub, GitHub Container Registry) para compartirla.

---

## 6. Acuerdos a cerrar con el equipo

Hablen y acuerden **antes de integrar**:

1. **Dirección del Kafka**. ¿Cada uno levanta el suyo, o hay UNO compartido? Si hay uno compartido, ¿dónde corre?
2. **Nombres de tópicos**. Los que están en el diagrama (`registro.pasajero`, `equipaje.seguridad`, etc.). Dejar fijos para que no se rompan los consumers.
3. **Schema de los eventos**. Confirmen el formato del JSON de este doc con los demás productores. Todos deberían emitir eventos con la misma forma (`eventId`, `tipo`, `timestamp`, `payload`).
4. **Puertos HTTP**. Cada microservicio debería usar uno distinto: Check-In `8081`, Seguridad `8082`, Aerolínea `8083`, etc.
5. **Base de datos**. ¿Cada servicio tiene su propia DB o comparten una (con schemas separados)?

---

## 7. Ejemplos rápidos con curl

```bash
# Linux/Mac
curl -X POST http://localhost:8081/api/v1/checkin \
  -H "Content-Type: application/json" \
  -d @examples/checkin-exitoso.json
```

```powershell
# Windows PowerShell
$body = Get-Content -Raw .\examples\checkin-exitoso.json
Invoke-RestMethod -Uri http://localhost:8081/api/v1/checkin `
  -Method Post -ContentType "application/json" -Body $body
```

O corré el script automatizado:

```bash
bash examples/probar-api.sh        # Linux/Mac
.\examples\probar-api.ps1          # Windows
```

---

## 8. Verificación end-to-end

Flujo de prueba completo:

1. `docker compose up -d` → Kafka + UI arriba.
2. `bash crear-topico.sh` o `.\crear-topico.bat` → crear tópico.
3. `sbt run` → microservicio arriba.
4. Ejecutar `.\examples\probar-api.ps1` → 4 peticiones HTTP.
5. Abrir `http://localhost:8080` → en el tópico `registro.pasajero` deberías ver los 2 eventos publicados (del primer caso exitoso).

Si algún compañero tuyo tiene un consumer conectado a ese tópico, **sus logs tendrían que mostrar que recibió los eventos**. Eso es la integración end-to-end funcionando.