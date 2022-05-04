import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import tornadofx.*

class PerformanceForm : View("Add participant") {
    private val model: Model by inject()

    override val root = form {
        fieldset("Required") {
            field("Id") { requiredText(model.id) }
            field("Name") { requiredText(model.name) }
            field("Category") { requiredText(model.category) }
            field("Age") { requiredText(model.age) }
            field("Repertoire") { requiredText(model.repertoire) }
        }
        fieldset("Optional") {
            field("Residence") { textfield(model.residence) }
        }
        button("Save") {
            enableWhen(model::valid)
            action {
                model.save()
                close()
            }
        }
        model.validate(decorateErrors = false)
    }

    private fun EventTarget.requiredText(value: ObservableValue<String>) {
        textfield(value).required(ValidationTrigger.OnChange())
    }

    class Model(private val save: (Performance) -> Unit) : ViewModel() {
        val id = prop()
        val name = prop()
        val category = prop()
        val age = prop()
        val residence = prop()
        val repertoire = prop()

        fun save() {
            save(
                Performance(
                    id.value!!.toInt(),
                    Participant(name.value!!, category.value!!, age.value!!, residence.value),
                    repertoire.value!!
                )
            )
        }

        private fun prop() = property<String>().fxProperty
    }
}