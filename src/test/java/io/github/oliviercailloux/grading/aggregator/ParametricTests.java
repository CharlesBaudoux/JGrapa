package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ParametricTests {

  @Test
  void testAggregateWithWeightingAndMultipliedOnly() {
    Parametric parametric = givenParametric();

    double actual = parametric.aggregate(TestUtils.givenMarksTree("multiplied", 0.8d, "weighting", 0.25d))  .weightedSum();

    double weightedSum = 0.8d * 0.25d;
    assertEquals(weightedSum, actual, 1e-12);
  }

  @Test
  void testAggregateWithThirdCriterion() {
    Parametric parametric = givenParametric();

    double actual = parametric
        .aggregate(TestUtils.givenMarksTree("multiplied", 0.8d, "weighting", 0.25d, "other", 0.4d)).weightedSum();

    double weightedSum = 0.8d * 0.25d + 0.4d * (1d - 0.25d);
    assertEquals(weightedSum, actual, 1e-12);
  }

  @Test
  void testAggregateMissingMultiplied() {
    Parametric parametric = givenParametric();

    double actual = parametric.aggregate(TestUtils.givenMarksTree("weighting", 0.25d, "other", 0.4d)).weightedSum();

    double weightedSum = 1d * 0.25d + 0.4d * (1d - 0.25d);
    assertEquals(weightedSum, actual, 1e-12);
  }

  @Test
  void testAggregateMissingWeighting() {
    Parametric parametric = givenParametric();

    double actual = parametric.aggregate(TestUtils.givenMarksTree("multiplied", 0.8d, "other", 0.4d)).weightedSum();

    double weightedSum = 0.8d * 1d + 0.4d * 0d;
    assertEquals(weightedSum, actual, 1e-12);
  }

  @Test
  void testAggregateWithSeveralOtherCriteria() {
    Parametric parametric = givenParametric();

    double actual = parametric.aggregate(
        TestUtils.givenMarksTree("multiplied", 0.8d, "weighting", 0.25d, "other", 0.4d, "d", 0.3d)).weightedSum();

    double weightedSum = 0.8d * 0.25d + 0.4d * (1d - 0.25d)/2d + 0.3d * (1d - 0.25d)/2d;
    assertEquals(weightedSum, actual, 1e-12);
  }

  @Test
  void testAggregateWithNegativeMarks() {
    Parametric parametric = givenParametric();

    double actual = parametric
        .aggregate(TestUtils.givenMarksTree("multiplied", -0.8d, "weighting", 0.25d, "other", 0.4d)).weightedSum();

    double weightedSum = -0.8d * 0.25d + 0.4d * (1d - 0.25d);
    assertEquals(weightedSum, actual, 1e-12);
  }

  private static Parametric givenParametric() {
    return Parametric.given(multiplied(), weighting(), ImmutableMap.of(), null);
  }

  private static Criterion multiplied() {
    return c("multiplied");
  }

  private static Criterion weighting() {
    return c("weighting");
  }

  private static Criterion c(String name) {
    return Criterion.given(name);
  }
}
