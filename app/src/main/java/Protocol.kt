import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Protocol {
    val json = Json {
        allowStructuredMapKeys = true
    }
}

typealias Jury = String

typealias JuryToken = String

@Serializable
data class Participant(
    val name: String,
    val category: String,
    val age: String,
    val residence: String? = null,
    val teacher: String? = null,
)

@Serializable
data class Performance(
    val id: Int,
    val participant: Participant,
    val repertoire: String,
) {
    val participantName get() = participant.name
}

@Serializable
data class PostAuth(val jury: Jury, val token: JuryToken)

@Serializable
data class GetQueue(val since: Int)

@Serializable
data class PostGrade(
    val jury: Jury,
    val token: JuryToken,
    val performance: Performance,
    val grade: Double,
    val comment: String?,
)
