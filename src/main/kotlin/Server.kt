import ContestModel.Companion.AuthResult.*
import ContestModel.Companion.GradeResult.GRADE_OK
import ContestModel.Companion.GradeResult.UNKNOWN_PERFORMANCE
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.Serializable
import tornadofx.Controller
import java.net.InetAddress

class Server(private val port: Int = 8080) : Controller() {
    val address: String get() = "${InetAddress.getLocalHost().hostName}:$port"

    fun start() = server.start()

    fun stop() = server.stop(1000, 5000)

    private val model: ContestModel by inject()

    private val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(AlexApp.json)
        }
        routing {
            post("/auth") {
                val (jury, token) = call.receive<PostAuth>()
                when (model.authorize(jury, token)) {
                    NEW_TOKEN -> call.respond(Created, "Added token to jury\r\n")
                    LOGIN_OK -> call.respond(OK, "Successful login\r\n")
                    LOGIN_FAIL -> call.respond(Unauthorized, "Failed to login\r\n")
                    UNKNOWN_JURY -> call.respond(Forbidden, "Unknown jury\r\n")
                }
            }

            get("/queue") {
                call.respond(model.queue)
            }

            post("/grade") {
                val (jury, token, performance, grade) = call.receive<PostGrade>()
                when (model.authorize(jury, token)) {
                    NEW_TOKEN, LOGIN_OK -> when (model.grade(jury, performance, grade)) {
                        GRADE_OK -> call.respond(OK, "Graded successfully\r\n")
                        UNKNOWN_PERFORMANCE -> call.respond(NotFound, "Performance not found\r\n")
                    }
                    LOGIN_FAIL -> call.respond(Unauthorized, "Failed to login\r\n")
                    UNKNOWN_JURY -> call.respond(Forbidden, "Unknown jury\r\n")
                }
            }
        }
    }

    companion object {
        @Serializable
        data class PostAuth(val jury: Jury, val token: JuryToken)

        @Serializable
        data class PostGrade(val jury: Jury, val token: JuryToken, val performance: Performance, val grade: Double)
    }
}