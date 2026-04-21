#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  Crea el tópico "registro.pasajero" en el Kafka local.
#  Correr DESPUÉS de `docker compose up -d`.
# ═══════════════════════════════════════════════════════════════

echo "Creando tópico registro.pasajero..."

docker exec checkin-kafka kafka-topics \
  --create \
  --if-not-exists \
  --topic registro.pasajero \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

echo ""
echo "Tópicos disponibles:"
docker exec checkin-kafka kafka-topics --list --bootstrap-server localhost:9092