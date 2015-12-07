package jetbrains.mps.logic.reactor.program;

/*Generated by MPS */

import jetbrains.mps.logic.reactor.constraint.SessionSolver;
import jetbrains.mps.logic.reactor.constraint.ConstraintSymbol;
import java.util.List;
import jetbrains.mps.logic.reactor.constraint.PredicateSymbol;
import jetbrains.mps.logic.reactor.constraint.Queryable;
import jetbrains.mps.logic.reactor.rule.Rule;
import jetbrains.mps.logic.reactor.rule.InvalidRuleException;
import java.util.Collection;

public abstract class PlanningSession {

  public static PlanningSession newSession(String name, SessionSolver sessionSolver) {
    if (ourFactory == null) {
      throw new IllegalStateException("factory not set");
    }
    return ourFactory.create(name, sessionSolver);
  }

  public abstract String name();

  public abstract SessionSolver sessionSolver();

  public abstract Iterable<ConstraintSymbol> constraintSymbols();

  public abstract List<Class<?>> constraintArgumentTypes(ConstraintSymbol constraintSymbol);

  public abstract Iterable<PredicateSymbol> predicateSymbols();

  public abstract Class<? extends Queryable> solverClass(PredicateSymbol predicateSymbol);

  public abstract Iterable<Rule> rules();

  public abstract void addRule(Rule rule) throws InvalidRuleException;

  public abstract void addRules(Collection<? extends Rule> rules) throws InvalidRuleException;

  protected static void setFactory(PlanningSession.Factory factory) {
    if (ourFactory != null) {
      throw new IllegalStateException("factory already set");
    }
    ourFactory = factory;
  }

  protected static void clearFactory(PlanningSession.Factory factory) {
    if (ourFactory != factory) {
      throw new IllegalStateException("illegal access");
    }
    ourFactory = null;
  }

  protected static interface Factory {

    public PlanningSession create(String name, SessionSolver sessionSolver);

  }

  private static PlanningSession.Factory ourFactory;
}
