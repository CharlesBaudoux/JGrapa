package io.github.oliviercailloux.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

public class JsonTests {

  @Test
  void testDeserializeWrongMark() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();
    assertThrows(IllegalArgumentException.class, () -> converter.fromJson(Resourcer.charSource("Wrong mark.json").read()));
  }       

  @Test
  void testDeserializeWrongComposite() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();
    assertThrows(IllegalArgumentException.class, () -> converter.fromJson(Resourcer.charSource("Wrong composite.json").read()));
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
        converter.fromJson(Resourcer.charSource("Composite with mark label.json").read());

    AssessmentTree expected = CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("a"),
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("c"), Assessment.given(0.8),
            Criterion.given("mark"), Assessment.given(0d))),
        Criterion.given("b"), CompositeAssessmentTree
            .given(ImmutableMap.of(Criterion.given("e"), Assessment.given(0.9, "Almost perfect.")))));
    assertEquals(expected, tree);
  }

  @Test
  void testSerializeCompositeWithOnlyMarkChild() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree =
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("mark"), Assessment.given(0d)));

    String expected = """
    {
      "mark" : {
        "mark" : 0.0
      }
    }""";
    assertEquals(expected, converter.toJson(tree));
  }

  @Test
  void testSerializeComposite() throws Exception {
    AssessmentTreeJsonConverter converter = AssessmentTreeJsonConverter.usingDefault();

    AssessmentTree tree =
    CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("a"),
        CompositeAssessmentTree.given(ImmutableMap.of(Criterion.given("c"), Assessment.given(0.8),
            Criterion.given("d"), Assessment.given(0d))),
        Criterion.given("b"), CompositeAssessmentTree
            .given(ImmutableMap.of(Criterion.given("e"), Assessment.given(0.9, "Almost perfect.")))));

    String expected = Resourcer.charSource("Composite.json").read();
    assertEquals(expected, converter.toJson(tree).replaceAll(" :", ":").replaceAll("(?m)0\\.0$", "0") + "\n");
  }
}
