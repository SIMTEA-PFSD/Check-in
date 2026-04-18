package rules

object RiskRule {
  def evaluate(eventJson: String): String = {
    if (eventJson.contains("\"riskLevel\": \"ALTO\"")) {
      "ALTO"
    } else if (eventJson.contains("\"riskLevel\": \"MEDIO\"")) {
      "MEDIO"
    } else {
      "BAJO"
    }
  }
}