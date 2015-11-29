package jetbrains.mps.logic.reactor.rule;

/*Generated by MPS */

import jetbrains.mps.logic.reactor.constraint.AndItem;
import jetbrains.mps.logic.reactor.constraint.Predicate;
import jetbrains.mps.logic.reactor.constraint.Constraint;
import java.util.Collections;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.ArrayList;

public class RuleBuilder {

  public RuleBuilder(String tag) {
    this.tag = tag;
  }

  public RuleBuilder appendBody(AndItem... item) {
    for (int i = 0; i < item.length; i++) {
      body.add(item[i]);
    }
    return this;
  }

  public RuleBuilder appendGuard(Predicate... pred) {
    for (int i = 0; i < pred.length; i++) {
      guard.add(pred[i]);
    }
    return this;
  }

  public RuleBuilder appendHeadReplaced(Constraint... cst) {
    for (int i = 0; i < cst.length; i++) {
      headReplaced.add(cst[i]);
    }
    return this;
  }

  public RuleBuilder appendHeadKept(Constraint... cst) {
    for (int i = 0; i < cst.length; i++) {
      headKept.add(cst[i]);
    }
    return this;
  }

  public boolean hasHead() {
    return !((headKept.isEmpty() && headReplaced.isEmpty()));
  }

  public boolean hasGuard() {
    return !(guard.isEmpty());
  }

  public boolean hasBody() {
    return !(body.isEmpty());
  }

  public RuleBuilder merge(RuleBuilder... other) {
    doMerge(other);
    return this;
  }

  public jetbrains.mps.logic.reactor.rule.Rule toRule() throws InvalidRuleException {
    jetbrains.mps.logic.reactor.rule.Rule.Kind kind;
    if (!(headKept.isEmpty()) && !(headReplaced.isEmpty())) {
      kind = jetbrains.mps.logic.reactor.rule.Rule.Kind.SIMPAGATION;

    } else if (!(headReplaced.isEmpty())) {
      kind = jetbrains.mps.logic.reactor.rule.Rule.Kind.SIMPLIFICATION;

    } else if (!(headKept.isEmpty())) {
      kind = jetbrains.mps.logic.reactor.rule.Rule.Kind.PROPAGATION;

    } else {
      throw new InvalidRuleException("Invalid rule: empty head in " + toString());
    }

    if (body.isEmpty()) {
      throw new InvalidRuleException("Invalid rule: empty body in " + toString());
    }

    headKept = Collections.unmodifiableList(headKept);
    headReplaced = Collections.unmodifiableList(headReplaced);
    guard = Collections.unmodifiableList(guard);
    body = Collections.unmodifiableList(body);

    return new RuleBuilder.Rule(kind, tag);
  }

  @Override
  public String toString() {
    return String.format("%s (%d,%d,%d,%d)", tag, headKept.size(), headReplaced.size(), guard.size(), body.size());
  }

  private void doMerge(RuleBuilder... other) {
    for (int i = 0; i < other.length; i++) {
      RuleBuilder toMerge = other[i];

      headReplaced.addAll(toMerge.headReplaced);
      headKept.addAll(toMerge.headKept);
      guard.addAll(toMerge.guard);
      body.addAll(toMerge.body);
    }
  }

  public class Rule extends jetbrains.mps.logic.reactor.rule.Rule {

    private Rule(jetbrains.mps.logic.reactor.rule.Rule.Kind kind, String tag) {
      this.kind = kind;
      this.tag = tag;
    }

    public jetbrains.mps.logic.reactor.rule.Rule.Kind kind() {
      return kind;
    }

    @Override
    public String tag() {
      return tag;
    }

    @Override
    public Iterable<Constraint> headKept() {
      return headKept;
    }

    @Override
    public Iterable<Constraint> headReplaced() {
      return headReplaced;
    }

    @Override
    public Iterable<Predicate> guard() {
      return guard;
    }

    @Override
    public Iterable<AndItem> body() {
      return body;
    }

    @Override
    public Iterable<AndItem> all() {
      return Iterables.concat(headKept, headReplaced, guard, body);
    }

    private jetbrains.mps.logic.reactor.rule.Rule.Kind kind;
    private String tag;
  }

  private String tag;
  private List<Constraint> headKept = new ArrayList<Constraint>(4);
  private List<Constraint> headReplaced = new ArrayList<Constraint>(4);
  private List<Predicate> guard = new ArrayList<Predicate>(4);
  private List<AndItem> body = new ArrayList<AndItem>(4);
}
