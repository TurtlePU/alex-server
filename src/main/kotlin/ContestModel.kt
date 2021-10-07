import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet
import tornadofx.ViewModel
import tornadofx.objectBinding
import tornadofx.onChange
import tornadofx.toObservable
import java.io.File
import java.util.*

class ContestModel(
    private val source: File,
    private val grades: WeakHashMap<Performance, PerfGrade>,
    private val queue: MutableList<Performance>,
    private val enqueued: ObservableSet<Performance>,
    private val indices: WeakHashMap<Performance, Int>,
    val performances: ObservableList<Performance>,
    val jury: List<Jury>,
) : ViewModel() {

    constructor(
        source: File,
        performances: Sequence<Performance>,
        jury: Sequence<Jury>,
        grades: Map<Performance, Map<Jury, Double?>> = mapOf(),
        queue: Sequence<Performance> = sequenceOf(),
    ) : this(
        source,
        WeakHashMap<Performance, PerfGrade>().apply { putAll(grades.mapValues { PerfGrade(it.value) }) },
        queue.toMutableList(),
        queue.toMutableSet().toObservable(),
        WeakHashMap(),
        performances.toMutableList().toObservable(),
        jury.toList(),
    )

    constructor(snapshot: Snapshot) : this(
        File(snapshot.sourcePath),
        snapshot.performances.asSequence(),
        snapshot.jury.asSequence(),
        snapshot.grades,
        snapshot.queue.asSequence()
    )

    init {
        indices.putAll(performances.indexed())
        performances.onChange {
            indices.putAll(it.addedSubList.indexed(it.from))
        }
    }

    fun snapshot(): Snapshot {
        return Snapshot(source.path, performances, jury, grades.mapValues { it.value.grades }, queue)
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

    private fun getOrCreate(performance: Performance): PerfGrade = grades.computeIfAbsent(performance) {
        PerfGrade(jury.associateWith<Jury, Double?> { null }.toObservable())
    }

    companion object {
        class PerfGrade(val grades: ObservableMap<Jury, Double?>, val mean: ObjectBinding<Double?>) {
            constructor(grades: ObservableMap<Jury, Double?>) : this(grades, objectBinding(grades, grades) {
                val values = values.filterNotNull()
                if (values.isEmpty()) null else values.sum() / values.size
            })

            constructor(grades: Map<Jury, Double?>) : this(grades.toObservable())
        }

        private fun <T> Iterable<T>.indexed(from: Int = 0) = withIndex().map { (i, x) -> x to i + from }
    }
}