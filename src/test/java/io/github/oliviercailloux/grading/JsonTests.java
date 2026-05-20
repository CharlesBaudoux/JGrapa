package io.github.oliviercailloux.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class JsonTests {

  @Test
  void testDeserializeCompositeWithMarkLabel() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree = converter.fromJson(Resourcer.charSource("Composite with mark label.json").read());

    AssessmentTree expected = CompositeAssessmentTree.given(Map.of(Criterion.given("a"),
        CompositeAssessmentTree.given(Map.of(Criterion.given("c"), Assessment.fromOptionalFeedback(0.8, null),
            Criterion.given("mark"), Assessment.fromOptionalFeedback(0d, null))),
        Criterion.given("b"), CompositeAssessmentTree.given(Map.of(Criterion.given("e"),
            Assessment.fromOptionalFeedback(0.9, null)))));
    assertEquals(expected, tree);
  }

  @Test
  void testDeserializeCompositeWithOnlyMarkChild() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree = converter.fromJson("{\"mark\": {\"mark\": 0}} ");

    AssessmentTree expected = CompositeAssessmentTree
        .given(Map.of(Criterion.given("mark"), Assessment.fromOptionalFeedback(0d, null)));
    assertEquals(expected, tree);
  }
  
}
