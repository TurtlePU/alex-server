import javafx.beans.binding.*
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.*
import tornadofx.*
import java.util.*

class ContestModel(
    private val grades: WeakHashMap<Performance, Data>,
    private val queue: MutableList<Performance>,
    private val enqueued: ObservableSet<Performance>,
    private val indices: WeakHashMap<Performance, Int>,
    val performances: ObservableList<Performance>,
    val jury: List<Jury>,
) : ViewModel() {
    constructor(
        performances: Sequence<Performance>,
        jury: Sequence<Jury>,
        grades: Sequence<Pair<Performance, Data>> = sequenceOf(),
        queue: Sequence<Performance> = sequenceOf(),
    ) : this(
        WeakHashMap<Performance, Data>().apply { putAll(grades) },
        queue.toMutableList(),
        queue.toMutableSet().toObservable(),
        WeakHashMap(),
        performances.toMutableList().toObservable(),
        jury.toList(),
    )

    init {
        indices.putAll(performances.indexed())
        performances.onChange {
            indices.putAll(it.addedSubList.indexed(it.from))
        }
    }

    fun enqueue(performance: Performance) {
        if (enqueued.add(performance)) queue.add(performance)
    }

    fun isEnqueued(performance: Performance): BooleanBinding =
        Bindings.createBooleanBinding({ enqueued.contains(performance) }, enqueued)

    fun indexOf(performance: Performance) = SimpleObjectProperty(indices[performance]!! + 1)

    fun countTotal(performance: Performance) = getOrCreate(performance).mean

    fun viewGrade(jury: Jury, performance: Performance): ObjectBinding<Double?> =
        Bindings.valueAt(getOrCreate(performance).grades, jury)

    private fun getOrCreate(performance: Performance): Data = grades.computeIfAbsent(performance) {
        val grades = jury.associateWith<Jury, Double?> { null }.toObservable()
        val mean = objectBinding(grades, grades) {
            val values = values.filterNotNull()
            if (values.isEmpty()) null else values.sum() / values.size
        }
        Data(mean, grades)
    }

    companion object {
        class Data(val mean: ObjectBinding<Double?>, val grades: ObservableMap<Jury, Double?>)

        private fun <T> Iterable<T>.indexed(from: Int = 0) = withIndex().map { (i, x) -> x to i + from }
    }
}