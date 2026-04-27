package checkin.domain.model

/**
 * REGISTRADO -> EN_SEGURIDAD -> EN_BODEGA -> EN_VEHICULO -> ENTREGADO
 *                                                        -> PERDIDO (anomalía)
 */

sealed trait EstadoEquipaje {
    def nombre: String = this.toString.toUpperCase
}

object EstadoEquipaje {
    case object Registrado extends EstadoEquipaje
    case object EnSeguridad extends EstadoEquipaje
    case object EnBodega extends EstadoEquipaje
    case object EnVehiculo extends EstadoEquipaje
    case object Entregado extends EstadoEquipaje
    case object Perdido extends EstadoEquipaje

    /**
    * Define las transiciones válidas. El Check-in solo produce REGISTRADO,
    * pero dejamos la función aquí para que otros microservicios la reutilicen.
    */
    def esTransicionValida(desde: EstadoEquipaje, hacia: EstadoEquipaje): Boolean =
        (desde, hacia) match {
            case (Registrado, EnSeguridad) => true
            case (EnSeguridad, EnBodega) => true
            case (EnBodega, EnVehiculo) => true
            case (EnVehiculo, Entregado) => true
            case (EnVehiculo, Perdido) => true
            case _ => false
        }
}