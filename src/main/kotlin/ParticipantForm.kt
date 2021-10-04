import javafx.beans.value.ObservableValue
import javafx.event.EventTarget
import tornadofx.*

class ParticipantForm : View("Add participant") {
    private val viewModel: ParticipantModel by inject()

    override val root = form {
        fieldset("Required") {
            field("Name") { requiredText(viewModel.name) }
            field("Category") { requiredText(viewModel.category) }
            field("Age") { requiredText(viewModel.age) }
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
}

class ParticipantModel(private val save: (Participant) -> Unit) : ViewModel() {
    val name = prop()
    val category = prop()
    val age = prop()
    val residence = prop()

    fun save() {
        save(Participant(name.value!!, category.value!!, age.value!!, residence.value))
    }

    private fun prop() = property<String>().fxProperty
}