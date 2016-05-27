package jetbrains.mps.logic.reactor.program;

/*Generated by MPS */

import java.util.List;
import java.util.Collection;
import jetbrains.mps.logic.reactor.logical.LogicalContext;

/**
 * A constraint provided by a handler. Can only be told.
 */
public interface Constraint extends AndItem {

  public ConstraintSymbol symbol();

  public List<Class<?>> argumentTypes();

  public Collection<?> occurrenceArguments(LogicalContext logicalContext);

  /**
   * Returns the collection of predicates that need to be applied after a successful match of this collection by a
   * rule's head. 
   * This method only returns meaningful results for constraints that serve as patterns in a rule's head. 
   */
  public Collection<? extends Predicate> patternPredicates();

}
