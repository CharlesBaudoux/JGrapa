package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.oliviercailloux.grading.Criterion;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Covers the default sub-aggregator: its optional nature, and the object methods that depend on it.
 */
public class DefaultSubTests {

  private static Criterion c(String name) {
    return Criterion.given(name);
  }

  private static ImmutableList<Aggregator> withoutDefaultSub() {
    return ImmutableList.of(Weighter.given(ImmutableMap.of(c("a"), 0.5d)),
        Owa.given(ImmutableList.of(0.2d, 0.8d)), Parametric.given(c("m"), c("w")));
  }

  @Test
  void testNoDefaultSubGivesEmptyOptional() {
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertEquals(Optional.empty(), aggregator.defaultSub);
      assertEquals(Weighter.FULL_EQUAL_WEIGHTER, aggregator.defaultSub());
    }
  }

  @Test
  void testExplicitDefaultSubIsKept() {
    Weighter defaultSub = Weighter.given(ImmutableMap.of(c("z"), 1d));
    for (Aggregator aggregator : withoutDefaultSub()) {
      Aggregator withDefault = aggregator.withDefaultSub(defaultSub);
      assertEquals(Optional.of(defaultSub), withDefault.defaultSub);
      assertEquals(defaultSub, withDefault.defaultSub());
    }
  }

  @Test
  void testWithSubsKeepsAbsenceOfDefaultSub() {
    ImmutableMap<Criterion, Aggregator> subs =
        ImmutableMap.of(c("s"), Weighter.given(ImmutableMap.of()));
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertEquals(Optional.empty(), aggregator.withSubs(subs).defaultSub);
    }
  }

  @Test
  void testWithDefaultSubRejectsNull() {
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertThrows(NullPointerException.class, () -> aggregator.withDefaultSub(null));
    }
  }

  /** Absent default sub means no sub-aggregator is provided for unlisted criteria. */
  @Test
  void testSubsOfUnknownCriterionIsEmptyWithoutDefaultSub() {
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertEquals(ImmutableMap.of(), aggregator.subs(ImmutableSet.of(c("unknown"))));
    }
  }

  @Test
  void testSubsOfUnknownCriterionUsesDefaultSub() {
    Weighter defaultSub = Weighter.given(ImmutableMap.of(c("z"), 1d));
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertEquals(ImmutableMap.of(c("unknown"), defaultSub),
          aggregator.withDefaultSub(defaultSub).subs(ImmutableSet.of(c("unknown"))));
    }
  }

  /** Guards against the endless recursion caused by hashing the substituted default sub. */
  @Test
  void testHashCodeTerminates() {
    Weighter.FULL_EQUAL_WEIGHTER.hashCode();
    for (Aggregator aggregator : withoutDefaultSub()) {
      aggregator.hashCode();
      aggregator.withDefaultSub(Weighter.FULL_EQUAL_WEIGHTER).hashCode();
    }
  }

  @Test
  void testToStringTerminates() {
    assertTrue(Weighter.FULL_EQUAL_WEIGHTER.toString().contains("Weighter"));
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertTrue(aggregator.toString().contains("defaultSub"));
      assertTrue(aggregator.withDefaultSub(Weighter.FULL_EQUAL_WEIGHTER).toString()
          .contains("defaultSub"));
    }
  }

  /** An aggregator usable as a map key requires equals and hashCode to agree. */
  @Test
  void testEqualsAndHashCodeAgree() {
    for (Aggregator aggregator : withoutDefaultSub()) {
      Aggregator same = aggregator.withSubs(ImmutableMap.of());
      assertEquals(aggregator, same);
      assertEquals(aggregator.hashCode(), same.hashCode());
      assertEquals(aggregator, ImmutableMap.of(aggregator, "v").keySet().iterator().next());
    }
  }

  /** Setting the default sub explicitly is a distinct state from leaving it unset. */
  @Test
  void testExplicitDefaultSubDiffersFromAbsentOne() {
    for (Aggregator aggregator : withoutDefaultSub()) {
      assertNotEquals(aggregator, aggregator.withDefaultSub(Weighter.FULL_EQUAL_WEIGHTER));
    }
  }
}
