package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.oliviercailloux.grading.Criterion;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Weighter extends Aggregator {

  private final ImmutableMap<Criterion, Double> weights;

  public static Weighter of(Map<Criterion, Double> weights, Map<Criterion, Aggregator> subs,
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
  public double aggregate(Map<Criterion, Double> marks) {
    checkNotNull(marks);
    checkArgument(!marks.isEmpty(), "Marks must be non-empty.");

    ImmutableSet<Criterion> missingInWeights =
        Sets.difference(marks.keySet(), weights.keySet()).immutableCopy();
    ImmutableSet<Criterion> missingInMarks =
        Sets.difference(weights.keySet(), marks.keySet()).immutableCopy();
    checkArgument(missingInWeights.isEmpty() || missingInMarks.isEmpty(),
        "Weights and marks disagree on criteria: weights=%s, marks=%s.", weights.keySet(),
        marks.keySet());

    double explicitWeightSum = 0d;
    for (Map.Entry<Criterion, Double> entry : weights.entrySet()) {
      if (marks.containsKey(entry.getKey())) {
        explicitWeightSum += entry.getValue();
      }
    }

    double missingShare = 0d;
    if (!missingInWeights.isEmpty()) {
      missingShare = Math.max(0d, 1d - explicitWeightSum) / missingInWeights.size();
    }

    double totalWeight = 0d;
    for (Criterion criterion : marks.keySet()) {
      totalWeight += weights.getOrDefault(criterion, missingShare);
    }
    if (totalWeight == 0d) {
      return 0d;
    }

    double result = 0d;
    for (Map.Entry<Criterion, Double> entry : marks.entrySet()) {
      double weight = weights.getOrDefault(entry.getKey(), missingShare);
      result += entry.getValue() * weight / totalWeight;
    }
    return result;
  }

  public ImmutableMap<Criterion, Double> weights() {
    return weights;
  }
}
