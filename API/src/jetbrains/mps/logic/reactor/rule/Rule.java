package jetbrains.mps.logic.reactor.rule;

/*Generated by MPS */

import jetbrains.mps.logic.reactor.constraint.Constraint;
import jetbrains.mps.logic.reactor.constraint.Predicate;
import jetbrains.mps.logic.reactor.constraint.AndItem;

public abstract class Rule {

  public abstract Rule.Kind kind();

  public abstract String tag();

  public abstract Iterable<Constraint> headKept();

  public abstract Iterable<Constraint> headReplaced();

  public abstract Iterable<Predicate> guard();

  public abstract Iterable<AndItem> body();

  public abstract Iterable<AndItem> all();

  public static   enum Kind {
    SIMPLIFICATION(),
    PROPAGATION(),
    SIMPAGATION();

  }

}
