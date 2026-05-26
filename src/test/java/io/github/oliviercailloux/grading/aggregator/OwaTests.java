package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OwaTests {

  @Test
  void testAggregateSameNumberOfWeightsAndMarks() {
    Owa owa = givenOwa(ImmutableList.of(0.7d, 0.3d));

    double actual = owa.aggregate(ImmutableMap.of(c("a"), 0.1d, c("b"), 0.9d));

    assertEquals(0.66d, actual, 1e-12);
  }

  @Test
  void testAggregateWithMoreMarksThanWeights() {
    Owa owa = givenOwa(ImmutableList.of(1d, 3d, 1d));

    double actual = owa.aggregate(ImmutableMap.of(c("a"), 0.6d, c("b"), 0.8d, c("c"), 1d,
        c("d"), 0.4d, c("e"), 0.2d, c("f"), 0d));

    double weightedSum =
      1d * (1d / 3d) + 0.8d * (1d / 3d) + 0.6d * (1d / 3d) + 0.4d * 3d + 0.2d * (1d / 2d);
    double totalWeight = 1d + 3d + 1d;
    assertEquals(weightedSum / totalWeight, actual, 1e-12);
  }

  @Test
  void testAggregateWithFewerMarksThanWeights() {
    Owa owa = givenOwa(ImmutableList.of(1d, 3d, 1d));

    double actual = owa.aggregate(ImmutableMap.of(c("a"), 0d, c("b"), 1d));

    double weightedSum = 1d * 1d + 0d * 3d;
    double totalWeight = 1d + 3d;
    assertEquals(weightedSum / totalWeight, actual, 1e-12);
  }

  @Test
  void testMaxBehavior() {
    Owa owa = givenOwa(ImmutableList.of(1d, 0d));

    double actual = owa.aggregate(ImmutableMap.of(c("a"), 0.2d, c("b"), 0.8d));

    assertEquals(0.8d, actual, 1e-12);
  }

  @Test
  void testNaNPropagates() {
    Owa owa = givenOwa(ImmutableList.of(1d, 0d));

    double actual = owa.aggregate(ImmutableMap.of(c("a"), Double.NaN, c("b"), 0.8d));

    assertTrue(Double.isNaN(actual));
  }

  private static Owa givenOwa(List<Double> weights) {
    return Owa.of(weights, ImmutableMap.of(), Optional.empty());
  }

  private static Criterion c(String name) {
    return Criterion.given(name);
  }
}
