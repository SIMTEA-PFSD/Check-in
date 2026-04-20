package checkin.domain.model

final case class Aerolinea(
    id: String,
    nombre: String,
    codigo: String
) {
    require(codigo.length == 2 || codigo.length == 3,
        s"Código IATA/OACI debe tener 2 o 3 caracteres: $codigo")
}