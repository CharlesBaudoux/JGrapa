package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A parametric aggregator that multiplies one criterion by a weighting criterion and optionally
 * blends other criteria with the complement of the weighting criterion.
 */
public final class Parametric extends Aggregator {

  private final Criterion multiplied;
  private final Criterion weighting;

  public static Parametric given(Criterion multiplied, Criterion weighting) {
    return new Parametric(multiplied, weighting, Map.of(), Optional.empty());
  }

  public static Parametric given(Criterion multiplied, Criterion weighting,
      Map<Criterion, Aggregator> subs, Optional<Aggregator> defaultSub) {
    return new Parametric(multiplied, weighting, subs, defaultSub);
  }

  private Parametric(Criterion multiplied, Criterion weighting, Map<Criterion, Aggregator> subs,
      Optional<Aggregator> defaultSub) {
    super(subs, defaultSub);
    this.multiplied = checkNotNull(multiplied);
    this.weighting = checkNotNull(weighting);
    checkArgument(!this.multiplied.equals(this.weighting),
        "The multiplied and weighting criteria must differ.");
  }

  @Override
  public double aggregate(OneLevelMarksTree marks) {
    ImmutableMap<Criterion, Double> effectiveWeights;
    {
      double weightingMark = marks.map().getOrDefault(weighting, Mark.max()).value();
      ImmutableMap.Builder<Criterion, Double> weightsBuilder = ImmutableMap.builder();
      ImmutableSet<Criterion> aPrioriCriteria = ImmutableSet.of(weighting, multiplied);
      ImmutableSet<Criterion> otherCriteria =
          Sets.difference(marks.map().keySet(), aPrioriCriteria).immutableCopy();
      weightsBuilder.put(multiplied, weightingMark);
      otherCriteria.forEach(
          criterion -> weightsBuilder.put(criterion, (1d - weightingMark) / otherCriteria.size()));
      effectiveWeights = weightsBuilder.build();
    }
    final ImmutableMap<Criterion, Mark> effectiveMarks;
    {
      ImmutableMap.Builder<Criterion, Mark> builder = ImmutableMap.<Criterion, Mark>builder();
      marks.map().keySet().stream().filter(criterion -> !criterion.equals(weighting))
          .forEach(criterion -> builder.put(criterion, marks.map().get(criterion)));
      if (!marks.map().containsKey(multiplied)) {
        builder.put(multiplied, Mark.max());
      }
      effectiveMarks = builder.build();
    }
    verify(effectiveMarks.keySet().equals(effectiveWeights.keySet()),
        "The effective marks and weights must be defined on the same criteria.");
    return Aggregator.WeightedMarks.given(OneLevelMarksTree.given(effectiveMarks), effectiveWeights)
        .weightedSum();
  }

  public Criterion multiplied() {
    return multiplied;
  }

  public Criterion weighting() {
    return weighting;
  }

  @Override
  public Parametric withSubs(Map<Criterion, Aggregator> newSubs) {
    return new Parametric(multiplied, weighting, newSubs, defaultSub());
  }

  @Override
  public Parametric withDefaultSub(Aggregator newDefaultSub) {
    return new Parametric(multiplied, weighting, subs(), Optional.of(newDefaultSub));
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Parametric)) {
      return false;
    }
    final Parametric t2 = (Parametric) o2;
    return multiplied.equals(t2.multiplied) && weighting.equals(t2.weighting)
        && subs().equals(t2.subs()) && defaultSub().equals(t2.defaultSub());
  }

  @Override
  public int hashCode() {
    return Objects.hash(multiplied, weighting, subs(), defaultSub());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("multiplied", multiplied)
        .add("weighting", weighting).add("subs", subs()).add("defaultSub", defaultSub()).toString();
  }
}
