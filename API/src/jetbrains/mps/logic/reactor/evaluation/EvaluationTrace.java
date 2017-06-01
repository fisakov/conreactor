package jetbrains.mps.logic.reactor.evaluation;

/*Generated by MPS */


/**
 * An interface to be implemented by clients wishing to be notified of the events during evaluation.
 */
public interface EvaluationTrace {

  void activate(ConstraintOccurrence occurrence);

  void reactivate(ConstraintOccurrence occurrence);

  void suspend(ConstraintOccurrence occurrence);

  void discard(ConstraintOccurrence occurrence);

  void trying(MatchRule matchRule);

  void reject(MatchRule matchRule);

  void trigger(MatchRule matchRule);

  void retry(MatchRule matchRule);

  void finish(MatchRule matchRule);

  void tell(PredicateInvocation invocation);

  void ask(boolean result, PredicateInvocation invocation);

  void failure(EvaluationFailureException fail);

  @Deprecated
  void reportFailure(String message);

  EvaluationTrace NULL = new EvaluationTrace() {

    public void activate(ConstraintOccurrence occurrence) {
    }
    public void reactivate(ConstraintOccurrence occurrence) {
    }
    public void suspend(ConstraintOccurrence occurrence) {
    }
    public void discard(ConstraintOccurrence occurrence) {
    }
    public void trying(MatchRule matchRule) {
    }
    public void reject(MatchRule matchRule) {
    }
    public void trigger(MatchRule matchRule) {
    }
    public void retry(MatchRule matchRule) {
    }
    public void finish(MatchRule matchRule) {
    }
    public void tell(PredicateInvocation invocation) {
    }
    public void ask(boolean result, PredicateInvocation invocation) {
    }
    public void failure(EvaluationFailureException fail) {
    }
    public void reportFailure(String message) {
    }

  };
}
