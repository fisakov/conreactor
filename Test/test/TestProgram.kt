import jetbrains.mps.logic.reactor.core.MemEvaluationSession
import jetbrains.mps.logic.reactor.evaluation.EvaluationSession
import jetbrains.mps.logic.reactor.logical.Logical
import solver.MemSessionSolver
import jetbrains.mps.logic.reactor.program.ConstraintSymbol
import jetbrains.mps.logic.reactor.program.JavaPredicateSymbol
import jetbrains.mps.logic.reactor.program.PredicateSymbol
import org.junit.*
import org.junit.Assert.*
import program.MemConstraint
import solver.eq
import solver.is_eq

/**
 * @author Fedor Isakov
 */

class TestProgram {

    companion object {
        @BeforeClass @JvmStatic fun setup() {
            MemEvaluationSession.init();
        }
        @AfterClass @JvmStatic fun teardown() {
            MemEvaluationSession.deinit();
        }
    }

    private fun Builder.session(name: String): EvaluationSession {
        val sessionSolver = MemSessionSolver(env.expressionSolver, env.equalsSolver)
        val programBuilder = MemProgramBuilder(sessionSolver)
        rules.forEach { r -> programBuilder.addRule(r) }
        return EvaluationSession.newSession(programBuilder.program(name, sessionSolver)).
            withPredicates(PredicateSymbol("equals", 2), JavaPredicateSymbol.EXPRESSION0, JavaPredicateSymbol.EXPRESSION1, JavaPredicateSymbol.EXPRESSION2, JavaPredicateSymbol.EXPRESSION3).
            withParam("main", MemConstraint(ConstraintSymbol("main", 0))).start()
    }

    @Test
    fun replace() {
        program(
            rule("main",
                headReplaced(
                    constraint("main")
                ),
                body(
                    constraint("foo")
                )),
            rule("main.foo",
                headKept(
                    constraint("foo")
                ),
                body(
                    constraint("bar")
                )
            )
        ).session("replace").run {
            assertEquals(setOf(ConstraintSymbol("foo", 0), ConstraintSymbol("bar", 0)), constraintSymbols())
        }
    }

    @Test
    fun logicalValue() {
        val (X, Y, Z) = metaLogical<Int>("X", "Y", "Z")
        program(
            rule("main",
                headReplaced(
                    constraint("main")
                ),
                body(
                    statement({ z -> z eq 33 }, Z),
                    constraint("foo", Z)
                )
            ),
            rule("rule1",
                headKept(
                    constraint("foo", X)
                ),
                body(
                    statement({x, y -> y eq (x.get() * 2) }, X, Y),
                    constraint("bar", Y)
                )
            )
        ).session("logicalValue").run {
            assertEquals(setOf(ConstraintSymbol("foo", 1), ConstraintSymbol("bar", 1)), constraintSymbols())
            assertEquals(2, constraintOccurrences().count())
            val yval = constraintOccurrences(ConstraintSymbol("bar", 1)).first().arguments().first()
            assertEquals(66, (yval as Logical<Int>).get())
        }
    }

    @Test
    fun simpleProgram() {
        val (X, Y) = metaLogical<Int>("X", "Y")

        program(
            rule("main",
                headReplaced( constraint("main") ),         body(   statement({ x -> x.set(5) }, X),
                                                                    constraint("val", X) )
            ),
            rule("dec",
                headReplaced( constraint("val", X) ),       guard(  expression({ x -> x.get() > 0 }, X) ),
                                                            body(   constraint("trail", X),
                                                                    statement({ x, y -> y.set(x.get() - 1)}, X, Y),
                                                                    constraint("val", Y) )
            )
        ).session("dec").run {
            assertEquals(setOf(ConstraintSymbol("val", 1), ConstraintSymbol("trail", 1)), constraintSymbols())
            assertEquals(1, constraintOccurrences(ConstraintSymbol.symbol("val", 1)).count())
            val a = constraintOccurrences(ConstraintSymbol.symbol("val", 1)).first().arguments().first()
            assertEquals(0, (a as Logical<Int>).get())
            assertEquals(5, constraintOccurrences(ConstraintSymbol.symbol("trail", 1)).count())
        }
    }

    @Test
    fun correctRulesOrder() {
        val X= metaLogical<Int>("X")

        program(
            rule("main",
                headReplaced( constraint("main") ),         body(   statement({ x -> x.set(1) }, X),
                                                                    constraint("bar"),
                                                                    constraint("foo", X) )
            ),
            rule("foo_if_zero",
                headReplaced( constraint("foo", X) ),       guard(  expression({ x -> x.get() == 0 }, X) ),
                                                            body(   constraint("foo_zero") )
            ),
            rule("foo_and_bar",
                headReplaced( constraint("foo", X) ),
                headKept( constraint("bar") ),
                                                            body(   constraint("foo_and_bar") )
            ),
            rule("foo_if_non_zero",
                headReplaced( constraint("foo", X) ),
                                                            guard(  expression({ x -> x.get() != 0 }, X) ),
                                                            body(   constraint("foo_non_zero") )
            )
        ).session("correctRulesOrder").run {
            assertEquals(setOf(ConstraintSymbol("bar", 0), ConstraintSymbol("foo_and_bar", 0)), constraintSymbols())
            assertEquals(1, constraintOccurrences(ConstraintSymbol("foo_and_bar", 0)).count())
            assertEquals(1, constraintOccurrences(ConstraintSymbol("bar", 0)).count())
        }
    }

    @Test
    fun reactivateOnUnion() {
        val (X,Y) = metaLogical<Int>("X", "Y")
        val S = metaLogical<String>("S")

        program(
            rule("main",
                headReplaced( constraint("main") ),         body(   constraint("foo", X, S),
                                                                    constraint("foo", Y, S),
                                                                    statement({ x, y -> x eq y }, X, Y) )
            ),
            rule("capture_foo",
                headKept( constraint("foo", X, S) ),
                                                            body(   constraint("capture", S) )
            ),
            rule("capture_foo_foo",
                headKept( constraint("foo", X, S) ),
                headReplaced( constraint("foo", Y, S) ),    guard(  expression({x, y -> x is_eq y }, X, Y)),
                                                            body(   constraint("replaced") )
            )
        ).session("reactivateOnUnion").run {
            assertEquals(setOf( ConstraintSymbol("foo", 2),
                                ConstraintSymbol("capture", 1),
                                ConstraintSymbol("replaced", 0)), constraintSymbols())
            assertEquals(1, constraintOccurrences(ConstraintSymbol("foo", 2)).count())
            // FIXME: this count should be 2 instead as per the "activation history" feature
            assertEquals(3, constraintOccurrences(ConstraintSymbol("capture", 1)).count())
            assertEquals(1, constraintOccurrences(ConstraintSymbol("replaced", 0)).count())
        }
    }

    @Test
    fun gcd() {
        val (M, N, TMP) = metaLogical<Int>("M", "N", "TMP")
        program(
            rule("main",
                headReplaced( constraint("main") ),     body(   statement({ m, n -> m.set(21); n.set(35) }, M, N),
                                                                constraint("gcd", M),
                                                                constraint("gcd", N) )
            ),
            rule("trivial",
                headReplaced( constraint("gcd", M) ),   guard(  expression({ x -> x.get() == 0 }, M) ),
                                                        body(   statement {  }  /*nothing*/ )
            ),
            rule("step",
                headKept( constraint("gcd", N) ),
                headReplaced( constraint("gcd", M) ),
                                                        guard(  expression({ m, n -> m.get() >= n.get()}, M, N) ),
                                                        body(   statement({ m, n, tmp -> tmp.set(m.get() - n.get())}, M, N, TMP),
                                                                constraint("gcd", TMP)
                )
            )
        ).session("gcd").run {
            assertEquals(1, constraintOccurrences().count())
            val arg = constraintOccurrences().first().arguments().first()
            assertEquals(7, (arg as Logical<Int>).get())
        }
    }


    @Test
    fun primes() {
        val (M, N) = metaLogical<Int>("M", "N")
        program(
            rule("main",
                headReplaced( constraint("main") ),     body(   statement({ n -> n.set(10) }, N),
                                                                constraint("prime", N)  )
            ),
            rule("gen",
                headKept( constraint("prime", N)),      guard(  expression({ n -> n.get() > 2 }, N)),
                                                        body(   statement({ m, n -> m.set(n.get() - 1) }, M, N),
                                                                constraint("prime", M)  )
            ),
            rule("sift",
                headKept( constraint("prime", M)),
                headReplaced( constraint("prime", N)),
                                                        guard(  expression({ m, n -> (n.get() % m.get()) == 0 }, M, N)),
                                                        body(   statement { } /* nothing */     )
            )
        ).session("primes").run {
            assertEquals(4, constraintOccurrences().count())
            assertEquals(setOf(2,3,5,7),                constraintOccurrences(ConstraintSymbol.symbol("prime", 1)).
                                                            flatMap { co -> co.arguments() }.
                                                            map { a -> (a as Logical<Int>).findRoot().value() }.
                                                            toSet())
        }

    }

}
