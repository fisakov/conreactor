package jetbrains.mps.logic.reactor.constraint.occurrence;

/*Generated by MPS */

import jetbrains.mps.logic.reactor.constraint.ConstraintSymbol;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

public abstract class AbstractConstraintOccurrence {

  protected AbstractConstraintOccurrence(ConstraintSymbol symbol, Object... arg) {
    this.symbol = symbol;
    this.arg = arg;
  }

  public ConstraintSymbol symbol() {
    return symbol;
  }

  public List<Object> arguments() {
    return (arg != null ? Collections.unmodifiableList(Arrays.asList(arg)) : Collections.emptyList());
  }

  public abstract ConstraintKind kind();

  @Override
  public String toString() {
    return getClass().getSimpleName() + " (" + symbol + ")" + "/" + arg.length;
  }

  private final ConstraintSymbol symbol;
  private Object[] arg;
}
