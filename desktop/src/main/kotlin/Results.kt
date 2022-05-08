import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.collections.ObservableSet
import javafx.scene.control.TableCell
import javafx.stage.FileChooser.ExtensionFilter
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class Results : View("Results") {
    private val model: Model by inject()

    override val root = vbox {
        tableview(model.leaderboard.toObservable()) {
            readonlyColumn("Name", PerformanceResult::participant)
            readonlyColumn("Repertoire", PerformanceResult::repertoire)
            readonlyColumn("Total", PerformanceResult::total)
            column<PerformanceResult, Boolean>("Printed") { model.isPrinted(it.value) }
                .cellFormat { printedFormat(it) }
            onDoubleClick { model.trySaveDiploma(selectedItem!!) }
        }
    }

    companion object {
        private fun TableCell<PerformanceResult, Boolean>.printedFormat(isPrinted: Boolean) {
            if (isPrinted) {
                text = "YES"
                tableRow?.removeClass(Style.enqueued)
            } else {
                text = "NO"
                tableRow?.addClass(Style.enqueued)
            }
        }
    }

    class Model(
        val leaderboard: List<PerformanceResult>,
        private val sourceFile: File,
        private val printed: ObservableSet<PerformanceResult> = mutableSetOf<PerformanceResult>().toObservable(),
        private var chosenTemplate: Document? = null,
    ) : ViewModel() {
        private val filters = arrayOf(ExtensionFilter("Text document", "*.doc", "*.docx"))

        fun isPrinted(item: PerformanceResult): BooleanBinding =
            Bindings.createBooleanBinding({ printed.contains(item) }, printed)

        fun trySaveDiploma(item: PerformanceResult) {
            val template = chosenTemplate ?: pickTemplate() ?: return
            chosenTemplate = template
            val diploma = template.fillWith(item)
            val saveTo = whereToSave(item) ?: return
            diploma.write(FileOutputStream(saveTo))
            printed.add(item)
        }

        private fun pickTemplate(): Document? =
            chooseFile(mode = FileChooserMode.Single, filters = filters).firstOrNull()?.let(::Document)

        private fun whereToSave(item: PerformanceResult): File? =
            chooseFile(
                mode = FileChooserMode.Save,
                filters = filters,
                initialDirectory = sourceFile.parentFile,
                initialFileName = item.diplomaFileName,
            ).firstOrNull()

        companion object {
            private val PerformanceResult.diplomaFileName: String get() = "$participant.$repertoire.docx"
        }
    }

    data class PerformanceResult(val performance: Performance, val total: Double) {
        val participant: String get() = performance.participantName
        val repertoire: String get() = performance.repertoire
    }

    class Document(file: File) : XWPFDocument(FileInputStream(file)) {
        private val participantParagraph = find { true }
        private val repertoireParagraph = find { true }
        private val degreeParagraph = find { true }

        fun fillWith(result: PerformanceResult): XWPFDocument {
            with(participantParagraph.createRun()) {
                setText(result.participant)
            }
            with(repertoireParagraph.createRun()) {
                setText(result.repertoire)
            }
            with(degreeParagraph.createRun()) {
                setText("TODO")
            }
            return this
        }

        companion object {
            private fun XWPFDocument.find(pred: XWPFParagraph.() -> Boolean): XWPFParagraph =
                paragraphs.find(pred) ?: createParagraph()
        }
    }
}