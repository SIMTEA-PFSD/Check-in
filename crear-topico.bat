@echo off
REM ═══════════════════════════════════════════════════════════════
REM  Crea el topico "registro.pasajero" en Kafka local (Windows)
REM  Correr DESPUES de `docker compose up -d`
REM ═══════════════════════════════════════════════════════════════

echo Creando topico registro.pasajero...
echo.

docker exec checkin-kafka kafka-topics --create --if-not-exists --topic registro.pasajero --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

echo.
echo Topicos disponibles:
docker exec checkin-kafka kafka-topics --list --bootstrap-server localhost:9092

echo.
pause