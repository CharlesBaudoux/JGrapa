package io.github.oliviercailloux.grading.aggregator;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.aggregator.Aggregator.OneLevelMarksTree;
import io.github.oliviercailloux.grading.assessment.Mark;
import java.util.Map;

public class TestUtils {

  public static OneLevelMarksTree givenMarksTree(String c1, double m1, String c2, double m2) {
    return givenMarksTree(ImmutableMap.of(c1, m1, c2, m2));
  }

  public static OneLevelMarksTree givenMarksTree(String c1, double m1, String c2, double m2, String c3, double m3) {
    return givenMarksTree(ImmutableMap.of(c1, m1, c2, m2, c3, m3));
  }

  public static OneLevelMarksTree givenMarksTree(String c1, double m1, String c2, double m2, String c3, double m3, String c4, double m4) {
    return givenMarksTree(ImmutableMap.of(c1, m1, c2, m2, c3, m3, c4, m4));
  }

  public  static OneLevelMarksTree givenMarksTree(String c1, double m1, String c2, double m2,
      String c3, double m3, String c4, double m4, String c5, double m5, String c6, double m6) {
    return givenMarksTree(ImmutableMap.of(c1, m1, c2, m2, c3, m3, c4, m4, c5, m5, c6, m6));
  }

  public  static OneLevelMarksTree givenMarksTree(Map<String, Double> marks) {
    ImmutableMap<Criterion, Mark> map = marks.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(e -> Criterion.given(e.getKey()), e -> Mark.given(e.getValue())));
    return OneLevelMarksTree.given(map);
  }
  
}
