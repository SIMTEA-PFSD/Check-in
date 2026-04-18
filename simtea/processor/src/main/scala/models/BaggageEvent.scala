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
)