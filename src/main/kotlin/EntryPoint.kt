import javafx.concurrent.Task
import javafx.stage.FileChooser.ExtensionFilter
import org.apache.poi.ss.usermodel.CellType
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
            val controller = try {
                createDashboardController(chosenFile.value!!)
            } catch (e: Exception) {
                busy.value = false
                throw e
            }
            setInScope(controller, scope)
            find<Dashboard>(scope)
        }
    }
}

fun createDashboardController(file: File): DashboardController {
    return WorkbookFactory.create(FileInputStream(file.path)).use {
        with(it) {
            DashboardController(
                getSheetAt(0)
                    .rowIterator()
                    .asSequence()
                    .drop(2) // header rows
                    .mapNotNull { row -> readParticipant(row) }
                    .toMutableList()
                    .toObservable(),
                getSheetAt(1)
                    .rowIterator()
                    .asSequence()
                    .mapNotNull { row -> readJury(row) }
                    .toList()
            )
        }
    }
}

fun readParticipant(row: Row): Participant? {
    val name = row.getStr(7) ?: return null
    val category = row.getStr(3) ?: return null
    val age = row.getInt(11)?.toString() ?: return null
    val facility = row.getStr(17)
    return Participant(name, category, age, facility)
}

fun readJury(row: Row): Jury? = row.cellIterator().next().stringCellValue?.nonEmpty()?.let(::Jury)

fun Row.getStr(column: Int): String? = getCell(column)?.let {
    if (it.cellType == CellType.STRING) it.stringCellValue else null
}

fun Row.getInt(column: Int): Int? = getDouble(column)?.let {
    val int = it.toInt()
    if (int.toDouble() == it) int else null
}

fun Row.getDouble(column: Int): Double? = getCell(column)?.let {
    if (it.cellType == CellType.NUMERIC) it.numericCellValue else null
}

fun String.nonEmpty(): String? = ifEmpty { null }