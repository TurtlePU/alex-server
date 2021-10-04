import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import tornadofx.*

class PerformanceForm : View("Add participant") {
    private val viewModel: Model by inject()

    override val root = form {
        fieldset("Required") {
            field("Name") { requiredText(viewModel.name) }
            field("Category") { requiredText(viewModel.category) }
            field("Age") { requiredText(viewModel.age) }
            field("Repertoire") { requiredText(viewModel.repertoire) }
        }
        fieldset("Optional") {
            field("Residence") { textfield(viewModel.residence) }
        }
        button("Save") {
            enableWhen(viewModel::valid)
            action {
                viewModel.save()
                close()
            }
        }
        viewModel.validate(decorateErrors = false)
    }

    private fun EventTarget.requiredText(value: ObservableValue<String>) {
        textfield(value).required(ValidationTrigger.OnChange())
    }

    class Model(private val save: (Performance) -> Unit) : ViewModel() {
        val name = prop()
        val category = prop()
        val age = prop()
        val residence = prop()
        val repertoire = prop()

        fun save() {
            save(
                Performance(
                    Participant(name.value!!, category.value!!, age.value!!, residence.value),
                    repertoire.value!!
                )
            )
        }

        private fun prop() = property<String>().fxProperty
    }
}