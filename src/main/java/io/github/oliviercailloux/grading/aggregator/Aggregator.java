package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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

    public Mark mark(Criterion criterion) {
      if (!map.containsKey(criterion)) {
        throw new NoSuchElementException("Criterion not found: " + criterion);
      }
      return map.get(criterion);
    }

    public Optional<Mark> optionalMark(Criterion criterion) {
      return Optional.ofNullable(map.get(criterion));
    }

    public ImmutableSet<Criterion> criteria() {
      return map.keySet();
    }
  }

  private final ImmutableMap<Criterion, Aggregator> subs;
  final Optional<Aggregator> defaultSub;

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
        defaultSub.ifPresent(d -> subAggregators.put(criterion, d));
      }
    }
    return subAggregators.build();
  }

  public static class WeightedMarks {
    public static WeightedMarks given(OneLevelMarksTree marks, Map<Criterion, Double> weights) {
      return new WeightedMarks(marks, weights);
    }

    public static final double TOLERANCE = 1e-9;
    private final OneLevelMarksTree marks;
    private final ImmutableMap<Criterion, Double> weights;

    private WeightedMarks(OneLevelMarksTree marks, Map<Criterion, Double> weights) {
      checkArgument(weights.values().stream().allMatch(w -> Double.isFinite(w)),
          "All weights must be finite.");
      checkArgument(weights.values().stream().allMatch(w -> 0d <= w),
          "All weights must be non-negative.");
      double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
      checkArgument(totalWeight <= 1d + TOLERANCE, "Total weight must be at most one.");
      checkArgument(marks.map().keySet().equals(weights.keySet()),
          "Marks and weights must have the same criteria.");
      this.marks = checkNotNull(marks);
      this.weights = ImmutableMap.copyOf(weights);
    }

    /**
     * @return not empty
     */
    public OneLevelMarksTree marks() {
      return marks;
    }

    /**
     * @return sum to at most one (up to {@link WeightedMarks#TOLERANCE}), non-negative, no NaN
     */
    public ImmutableMap<Criterion, Double> weights() {
      return weights;
    }

    public ImmutableSet<Criterion> criteria() {
      return marks.map().keySet();
    }

    public Mark mark(Criterion criterion) {
      if (!marks.map().containsKey(criterion)) {
        throw new NoSuchElementException("Criterion not found: " + criterion);
      }
      return marks.map().get(criterion);
    }

    public double weight(Criterion criterion) {
      if (!weights.containsKey(criterion)) {
        throw new NoSuchElementException("Criterion not found: " + criterion);
      }
      return weights.get(criterion);
    }

    public double weightedSum() {
      double result = 0d;
      for (Criterion criterion : criteria()) {
        result += mark(criterion).value() * weight(criterion);
      }
      return result;
    }

    @Override
    public boolean equals(Object o2) {
      if (!(o2 instanceof WeightedMarks)) {
        return false;
      }
      final WeightedMarks t2 = (WeightedMarks) o2;
      return this.marks.equals(t2.marks) && this.weights.equals(t2.weights);
    }

    @Override
    public int hashCode() {
      return Objects.hash(marks, weights);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("marks", marks).add("weights", weights)
          .toString();
    }
  }

  public abstract WeightedMarks aggregate(OneLevelMarksTree marks);

  public ImmutableMap<Criterion, Aggregator> subs() {
    return subs;
  }

  public Aggregator defaultSub() {
    return defaultSub.orElse(Weighter.FULL_EQUAL_WEIGHTER);
  }

  public abstract Aggregator withSubs(Map<Criterion, Aggregator> newSubs);

  public abstract Aggregator withDefaultSub(Aggregator newDefaultSub);
}
