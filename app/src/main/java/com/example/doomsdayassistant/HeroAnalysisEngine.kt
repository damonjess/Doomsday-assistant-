package com.example.doomsdayassistant

data class AnalysisResult(
    val heroName: String,
    val recommendation: String,
    val statsFound: Map<String, String>
)

class HeroAnalysisEngine {

    fun analyze(text: String): AnalysisResult {
        // Mock implementation
        val lines = text.lines()
        var heroName = "Unknown Hero"
        val stats = mutableMapOf<String, String>()

        // Very basic heuristic: first line might be hero name
        if (lines.isNotEmpty()) {
            heroName = lines[0]
        }

        // Look for numbers that might be stats
        val numberRegex = "\\d+".toRegex()
        lines.forEach { line ->
            if (line.contains("ATK", ignoreCase = true)) {
                stats["ATK"] = numberRegex.find(line)?.value ?: "N/A"
            }
            if (line.contains("HP", ignoreCase = true)) {
                stats["HP"] = numberRegex.find(line)?.value ?: "N/A"
            }
        }

        return AnalysisResult(
            heroName = heroName,
            recommendation = "Focus on upgrading Skill 2. This hero benefits most from HP stats in the current meta.",
            statsFound = stats
        )
    }
}
