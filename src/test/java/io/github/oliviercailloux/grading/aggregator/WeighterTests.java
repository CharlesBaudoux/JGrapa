package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class WeighterTests {

	@Test
	void testAggregateSameCriteria() {
		Weighter weighter = givenWeighter(ImmutableMap.of(c("a"), 0.25d, c("b"), 0.75d));

		double actual = weighter.aggregate(ImmutableMap.of(c("a"), 0.2d, c("b"), 0.8d));

		assertEquals(0.65d, actual, 1e-12);
	}

	@Test
	void testAggregateMissingCriteriaGetsComplementShare() {
		Weighter weighter = givenWeighter(ImmutableMap.of(c("a"), 0.25d, c("b"), 0.25d));

		double actual = weighter.aggregate(
				ImmutableMap.of(c("a"), 0.2d, c("b"), 0.8d, c("c"), 0.6d));

		assertEquals(0.55d, actual, 1e-12);
	}

	@Test
	void testAggregateIgnoresExtraWeightsWhenMarksAreSubset() {
		Weighter weighter = givenWeighter(
				ImmutableMap.of(c("a"), 0.6d, c("b"), 0.4d, c("c"), 0.0d));

		double actual = weighter.aggregate(ImmutableMap.of(c("a"), 0.2d, c("b"), 0.8d));

		assertEquals(0.44d, actual, 1e-12);
	}

	@Test
	void testAggregateRejectsMixedDisagreementOnCriteria() {
		Weighter weighter = givenWeighter(ImmutableMap.of(c("a"), 1.0d, c("b"), 1.0d));

		assertThrows(IllegalArgumentException.class,
				() -> weighter.aggregate(ImmutableMap.of(c("a"), 0.2d, c("c"), 0.8d)));
	}

	@Test
	void testAggregateReturnsZeroWhenTotalWeightIsZero() {
		Weighter weighter = givenWeighter(ImmutableMap.of(c("a"), 0.0d, c("b"), 0.0d));

		double actual = weighter.aggregate(ImmutableMap.of(c("a"), 0.2d, c("b"), 0.8d));

		assertEquals(0.0d, actual, 1e-12);
	}

	@Test
	void testAverage() {
		Weighter weighter = givenWeighter(ImmutableMap.of());

		double actual = weighter.aggregate(ImmutableMap.of(c("a"), 0.3d, c("b"), 0.9d));

		assertEquals(0.6d, actual, 1e-12);
	}

	private static Weighter givenWeighter(Map<Criterion, Double> weights) {
		return Weighter.given(weights, ImmutableMap.of(), Optional.empty());
	}

	private static Criterion c(String name) {
		return Criterion.given(name);
	}
}
