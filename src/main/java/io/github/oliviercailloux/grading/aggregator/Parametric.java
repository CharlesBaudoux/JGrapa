package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.github.oliviercailloux.grading.Criterion;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A parametric aggregator that multiplies one criterion by a weighting criterion and optionally
 * blends a third criterion with the complement of the weighting criterion.
 */
public final class Parametric extends Aggregator {

  private final Criterion multiplied;
  private final Criterion weighting;

  public static Parametric of(Criterion multiplied, Criterion weighting,
      Map<Criterion, Aggregator> subs, Optional<Aggregator> defaultSub) {
    return new Parametric(multiplied, weighting, subs, defaultSub);
  }

  private Parametric(Criterion multiplied, Criterion weighting,
      Map<Criterion, Aggregator> subs, Optional<Aggregator> defaultSub) {
    super(subs, defaultSub);
    this.multiplied = checkNotNull(multiplied);
    this.weighting = checkNotNull(weighting);
    checkArgument(!this.multiplied.equals(this.weighting),
        "The multiplied and weighting criteria must differ.");
  }

  @Override
  public double aggregate(Map<Criterion, Double> marks) {
    checkNotNull(marks);
    ImmutableSet<Criterion> otherCriteria = marks.keySet().stream()
        .filter(criterion -> !criterion.equals(multiplied) && !criterion.equals(weighting))
        .collect(ImmutableSet.toImmutableSet());
        if(2 <= otherCriteria.size()) {
          return Double.NaN;
        }
    double multipliedMark = marks.getOrDefault(multiplied, Double.NaN);
    double weightingMark = marks.getOrDefault(weighting, Double.NaN);
    final double complementWeightingMark;
    if(otherCriteria.isEmpty()) {
      complementWeightingMark = 0d;
    } else {
      complementWeightingMark = marks.get(Iterables.getOnlyElement(otherCriteria)) * (1d - weightingMark);
    }
    double result = multipliedMark * weightingMark + complementWeightingMark;
    return result;
  }

  public Criterion multiplied() {
    return multiplied;
  }

  public Criterion weighting() {
    return weighting;
  }

}
