import tornadofx.*

class ParticipantForm : View("Add participant") {
    private val viewModel: ParticipantModel by inject()

    override val root = form {
        fieldset("Required") {
            field("Name") { textfield(viewModel.name).required(ValidationTrigger.OnBlur) }
            field("Category") { textfield(viewModel.category).required(ValidationTrigger.OnBlur) }
            field("Age") { textfield(viewModel.age).required(ValidationTrigger.OnBlur) }
        }
        fieldset("Optional") {
            field("Residence") { textfield(viewModel.residence) }
        }
        button("Save") {
            enableWhen(viewModel::valid)
            action(viewModel::save)
        }
        viewModel.validate(decorateErrors = false)
    }
}

class ParticipantModel(private val save: (Participant) -> Unit) : ViewModel() {
    val name = stringProperty()
    val category = stringProperty()
    val age = stringProperty()
    val residence = stringProperty()

    fun save() {
        save(Participant(name.value, category.value, age.value, residence.value))
    }
}