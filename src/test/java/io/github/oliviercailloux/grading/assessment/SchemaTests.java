package io.github.oliviercailloux.grading.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.github.oliviercailloux.grading.Resourcer;
import io.github.oliviercailloux.grading.assessment.AssessmentTreeJsonConverter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;

public class SchemaTests {

  @BeforeAll
  static void setLocale() {
    Locale.setDefault(Locale.ENGLISH);
  }

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaTests.class);

  @Test
  void testWrongSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    Schema draftSchema =
        schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String input = "{\r\n" + "  \"type\": \"object\",\r\n" + "  \"properties\": {\r\n"
        + "    \"key\": {\r\n" + "      \"title\" : \"My key\",\r\n"
        + "      \"type\": \"invalidtype\"\r\n" + "    }\r\n" + "  }\r\n" + "}";
    List<Error> errors = draftSchema.validate(input, InputFormat.JSON);
    assertEquals(2, errors.size());
    Error e0 = errors.get(0);
    assertTrue(e0.getMessage().contains("enumeration"), e0.getMessage());
    Error e1 = errors.get(1);
    assertTrue(e1.getMessage().contains("array expected"), e1.getMessage());
  }

  @Test
  void testGoodSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    Schema draftSchema =
        schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String mySchema = Resourcer.charSource("schemas/assessment-tree.schema.json").read();
    assertEquals(ImmutableList.of(), draftSchema.validate(mySchema, InputFormat.JSON));
  }

  @Test
  void testMark() throws Exception {
    Schema assessmentTreeSchema = AssessmentTreeJsonConverter.assessmentTreeSchema();
    List<Error> errors =
        assessmentTreeSchema.validate(Resourcer.charSource("Assessment/Mark.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testWrongMark() throws Exception {
    Schema assessmentTreeSchema = AssessmentTreeJsonConverter.assessmentTreeSchema();
    List<Error> errors = assessmentTreeSchema
        .validate(Resourcer.charSource("Assessment/Wrong mark.json").read(), InputFormat.JSON);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testComposite() throws Exception {
    Schema assessmentTreeSchema = AssessmentTreeJsonConverter.assessmentTreeSchema();
    List<Error> errors = assessmentTreeSchema
        .validate(Resourcer.charSource("Assessment/Composite.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testCompositeWithMarkLabel() throws Exception {
    Schema assessmentTreeSchema = AssessmentTreeJsonConverter.assessmentTreeSchema();
    List<Error> errors = assessmentTreeSchema
        .validate(Resourcer.charSource("Assessment/Composite with mark label.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testWrongComposite() throws Exception {
    Schema assessmentTreeSchema = AssessmentTreeJsonConverter.assessmentTreeSchema();
    List<Error> errors = assessmentTreeSchema
        .validate(Resourcer.charSource("Assessment/Wrong composite.json").read(), InputFormat.JSON);
    assertFalse(errors.isEmpty());
  }
}
