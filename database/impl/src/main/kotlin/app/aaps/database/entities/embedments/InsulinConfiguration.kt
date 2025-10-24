package app.aaps.database.entities.embedments

data class InsulinConfiguration(
    var insulinLabel: String,
    var insulinEndTime: Long, // DIA before [milliseconds]
    var peak: Long, // [milliseconds]
    var concentration: Double // multiplication factor, 1.0 for U100, 2.0 for U200...
)