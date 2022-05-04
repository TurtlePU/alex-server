import javafx.concurrent.Task
import javafx.stage.FileChooser.ExtensionFilter
import kotlinx.serialization.decodeFromString
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import java.util.regex.Pattern

class ChooseSpreadsheet : View() {
    private val controller: Control by inject()

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

    class Control : Controller() {
        private val chosenFile = objectProperty<File?>()
        private val busy = booleanProperty(false)
        private val filters = arrayOf(
            ExtensionFilter(messages["choose.spreadsheet"], "*.xls", "*.xlsx"),
            ExtensionFilter("JSON cache", "*.json"),
        )

        val fileName get() = chosenFile.stringBinding { it?.name }
        val enableTransition get() = chosenFile.booleanBinding(busy) { it != null && !busy.value }

        fun openDialog() =
            chooseFile(mode = FileChooserMode.Single, filters = filters).firstOrNull()?.let { chosenFile.value = it }

        fun createDashboard(): Task<View> {
            busy.value = true
            return runAsync {
                val scope = Scope()
                val contestModel = try {
                    createDashboardModel(chosenFile.value!!)
                } catch (e: Exception) {
                    busy.value = false
                    throw e
                }
                setInScope(contestModel, scope)
                find<Dashboard>(scope)
            }
        }

        companion object {
            fun createDashboardModel(file: File): ContestModel {
                return if (file.extension == "json") {
                    file.bufferedReader().use {
                        ContestModel(Protocol.json.decodeFromString(it.readText()))
                    }
                } else {
                    WorkbookFactory.create(FileInputStream(file.path)).use {
                        with(it) {
                            ContestModel(
                                file,
                                rows(0).drop(3 /* header rows */).mapNotNull(::readPerformance),
                                rows(1).drop(1 /* header rows */).mapNotNull(::readJury)
                            )
                        }
                    }
                }
            }

            private fun readPerformance(row: Row): Performance? {
                val id = row.getInt(6) ?: return null
                val participant = readParticipant(row) ?: return null
                val repertoire = row.getStr(11) ?: return null
                return Performance(id, participant, repertoire)
            }

            private fun readParticipant(row: Row): Participant? {
                val name = row.getStr(10) ?: return null
                val category = row.getStr(7) ?: return null
                val age = row.getStr(8) ?: return null
                val residence = row.getStr(20)
                val teacher = row.getStr(20)
                return Participant(name, category, age, residence, teacher)
            }

            private fun readJury(row: Row): Jury? {
                val raw = row.cellIterator().next().stringCellValue?.nonEmpty() ?: return null
                return raw.split(Pattern.compile("\\s")).joinToString(" ") { it.trim() }
            }

            private fun Workbook.rows(sheet: Int): Sequence<Row> = getSheetAt(sheet).rowIterator().asSequence()

            private fun Row.getStr(column: Int): String? = getCell(column)?.let {
                if (it.cellType == CellType.STRING) it.stringCellValue else null
            }

            private fun Row.getInt(column: Int): Int? = getDouble(column)?.let {
                val int = it.toInt()
                if (int.toDouble() == it) int else null
            }

            private fun Row.getDouble(column: Int): Double? = getCell(column)?.let {
                if (it.cellType == CellType.NUMERIC) it.numericCellValue else null
            }

            private fun String.nonEmpty(): String? = ifEmpty { null }
        }
    }
}