import javafx.concurrent.Task
import javafx.stage.FileChooser.ExtensionFilter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import tornadofx.*
import tornadofx.FileChooserMode.Single
import java.io.File
import java.io.FileInputStream

fun main(args: Array<String>) {
    launch<AlexApp>(args)
}

class AlexApp : App(ChooseSpreadsheet::class)

class ChooseSpreadsheet : View() {
    private val controller: ChooseSpreadsheetController by inject()

    override val root = vbox {
        hbox {
            button(messages["choose.file"]) {
                action(controller::openDialog)
            }
            label(controller.fileName)
        }
        button(messages["choose.start"]) {
            enableWhen(controller.enableTransition)
            action { controller.createDashboard().ui { replaceWith(it) } }
        }
    }
}

class ChooseSpreadsheetController : Controller() {
    private val chosenFile = objectProperty<File?>()
    private val filters = arrayOf(ExtensionFilter(messages["choose.spreadsheet"], "*.xls", "*.xlsx"))
    private val busy = booleanProperty(false)

    val fileName get() = chosenFile.stringBinding { it?.name }
    val enableTransition get() = chosenFile.booleanBinding(busy) { it != null && !busy.value }

    fun openDialog() = chooseFile(mode = Single, filters = filters).firstOrNull()?.let { chosenFile.value = it }

    fun createDashboard(): Task<View> {
        busy.value = true
        return runAsync {
            val scope = Scope()
            val controller = createDashboardController(chosenFile.value!!)
            setInScope(controller, scope)
            find<Dashboard>(scope)
        }
    }
}

fun createDashboardController(file: File): DashboardController {
    return WorkbookFactory.create(FileInputStream(file.path)).use { with(it) {
        DashboardController(
            getSheetAt(0)
                .rowIterator()
                .asSequence()
                .drop(2)
                .mapNotNull { readParticipant() }
                .toMutableList()
                .toObservable(),
            getSheetAt(1)
                .rowIterator()
                .asSequence()
                .mapNotNull { row -> readJury(row) }
                .toList()
        )
    } }
}

fun readParticipant(): Participant? = null

fun readJury(row: Row): Jury? = row.cellIterator().next().stringCellValue?.ifNotEmpty(::Jury)

fun <T> String.ifNotEmpty(f: (String) -> T): T? = if (isNotEmpty()) f(this) else null