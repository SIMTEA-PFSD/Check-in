package models

import models.RiskLevel._

case class BaggageEvent(
    eventId: String,
    eventType: String,
    timestamp: String,
    baggageId: String,
    flightId: String,
    location: String,
    status: String,
    riskLevel: RiskLevel,
    details: String
) {
    def toJson: String =
    s"""{
       |  "eventId": "$eventId",
       |  "eventType": "$eventType",
       |  "timestamp": "$timestamp",
       |  "baggageId": "$baggageId",
       |  "flightId": "$flightId",
       |  "location": "$location",
       |  "status": "$status",
       |  "riskLevel": "$riskLevel",
       |  "details": "$details"
       |}""".stripMargin
}