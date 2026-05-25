package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.math.DoubleMath;
import io.github.oliviercailloux.grading.Criterion;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Owa extends Aggregator {

  private final ImmutableList<Double> weights;

  public static Owa of(List<Double> weights, Map<Criterion, Aggregator> subs,
      Optional<Aggregator> defaultSub) {
    return new Owa(weights, subs, defaultSub);
  }

  private Owa(List<Double> weights, Map<Criterion, Aggregator> subs,
      Optional<Aggregator> defaultSub) {
    super(subs, defaultSub);
    this.weights = ImmutableList.copyOf(weights);
    checkArgument(2 <= ImmutableSet.copyOf(this.weights).size(),
        "OWA weights must contain at least two different values.");
    for (Double weight : weights) {
      checkArgument(0d <= weight, "OWA weights must be non-negative.");
    }
  }

  @Override
  public double aggregate(Map<Criterion, Double> marks) {
    return aggregate(ImmutableMultiset.copyOf(marks.values()));
  }

  /**
   * @param marks non-empty
   */
  public double aggregate(Multiset<Double> marks) {
    checkArgument(!marks.isEmpty(), "Marks must be non-empty.");

    final ImmutableList<Double> effectiveWeights;
    if (weights.size() <= marks.size()) {
      double repeatedWeight =
          weights.stream().min(Double::compareTo).orElseThrow(VerifyException::new);
      int nbOccurrenceOfRepeatedWeight =
          (int) weights.stream().filter(weight -> weight.equals(repeatedWeight)).count();
      int nbToDuplicate = marks.size() - weights.size();
      int roundedNbToDuplicatePerOccurrence = DoubleMath
          .roundToInt((double) nbToDuplicate / nbOccurrenceOfRepeatedWeight, RoundingMode.DOWN);
      int remainder =
          nbToDuplicate - roundedNbToDuplicatePerOccurrence * nbOccurrenceOfRepeatedWeight;
      ImmutableList.Builder<Double> effectiveWeightsBuilder = ImmutableList.builder();
      for (Double weight : weights) {
        final int nbToInsert;
        if (weight.equals(repeatedWeight)) {
          nbToInsert = roundedNbToDuplicatePerOccurrence + (remainder > 0 ? 1 : 0);
          --remainder;
        } else {
          nbToInsert = 1;
        }
        for (int duplicateIndex = 0; duplicateIndex < nbToInsert; duplicateIndex++) {
          effectiveWeightsBuilder.add(weight / nbToDuplicate);
        }
      }
      effectiveWeights = effectiveWeightsBuilder.build();
    } else {
      double smallestAcceptedWeight = weights.stream().sorted(Comparator.reverseOrder())
          .limit(marks.size()).min(Double::compareTo).orElseThrow(VerifyException::new);
      effectiveWeights = weights.stream().sorted().filter(w -> smallestAcceptedWeight <= w)
          .limit(marks.size()).collect(ImmutableList.toImmutableList());
    }
    verify(effectiveWeights.size() == marks.size(),
        "Effective weights size must equal the number of marks.");
    double totalWeight = effectiveWeights.stream().mapToDouble(Double::doubleValue).sum();
    verify(0d < totalWeight, "Total weight must be positive.");

    List<Double> orderedMarks = new ArrayList<>(marks);
    orderedMarks.sort(Comparator.reverseOrder());

    double result = 0d;
    for (int index = 0; index < orderedMarks.size(); ++index) {
      result += orderedMarks.get(index) * effectiveWeights.get(index) / totalWeight;
    }
    if (marks.stream().anyMatch(d -> Double.isNaN(d))) {
      verify(Double.isNaN(result), "Result must be NaN if any mark is NaN.");
    }
    return result;
  }

  public ImmutableList<Double> weights() {
    return weights;
  }
}
