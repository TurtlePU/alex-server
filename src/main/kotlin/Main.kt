import javafx.beans.property.SimpleObjectProperty
import javafx.stage.FileChooser.ExtensionFilter
import tornadofx.*
import tornadofx.FileChooserMode.Single
import java.io.File

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
            enableWhen(controller.fileChosen)
            action { }
        }
    }
}

class ChooseSpreadsheetController : Controller() {
    private val chosenFile = SimpleObjectProperty<File?>()
    private val filters = arrayOf(ExtensionFilter(messages["choose.spreadsheet"], "*.xls", "*.xlsx"))

    val fileName get() = chosenFile.stringBinding { it?.name }
    val fileChosen get() = chosenFile.booleanBinding { it != null }

    fun openDialog() = chooseFile(mode = Single, filters = filters).firstOrNull()?.let { chosenFile.value = it }
}