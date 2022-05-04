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
import org.slf4j.event.Level
import tornadofx.Controller
import java.net.NetworkInterface

class Server(private val port: Int = 8080) : Controller() {
    val address: String get() = "$ip:$port"

    private val ip: String get() {
        for (iface in NetworkInterface.networkInterfaces()) {
            for (addr in iface.inetAddresses()) {
                if (!addr.isLoopbackAddress && addr.hostAddress.find { it == ':' } == null) {
                    return addr.hostAddress
                }
            }
        }
        return ""
    }

    fun start() = server.start()

    fun stop() = server.stop(1000, 5000)

    private val model: ContestModel by inject()

    private val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Protocol.json)
        }
        install(CallLogging) {
            level = Level.INFO
        }
        routing {
            post("/auth") {
                println(call.request)
                val (jury, token) = call.receive<PostAuth>()
                when (model.authorize(jury, token)) {
                    NEW_TOKEN -> call.respond(Created, "Added token to jury\r\n")
                    LOGIN_OK -> call.respond(OK, "Successful login\r\n")
                    LOGIN_FAIL -> call.respond(Unauthorized, "Failed to login\r\n")
                    UNKNOWN_JURY -> call.respond(Forbidden, "Unknown jury\r\n")
                }
            }

            get("/queue") {
                val since = call.receive<GetQueue>()
                call.respond(model.queue.drop(since.since))
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
}
