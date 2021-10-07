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
    private val enqueued: ObservableSet<Performance>,
    private val indices: WeakHashMap<Performance, Int>,
    private val juryTokens: MutableMap<Jury, JuryToken>,
    private val jurySet: MutableSet<Jury>,
    val queue: MutableList<Performance>,
    val performances: ObservableList<Performance>,
    val jury: List<Jury>,
) : ViewModel() {

    constructor(
        source: File,
        performances: Sequence<Performance>,
        jury: Sequence<Jury>,
        grades: Map<Performance, Map<Jury, Double?>> = mapOf(),
        queue: Sequence<Performance> = sequenceOf(),
        juryTokens: Map<Jury, JuryToken> = mapOf(),
    ) : this(
        source,
        WeakHashMap<Performance, PerfGrade>().apply { putAll(grades.mapValues { PerfGrade(it.value) }) },
        queue.toMutableSet().toObservable(),
        WeakHashMap(),
        juryTokens.toMutableMap(),
        mutableSetOf<Jury>(),
        queue.toMutableList(),
        performances.toMutableList().toObservable(),
        jury.toList(),
    )

    constructor(snapshot: Snapshot) : this(
        File(snapshot.sourcePath),
        snapshot.performances.asSequence(),
        snapshot.jury.asSequence(),
        snapshot.grades,
        snapshot.queue.asSequence(),
        snapshot.juryTokens,
    )

    init {
        indices.putAll(performances.indexed())
        performances.onChange {
            indices.putAll(it.addedSubList.indexed(it.from))
        }
        jurySet.addAll(jury)
    }

    fun snapshot(): Snapshot {
        return Snapshot(source.path, performances, jury, grades.mapValues { it.value.grades }, queue, juryTokens)
    }

    fun enqueue(performance: Performance) {
        if (enqueued.add(performance)) queue.add(performance)
    }

    fun isEnqueued(performance: Performance): BooleanBinding =
        Bindings.createBooleanBinding({ enqueued.contains(performance) }, enqueued)

    fun indexOf(performance: Performance) = SimpleObjectProperty(indices[performance]!! + 1)

    fun countTotal(performance: Performance) = getOrCreate(performance).mean

    fun authorize(jury: Jury, token: JuryToken): AuthResult {
        val oldToken = juryTokens[jury]
        return if (oldToken != null) {
            if (oldToken == token) AuthResult.LOGIN_OK else AuthResult.LOGIN_FAIL
        } else if (jurySet.contains(jury)) {
            juryTokens[jury] = token
            AuthResult.NEW_TOKEN
        } else {
            AuthResult.UNKNOWN_JURY
        }
    }

    fun grade(jury: Jury, performance: Performance, grade: Double): GradeResult {
        val data = grades[performance] ?: return GradeResult.UNKNOWN_PERFORMANCE
        data.grades[jury] = grade
        return GradeResult.GRADE_OK
    }

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

        enum class AuthResult { NEW_TOKEN, LOGIN_OK, LOGIN_FAIL, UNKNOWN_JURY }

        enum class GradeResult { GRADE_OK, UNKNOWN_PERFORMANCE }

        private fun <T> Iterable<T>.indexed(from: Int = 0) = withIndex().map { (i, x) -> x to i + from }
    }
}