package models

case class BaggageEvent(
  eventId: String,
  eventType: String,
  timestamp: String,
  baggageId: String,
  flightId: String,
  location: String,
  status: String,
  riskLevel: String,
  details: String
)