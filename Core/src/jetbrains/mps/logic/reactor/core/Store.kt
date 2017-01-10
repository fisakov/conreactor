package jetbrains.mps.logic.reactor.core

import com.github.andrewoma.dexx.collection.DerivedKeyHashMap
import com.github.andrewoma.dexx.collection.KeyFunction
import com.github.andrewoma.dexx.collection.Maps
import com.github.andrewoma.dexx.collection.internal.base.AbstractSet
import jetbrains.mps.logic.reactor.evaluation.ConstraintOccurrence
import jetbrains.mps.logic.reactor.evaluation.EvaluationSession
import jetbrains.mps.logic.reactor.evaluation.StoreView
import jetbrains.mps.logic.reactor.logical.Logical
import jetbrains.mps.logic.reactor.logical.LogicalContext
import jetbrains.mps.logic.reactor.program.Constraint
import jetbrains.mps.logic.reactor.program.ConstraintSymbol
import jetbrains.mps.unification.Term
import jetbrains.mps.logic.reactor.util.*
import java.util.*
import com.github.andrewoma.dexx.collection.Map as PersMap
import com.github.andrewoma.dexx.collection.Set as PersSet
import com.github.andrewoma.dexx.collection.Vector as PersVector

/**
 * @author Fedor Isakov
 */

fun ConstraintOccurrence.isStored(): Boolean =
    // TODO: superfluous cast
    (this as StoreItem).stored

fun ConstraintOccurrence.isAlive(): Boolean =
    // TODO: superfluous cast
    (this as StoreItem).alive

interface StoreItem {

    var alive: Boolean

    var stored: Boolean

    fun terminate(): Unit

}

interface StoreKeeper {

    fun store(): Store

    fun addObserver(logical: Logical<*>, obs: (StoreKeeper) -> LogicalObserver)

    fun removeObserver(logical: Logical<*>, obs: (StoreKeeper) -> LogicalObserver)

}

interface OccurrenceIndex {

    fun forSymbol(symbol: ConstraintSymbol): Iterable<ConstraintOccurrence>

    fun forLogical(logical: Logical<*>): Iterable<ConstraintOccurrence>

    fun forTerm(term: Term): Iterable<ConstraintOccurrence>

    fun forTermAndConstraint(term: Term, cst: Constraint): Iterable<ConstraintOccurrence>

    fun forValue(value: Any): Iterable<ConstraintOccurrence>

}

/**
 * TODO: make this class persistent.
 */
class Store : LogicalObserver, OccurrenceIndex {

    val currentFrame: () -> StoreKeeper

    var symbol2occurrences: PersMap<ConstraintSymbol, IdHashSet<ConstraintOccurrence>>

    var logical2occurrences: PersMap<IdWrapper<Logical<*>>, IdHashSet<ConstraintOccurrence>>

    var term2occurrences: TermTrie<ConstraintOccurrence>

    var value2occurrences: PersMap<Any, IdHashSet<ConstraintOccurrence>>

    constructor(copyFrom: Store, currentFrame: () -> StoreKeeper) {
        this.currentFrame = currentFrame
        this.symbol2occurrences = copyFrom.symbol2occurrences
        this.logical2occurrences = copyFrom.logical2occurrences
        this.term2occurrences = copyFrom.term2occurrences
        this.value2occurrences = copyFrom.value2occurrences
    }

    constructor(copyFrom: StoreView, currentFrame: () -> StoreKeeper) {
        this.currentFrame = currentFrame
        this.symbol2occurrences = copyFrom.constraintSymbols()
            .fold(Maps.of()) { map, sym -> map.put(sym, IdHashSet(copyFrom.occurrences(sym))) }

        var l2o = Maps.of<IdWrapper<Logical<*>>, IdHashSet<ConstraintOccurrence>>()
        var t2o = TermTrie<ConstraintOccurrence>()
        var v2o = Maps.of<Any, IdHashSet<ConstraintOccurrence>>()

        copyFrom.allOccurrences().forEach { occ ->
            occ.arguments().forEach { a ->
                when (a) {
                    is Logical<*>   ->  l2o = l2o.put(IdWrapper(a), l2o[IdWrapper(a)]?.add(occ) ?: singletonSet(occ))
                    is Term         ->  t2o = t2o.put(a, occ)
                    is Any          ->  v2o = v2o.put(a, v2o[a]?.add(occ) ?: singletonSet(occ))
                }
            }

        }

        this.logical2occurrences = l2o
        this.term2occurrences = t2o
        this.value2occurrences = v2o
    }

    constructor(currentFrame: () -> StoreKeeper) {
        this.currentFrame = currentFrame
        this.symbol2occurrences = Maps.of()
        this.logical2occurrences = Maps.of()
        this.term2occurrences = TermTrie()
        this.value2occurrences = Maps.of()
    }

    override fun valueUpdated(logical: Logical<*>) {
        logical2occurrences[IdWrapper(logical.findRoot())]?.let { toMerge ->
            val value = logical.findRoot().value()
            when (value) {
                is Term     -> {
                    for (occ in toMerge) {
                        this.term2occurrences = term2occurrences.put(value.withConstraint(occ.constraint()), occ)
                    }
                }
                is Any      -> {
                    var newSet = value2occurrences[value] ?: emptySet()
                    for (occ in toMerge) {
                        newSet = newSet.add(occ)
                    }
                    this.value2occurrences = value2occurrences.put(value, newSet)
                }
                else        -> {
                    // never happens
                    throw NullPointerException()
                }
            }
        }
    }

    override fun parentUpdated(logical: Logical<*>) {
        // TODO: should we care about the order in which occurrences are stored?
        val logicalId = IdWrapper(logical)
        logical2occurrences[logicalId]?.let { toMerge ->
            val rootId = IdWrapper(logical.findRoot())
            var newSet = logical2occurrences[rootId] ?: emptySet()
            for (log in toMerge) {
                newSet = newSet.add(log)
            }
            this.logical2occurrences = logical2occurrences.remove(logicalId).put(rootId, newSet)
            assert(logical2occurrences.containsKey(rootId))
            assert(!logical2occurrences.containsKey(logicalId))
        }
    }

    fun store(occ: ConstraintOccurrence): Unit {
        val symbol = occ.constraint().symbol()

        this.symbol2occurrences = symbol2occurrences.put(symbol,
            symbol2occurrences[symbol]?.add(occ) ?: singletonSet(occ))

        for (arg in occ.arguments()) {
            val value = if (arg is Logical<*> && arg.isBound) arg.findRoot().value() else arg
            when (value) {
                is Logical<*>   -> {
                    // free logical
                    val argId = IdWrapper(value.findRoot())
                    this.logical2occurrences = logical2occurrences.put(argId,
                        logical2occurrences[argId]?.add(occ) ?: singletonSet(occ))
                    currentFrame().addObserver(value) { frame -> frame.store() }
                }
                is Term         -> {
                    this.term2occurrences = term2occurrences.put(value.withConstraint(occ.constraint()), occ)
                }
                is Any          -> {
                    this.value2occurrences = value2occurrences.put(value,
                        value2occurrences[value]?.add(occ) ?: singletonSet(occ))
                }
                else            -> {
                    // never happens
                    throw NullPointerException()
                }
            }
        }

        // TODO: superfluous cast
        (occ as StoreItem).stored = true
    }

    fun discard(occ: ConstraintOccurrence, profiler: Profiler? = null, tag: String? = null): Unit {
        val symbol = occ.constraint().symbol()

        symbol2occurrences[symbol]?.remove(occ)?.let { newList ->
            this.symbol2occurrences = symbol2occurrences.put(symbol, newList)
        }

        for (arg in occ.arguments()) {
            when (arg) {
                is Logical<*>   -> {
                    val argId = IdWrapper(arg.findRoot())
                    logical2occurrences[argId]?.remove(occ)?.let { newList ->
                        this.logical2occurrences = if (newList.isEmpty) {
                            logical2occurrences.remove(argId)

                        } else {
                            logical2occurrences.put(argId, newList)
                        }
                    }
                }
                is Term         -> {
                    // removing occurrences from the index is *very* expensive,
                    // so let's simply use the 'alive' flag to filter out terminated occurrences
//                    this.term2occurrences = term2occurrences.remove(arg, occ)
                }
                is Any          ->  {
                    value2occurrences[arg]?.remove(occ)?. let { newList ->
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
        return symbol2occurrences.values().flatten().filter { co -> co.isStored() }.asSequence()
    }

    fun view(): StoreView = StoreViewImpl(allOccurrences())

    override fun forSymbol(symbol: ConstraintSymbol): Iterable<ConstraintOccurrence> {
        return (symbol2occurrences[symbol] ?: emptySet()).filter { co -> co.isStored() }
    }

    override fun forLogical(logical: Logical<*>): Iterable<ConstraintOccurrence> {
        return if (logical.isBound) {
            val value = logical.findRoot().value()
            when (value) {
                is Term     -> forTerm(value)
                is Any      -> forValue(value)
                else        -> throw NullPointerException()
            }

        } else {
            (logical2occurrences[IdWrapper(logical.findRoot())] ?: emptySet()).filter { co -> co.isStored() }
        }
    }

    override fun forTerm(term: Term): Iterable<ConstraintOccurrence> {
        return term2occurrences.lookupValues(term.withAny()).filter { it.isStored() }
    }

    override fun forTermAndConstraint(term: Term, cst: Constraint): Iterable<ConstraintOccurrence> {
        return term2occurrences.lookupValues(term.withConstraint(cst)).filter { it.isStored() }
    }

    override fun forValue(value: Any): Iterable<ConstraintOccurrence> {
        return (value2occurrences[value] ?: emptySet()).filter { co -> co.isStored() }
    }

    private fun Term.withConstraint(cst: Constraint): Term = Function(CONSTRAINT, listOf(Function(cst.symbol(), emptyList()), this))

    private fun Term.withAny(): Term = Function(CONSTRAINT, listOf(Variable(ANY), this))

    private companion object {

        private val CONSTRAINT = object : Any() {
            override fun toString(): String = "CONSTRAINT"
        }

        private val ANY = object : Any() {
            override fun toString(): String = "ANY"
        }

    }

}

private class StoreViewImpl(occurrences: Sequence<ConstraintOccurrence>) : StoreView {

    val allOccurrences = occurrences.toSet()

    val allSymbols = allOccurrences.map { co -> co.constraint().symbol() }.toSet()

    override fun constraintSymbols(): Iterable<ConstraintSymbol> = allSymbols

    override fun allOccurrences(): Iterable<ConstraintOccurrence> = allOccurrences

    override fun occurrences(symbol: ConstraintSymbol): Iterable<ConstraintOccurrence> =
        allOccurrences.filter { co -> co.constraint().symbol() == symbol }.toSet()

}
