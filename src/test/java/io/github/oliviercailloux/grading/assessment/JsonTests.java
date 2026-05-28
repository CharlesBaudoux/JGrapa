package io.github.oliviercailloux.grading.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.Resourcer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

public class JsonTests {

  @Test
  void testDeserializeWrongMark() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();
    assertThrows(IllegalArgumentException.class, () -> converter.fromJson(Resourcer.charSource("Assessment/Wrong mark.json").read()));
  }       

  @Test
  void testDeserializeWrongComposite() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();
    assertThrows(IllegalArgumentException.class, () -> converter.fromJson(Resourcer.charSource("Assessment/Wrong composite.json").read()));
  }       

  @Test
  void testDeserializeCompositeWithOnlyMarkChild() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree = converter.fromJson("""
        {"mark": {"mark": 0}}
        """);

    AssessmentTree expected =
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("mark"), Assessment.given(0d)));
    assertEquals(expected, tree);
  }

  @Test
  void testDeserializeCompositeWithMarkLabel() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree =
        converter.fromJson(Resourcer.charSource("Assessment/Composite with mark label.json").read());

    AssessmentTree expected = CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("a"),
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("c"), Assessment.given(0.8),
            Criterion.given("mark"), Assessment.given(0d))),
        Criterion.given("b"), CompositeAssessmentTree
            .given(ImmutableMap.of(Criterion.given("e"), Assessment.given(0.9, "Almost perfect.")))));
    assertEquals(expected, tree);
  }

  @Test
  void testSerializeCompositeWithOnlyMarkChild() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.using(mapper);

    AssessmentTree tree =
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("mark"), Assessment.given(0d)));

    String expected = """
    {
      "mark" : {
        "mark" : 0
      }
    }""";
    assertEquals(mapper.readTree(expected), converter.toJson(tree));
  }

  @Test
  void testSerializeComposite() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.using(mapper);

    AssessmentTree tree =
    CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("a"),
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("c"), Assessment.given(0.8),
            Criterion.given("d"), Assessment.given(0d))),
        Criterion.given("b"), CompositeAssessmentTree
            .given(ImmutableMap.of(Criterion.given("e"), Assessment.given(0.9, "Almost perfect.")))));

    String expected = Resourcer.charSource("Assessment/Composite.json").read();
    assertEquals(mapper.readTree(expected), converter.toJson(tree));
  }
}
