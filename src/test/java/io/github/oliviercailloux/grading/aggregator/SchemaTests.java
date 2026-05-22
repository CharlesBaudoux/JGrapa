package io.github.oliviercailloux.grading.aggregator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.ImmutableList;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.github.oliviercailloux.grading.Resourcer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaTests.class);

  @Test
  void testGoodSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    Schema draftSchema =
        schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String mySchema = Resourcer.charSource("schemas/aggregator.schema.json").read();
    assertEquals(ImmutableList.of(), draftSchema.validate(mySchema, InputFormat.JSON));
  }

  @Test
  void testComplex() throws Exception {
    Schema aggregatorSchema = AggregatorJsonConverter.aggregatorSchema();
    List<Error> errors =
        aggregatorSchema.validate(Resourcer.charSource("Aggregator/Complex.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size(), errors.toString());
  }

  @Test
  void testWrong() throws Exception {
    Schema aggregatorSchema = AggregatorJsonConverter.aggregatorSchema();
    List<Error> errors =
        aggregatorSchema.validate(Resourcer.charSource("Aggregator/Wrong.json").read(), InputFormat.JSON);
    assertFalse(errors.isEmpty(), errors.toString());
  }
}
