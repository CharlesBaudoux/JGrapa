package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Weighter extends Aggregator {

  private final ImmutableMap<Criterion, Double> weights;

  public static Weighter given(Map<Criterion, Double> weights) {
    return new Weighter(weights, Map.of(), Optional.empty());
  }

  public static Weighter given(Map<Criterion, Double> weights, Map<Criterion, Aggregator> subs,
      Optional<Aggregator> defaultSub) {
    return new Weighter(weights, subs, defaultSub);
  }

  private Weighter(Map<Criterion, Double> weights, Map<Criterion, Aggregator> subs,
      Optional<Aggregator> defaultSub) {
    super(subs, defaultSub);
    this.weights = ImmutableMap.copyOf(weights);
    weights.values().forEach(w -> checkArgument(w >= 0d, "Weights must be non-negative."));
  }

  @Override
  public double aggregate(OneLevelMarksTree marks) {
    ImmutableSet<Criterion> missingInWeights =
        Sets.difference(marks.map().keySet(), weights.keySet()).immutableCopy();

    double explicitWeightSum = 0d;
    for (Map.Entry<Criterion, Double> entry : weights.entrySet()) {
      if (marks.map().containsKey(entry.getKey())) {
        explicitWeightSum += entry.getValue();
      }
    }

    double missingShare = 0d;
    double complement = Math.max(0d, 1d - explicitWeightSum);
    if (!missingInWeights.isEmpty()) {
      missingShare = complement / missingInWeights.size();
    }

    final double totalWeight =
        Sets.intersection(marks.map().keySet(), weights.keySet()).stream().mapToDouble(weights::get).sum()
            + complement;
    if (totalWeight == 0d) {
      return 0d;
    }

    double result = 0d;
    for (Map.Entry<Criterion, Mark> entry : marks.map().entrySet()) {
      double weight = weights.getOrDefault(entry.getKey(), missingShare) / totalWeight;
      result += entry.getValue().value() * weight;
    }
    return result;
  }

  public ImmutableMap<Criterion, Double> weights() {
    return weights;
  }

  @Override
  public Weighter withSubs(Map<Criterion, Aggregator> newSubs) {
    return new Weighter(weights, newSubs, defaultSub());
  }

  @Override
  public Weighter withDefaultSub(Aggregator newDefaultSub) {
    return new Weighter(weights, subs(), Optional.of(newDefaultSub));
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Weighter)) {
      return false;
    }
    final Weighter t2 = (Weighter) o2;
    return weights.equals(t2.weights) && subs().equals(t2.subs())
        && defaultSub().equals(t2.defaultSub());
  }

  @Override
  public int hashCode() {
    return Objects.hash(weights, subs(), defaultSub());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("weights", weights).add("subs", subs())
        .add("defaultSub", defaultSub()).toString();
  }
}
