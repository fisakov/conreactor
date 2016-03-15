package jetbrains.mps.logic.reactor.program;

/*Generated by MPS */


public class JavaPredicateSymbol extends PredicateSymbol {

  public static final JavaPredicateSymbol EXPRESSION0 = new JavaPredicateSymbol(1);

  public static final JavaPredicateSymbol EXPRESSION1 = new JavaPredicateSymbol(2);

  public static final JavaPredicateSymbol EXPRESSION2 = new JavaPredicateSymbol(3);

  public static final JavaPredicateSymbol EXPRESSION3 = new JavaPredicateSymbol(4);

  private static final String EXPRESSION = "expression";

  private static JavaPredicateSymbol[] KNOWN_SYMBOLS = {EXPRESSION0, EXPRESSION1, EXPRESSION2, EXPRESSION3};

  public static JavaPredicateSymbol withArity(int arity) {
    return KNOWN_SYMBOLS[arity];
  }

  private JavaPredicateSymbol(int arity) {
    super(EXPRESSION, arity);
  }

}
