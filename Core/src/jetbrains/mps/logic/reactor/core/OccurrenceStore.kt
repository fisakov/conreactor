package jetbrains.mps.logic.reactor.core

import com.github.andrewoma.dexx.collection.ConsList
import com.github.andrewoma.dexx.collection.Maps
import com.github.andrewoma.dexx.collection.Map as PersMap
import jetbrains.mps.logic.reactor.evaluation.ConstraintOccurrence
import jetbrains.mps.logic.reactor.evaluation.EvaluationSession
import jetbrains.mps.logic.reactor.logical.Logical
import jetbrains.mps.logic.reactor.logical.LogicalContext
import jetbrains.mps.logic.reactor.program.Constraint
import jetbrains.mps.logic.reactor.program.ConstraintSymbol
import java.util.*

/**
 * @author Fedor Isakov
 */

fun Constraint.occurrence(proxy: LogicalObserverProxy, context: LogicalContext): ConstraintOccurrence =
    MemConstraintOccurrence(proxy, this, occurrenceArguments(context))

fun ConstraintOccurrence.isStored(): Boolean =
    // TODO: superfluous cast
    (this as StoreItem).stored

interface StoreItem {
    var alive: Boolean
    var stored: Boolean
    fun terminate(): Unit
}

interface OccurrenceIndex {

    fun lookupOccurrences(
        symbol: ConstraintSymbol,
        logicals: Iterable<Logical<*>>,
        values: Iterable<Any> = emptyList(),
        acceptable: (ConstraintOccurrence) -> Boolean): Sequence<ConstraintOccurrence>

}

class OccurrenceStore : LogicalObserver, OccurrenceIndex {

    val proxy: LogicalObserverProxy

    lateinit var symbol2occurrences: PersMap<ConstraintSymbol, ConsList<ConstraintOccurrence>>

    lateinit var logical2occurrences: PersMap<Logical<*>, ConsList<ConstraintOccurrence>>

    lateinit var value2occurrences: PersMap<Any, ConsList<ConstraintOccurrence>>

    constructor(copyFrom: OccurrenceStore, proxy: LogicalObserverProxy)
    {
        this.proxy = proxy
        this.symbol2occurrences = copyFrom.symbol2occurrences
        this.logical2occurrences = copyFrom.logical2occurrences
        this.value2occurrences = copyFrom.value2occurrences
    }

    constructor(proxy: LogicalObserverProxy) {
        this.proxy = proxy
        this.symbol2occurrences = Maps.of()
        this.logical2occurrences = Maps.of()
        this.value2occurrences = Maps.of()
    }

    override fun valueUpdated(logical: Logical<*>) { /* ignore */ }

    override fun parentUpdated(logical: Logical<*>) {
        // TODO: should we care about the order in which occurrences are stored?
        logical2occurrences[logical]?.let { toMerge ->
            var newList = logical2occurrences[logical.findRoot()] ?: emptyConsList()
            for (log in toMerge) {
                newList = newList.prepend(log)
            }
            this.logical2occurrences = logical2occurrences.put(logical.findRoot(), newList)
        }

        this.logical2occurrences = logical2occurrences.remove(logical)
    }

    fun storeAll(all: Iterable<ConstraintOccurrence>): Unit {
        for(occ in all) {
            store(occ)
        }
    }

    fun store(occ: ConstraintOccurrence): Unit {
        val symbol = occ.constraint().symbol()

        this.symbol2occurrences = symbol2occurrences.put(symbol,
            symbol2occurrences[symbol]?.prepend(occ) ?: cons(occ))

        for (arg in occ.arguments()) {
            when (arg) {
                is Logical<*>               -> {
                    this.logical2occurrences = logical2occurrences.put(arg.findRoot(),
                        logical2occurrences[arg.findRoot()]?.prepend(occ) ?: cons(occ))

                    proxy.addObserver(arg, this)

                }
                else                        -> {
                    this.value2occurrences = value2occurrences.put(arg!!,
                        value2occurrences[arg]?.prepend(occ) ?: cons(occ))
                }
            }
        }

        // TODO: superfluous cast
        (occ as StoreItem).stored = true
    }

    fun discard(occ: ConstraintOccurrence): Unit {
        val symbol = occ.constraint().symbol()

        symbol2occurrences[symbol].remove(occ)?.let { newList ->
            this.symbol2occurrences = symbol2occurrences.put(symbol, newList)
        }

        for (arg in occ.arguments()) {
            when (arg) {
                is Logical<*>               -> {
                    logical2occurrences[arg.findRoot()].remove(occ)?. let { newList ->
                        this.logical2occurrences = logical2occurrences.put(arg.findRoot(), newList)
                    }
                    // TODO: remove observer?
                }
                else                        -> {
                    value2occurrences[arg!!].remove(occ)?. let { newList ->
                        this.value2occurrences = value2occurrences.put(arg, newList)
                    }
                }
            }
        }

        // TODO: superfluous cast
        (occ as StoreItem).stored = false
        occ.terminate()
    }

    fun allOccurrences(): Sequence<ConstraintOccurrence> {
        return symbol2occurrences.values().flatMap { it }.filter { co -> co.isStored() }.asSequence()
    }

    fun forSymbol(symbol: ConstraintSymbol): Sequence<ConstraintOccurrence> {
        val list = symbol2occurrences[symbol] ?: emptyConsList()
        return list.asSequence().filter { co -> co.isStored() }
    }

    fun forLogical(ptr: Logical<*>): Sequence<ConstraintOccurrence> {
        val list = logical2occurrences[ptr.findRoot()] ?: emptyConsList()
        return list.asSequence().filter { co -> co.isStored() }
    }

    fun forValue(value: Any): Sequence<ConstraintOccurrence> {
        val list = value2occurrences[value] ?: emptyConsList()
        return list.asSequence().filter { co -> co.isStored() }
    }

    override fun lookupOccurrences(
        symbol: ConstraintSymbol,
        logicals: Iterable<Logical<*>>,
        values: Iterable<Any>,
        acceptable: (ConstraintOccurrence) -> Boolean): Sequence<ConstraintOccurrence>
    {
        val occs = if (!logicals.any() && !values.any())
            forSymbol(symbol)
        else
            (logicals.asSequence().flatMap { log -> forLogical(log) } +
                values.asSequence().flatMap { value -> forValue(value) }).
                filter { co -> co.constraint().symbol() == symbol }

        return occs.filter { acceptable(it) }
    }

}

private data class MemConstraintOccurrence(val proxy: LogicalObserverProxy, val constraint: Constraint, val arguments: List<*>) :
    ConstraintOccurrence,
    LogicalObserver,
    StoreItem
{

    override var alive = true

    override var stored = false

    constructor(proxy: LogicalObserverProxy, constraint: Constraint, arguments: Collection<*>) :
        this(proxy, constraint, ArrayList(arguments))
    {
        for (a in arguments) {
            if (a is Logical<*>) {
                proxy.addObserver(a, this)
            }
        }
    }

    override fun constraint(): Constraint = constraint

    override fun arguments(): List<*> = arguments

    override fun valueUpdated(logical: Logical<*>) {
        Handler.current.queue(this)
    }

    override fun parentUpdated(logical: Logical<*>) {
        Handler.current.queue(this)
    }

    override fun terminate() {
        for (a in arguments) {
            if (a is Logical<*>) {
                proxy.removeObserver(a, this)
            }
        }
        alive = false
    }

    override fun toString(): String = "${constraint().symbol()}(${arguments().joinToString()})"

}
