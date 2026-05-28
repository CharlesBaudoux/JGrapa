package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public sealed abstract class Aggregator permits Parametric, Weighter, Owa {
  public static class WeightedMarks {
    private static final double TOLERANCE = 1e-9;
    private final ImmutableMap<Criterion, Mark> marks;
    private final ImmutableMap<Criterion, Double> weights;

    private WeightedMarks(Map<Criterion, Mark> marks, Map<Criterion, Double> weights) {
      checkArgument(weights.values().stream().allMatch(w -> Double.isFinite(w)),
      "All weights must be finite.");
      checkArgument(weights.values().stream().allMatch(w -> 0d <= w),
      "All weights must be non-negative.");
      double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
      checkArgument(DoubleMath.fuzzyEquals(totalWeight, 1d, TOLERANCE), "Total weight must be one.");
      checkArgument(marks.keySet().equals(weights.keySet()), "Marks and weights must have the same criteria.");
      this.marks = ImmutableMap.copyOf(marks);
      this.weights = ImmutableMap.copyOf(weights);
    }

    /**
     * @return not empty
     */
    public ImmutableMap<Criterion, Mark> marks() {
      return marks;
    }
    
    /**
     * @return sum to one, non-negative, no NaN
     */
    public ImmutableMap<Criterion, Double> weights() {
      return weights;
    }

    public ImmutableSet<Criterion> criteria() {
      return marks.keySet();
    }
    
    public Mark mark(Criterion criterion) {
      if(!marks.containsKey(criterion)) {
        throw new NoSuchElementException("Criterion not found: " + criterion);
      }
      return marks.get(criterion);
    }

    public double weight(Criterion criterion) {
      if(!weights.containsKey(criterion)) {
        throw new NoSuchElementException("Criterion not found: " + criterion);
      }
      return weights.get(criterion);
    }

    public double weightedAverage() {
      double result = 0d;
      for (Criterion criterion : criteria()) {
        result += mark(criterion).value() * weight(criterion);
      }
      return result;
    }
  }

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
