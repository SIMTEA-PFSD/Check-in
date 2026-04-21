package checkin.domain.model

import java.time.Instant

final case class Vuelo(
    id: String,
    numero: String,
    origen: String,
    destino: String,
    salida: Instant,
    gate: String,
    aerolineaId: String
) {
    require(numero.nonEmpty, "El número de vuelo no puede estar vacío")
    require(origen != destino, "Origen y destino no pueden ser iguales")
}