import io.ktor.server.engine.*
import io.ktor.server.netty.*
import tornadofx.*
import java.net.InetAddress

class Dashboard : View("My View") {
    private val controller: DashboardController by inject()

    override val root = borderpane {
        top = menubar {
            menu("File") {
                item("Quit", "Shortcut+Shift+Q").action {
                    replaceWith<Results>()
                    close()
                }
            }
            menu(controller.serverAddress)
        }
        bottom = button(messages["preview.results"]) {
            action { openInternalWindow(controller.createPreview()) }
        }
    }

    init {
        whenDocked { controller.startServer() }
        whenUndocked { controller.stopServer() }
    }
}

class DashboardController : Controller() {
    val serverAddress: String get() = "${InetAddress.getLocalHost().hostName}:$port"

    fun createPreview() = find<Results>()

    fun startServer() = runAsync { server.start() }

    fun stopServer() = runAsync { server.stop(1000, 5000) }

    private val port = 8080
    private val server = embeddedServer(Netty, port = port) {}
}