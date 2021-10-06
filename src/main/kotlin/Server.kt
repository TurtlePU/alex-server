import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tornadofx.Controller
import java.net.InetAddress

class Server(private val port: Int = 8080) : Controller() {
    val address: String get() = "${InetAddress.getLocalHost().hostName}:$port"

    fun start() = server.start()

    fun stop() = server.stop(1000, 5000)

    private val server = embeddedServer(Netty, port = port) {}
}