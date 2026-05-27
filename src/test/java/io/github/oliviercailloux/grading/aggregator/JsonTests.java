package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import io.github.oliviercailloux.grading.Resourcer;
import org.junit.jupiter.api.Test;

public class JsonTests {

  @Test
  void testDeserializeWrongMark() throws Exception {
    AggregatorJsonConverter converter = AggregatorJsonConverter.usingDefault();
    assertThrows(IllegalArgumentException.class,
        () -> converter.fromJson(Resourcer.charSource("Aggregator/Wrong.json").read()));
  }

  @Test
  void testDeserializeSimple() throws Exception {
    AggregatorJsonConverter converter = AggregatorJsonConverter.usingDefault();

    Aggregator tree = converter.fromJson("{}");

    Aggregator expected = Weighter.given(ImmutableMap.of());
    assertEquals(expected, tree);
  }

  @Test
  void testDeserializeComplex() throws Exception {
    AggregatorJsonConverter converter = AggregatorJsonConverter.usingDefault();

    Aggregator tree = converter.fromJson(Resourcer.charSource("Aggregator/Complex.json").read());

    Weighter sub2Default = Weighter.given(ImmutableMap.of());
    Aggregator sub2 = Owa.given(ImmutableList.of(0.2d, 0.8d)).withDefaultSub(sub2Default);
    Aggregator sub1 = Weighter.given(ImmutableMap.of(Criterion.given("a"), 0.8d));
    ImmutableMap<Criterion, Aggregator> subs = ImmutableMap.of(Criterion.given("sub1"), sub1, Criterion.given("sub2"), sub2);
    Aggregator expected = Parametric.given(Criterion.given("x"), Criterion.given("w")).withSubs(subs);
    assertEquals(expected, tree);
  }

  @Test
  void testSerializeEmpty() throws Exception {
    AggregatorJsonConverter converter = AggregatorJsonConverter.usingDefault();

    assertEquals("{ }", converter.toJson(Weighter.given(ImmutableMap.of())));
  }

  @Test
  void testSerializeComplex() throws Exception {
    AggregatorJsonConverter converter = AggregatorJsonConverter.usingDefault();

    Weighter sub2Default = Weighter.given(ImmutableMap.of());
    Aggregator sub2 = Owa.given(ImmutableList.of(0.2d, 0.8d)).withDefaultSub(sub2Default);
    Aggregator sub1 = Weighter.given(ImmutableMap.of(Criterion.given("a"), 0.8d));
    ImmutableMap<Criterion, Aggregator> subs = ImmutableMap.of(Criterion.given("sub1"), sub1, Criterion.given("sub2"), sub2);
    Aggregator tree = Parametric.given(Criterion.given("x"), Criterion.given("w")).withSubs(subs);

    String expected = Resourcer.charSource("Aggregator/Complex.json").read();
    assertEquals(expected.replaceAll("\\[\n +0.2,\n +0.8\n +\\]", "[ 0.2, 0.8 ]"),
        converter.toJson(tree).replaceAll(" :", ":").replaceAll("(?m)0\\.0$", "0").replace("{ }", "{}") + "\n");
  }
}
