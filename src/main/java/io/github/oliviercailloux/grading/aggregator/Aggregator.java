package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public sealed abstract class Aggregator permits Parametric, Weighter, Owa {
  public static record OneLevelMarksTree (/**
                                           * non-empty
                                           */
  ImmutableMap<Criterion, Mark> map) {
    public static OneLevelMarksTree given(Map<Criterion, Mark> marks) {
      return new OneLevelMarksTree(ImmutableMap.copyOf(marks));
    }

    public OneLevelMarksTree {
      checkArgument(!map.isEmpty(), "marks cannot be empty");
    }
  }

  private final ImmutableMap<Criterion, Aggregator> subs;
  private final Optional<Aggregator> defaultSub;

  protected Aggregator(Map<Criterion, Aggregator> subs, Optional<Aggregator> defaultSub) {
    this.subs = ImmutableMap.copyOf(subs);
    this.defaultSub = checkNotNull(defaultSub);
  }

  public ImmutableMap<Criterion, Aggregator> subs(Set<Criterion> criteria) {
    checkNotNull(criteria);
    ImmutableMap.Builder<Criterion, Aggregator> subAggregators = ImmutableMap.builder();
    for (Criterion criterion : criteria) {
      if (subs.containsKey(criterion)) {
        subAggregators.put(criterion, subs.get(criterion));
      } else {
        defaultSub.ifPresent(agg -> subAggregators.put(criterion, agg));
      }
    }
    return subAggregators.build();
  }

  public abstract double aggregate(OneLevelMarksTree marks);

  public ImmutableMap<Criterion, Aggregator> subs() {
    return subs;
  }

  public Optional<Aggregator> defaultSub() {
    return defaultSub;
  }

  public abstract Aggregator withSubs(Map<Criterion, Aggregator> newSubs);

  public abstract Aggregator withDefaultSub(Aggregator newDefaultSub);
}
