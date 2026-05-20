package io.github.oliviercailloux.grading;

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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(MyTests.class);

  @Test
  void testWrongSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    Schema draftSchema = schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String input = "{\r\n" + "  \"type\": \"object\",\r\n" + "  \"properties\": {\r\n"
        + "    \"key\": {\r\n" + "      \"title\" : \"My key\",\r\n"
        + "      \"type\": \"invalidtype\"\r\n" + "    }\r\n" + "  }\r\n" + "}";
    List<Error> errors = draftSchema.validate(input, InputFormat.JSON);
    assertEquals(2, errors.size());
    Error e0 = errors.get(0);
    assertTrue(e0.getMessage().contains("valeur dans l'énumération"), e0.getMessage());
    Error e1 = errors.get(1);
    assertTrue(e1.getMessage().contains("array attendu"), e1.getMessage());
  }

  @Test
  void testGoodSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    Schema draftSchema = schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String markTreeSchema = Resourcer.charSource("schemas/mark-tree.schema.json").read();
    assertEquals(ImmutableList.of(), draftSchema.validate(markTreeSchema, InputFormat.JSON));
  }

  @Test
  void testMark() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json", "classpath:/io/github/oliviercailloux/grading/schemas/mark-tree.schema.json")));
    Schema markTreeSchema = schemaRegistry
        .getSchema(SchemaLocation.of("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json"));

    List<Error> errors = markTreeSchema.validate(Resourcer.charSource("Mark.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testWrongMark() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json", "classpath:/io/github/oliviercailloux/grading/schemas/mark-tree.schema.json")));
    Schema markTreeSchema = schemaRegistry
        .getSchema(SchemaLocation.of("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json"));

    List<Error> errors = markTreeSchema.validate(Resourcer.charSource("Wrong mark.json").read(), InputFormat.JSON);
    assertFalse(errors.isEmpty());
  }

  @Test
  void testComposite() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json", "classpath:/io/github/oliviercailloux/grading/schemas/mark-tree.schema.json")));
    Schema markTreeSchema = schemaRegistry
        .getSchema(SchemaLocation.of("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json"));

    List<Error> errors = markTreeSchema.validate(Resourcer.charSource("Composite.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testCompositeWithMarkLabel() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json", "classpath:/io/github/oliviercailloux/grading/schemas/mark-tree.schema.json")));
    Schema markTreeSchema = schemaRegistry
        .getSchema(SchemaLocation.of("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json"));

    List<Error> errors = markTreeSchema.validate(Resourcer.charSource("Composite with mark label.json").read(), InputFormat.JSON);
    assertEquals(0, errors.size());
  }

  @Test
  void testWrongComposite() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(), builder -> builder
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json", "classpath:/io/github/oliviercailloux/grading/schemas/mark-tree.schema.json")));
    Schema markTreeSchema = schemaRegistry
        .getSchema(SchemaLocation.of("https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/mark-tree.schema.json"));

    List<Error> errors = markTreeSchema.validate(Resourcer.charSource("Wrong composite.json").read(), InputFormat.JSON);
    assertFalse(errors.isEmpty());
  }
}
