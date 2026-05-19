package io.github.oliviercailloux.grading;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.regex.JoniRegularExpressionFactory;
import com.networknt.schema.Error;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(MyTests.class);

  @Test
  void testSchema() throws Exception {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012());
    /*
     * Due to the mapping the meta-schema for the dialect will be retrieved from the classpath at
     * classpath:draft/2020-12/schema.
     */
    Schema schema = schemaRegistry.getSchema(SchemaLocation.of(Dialects.getDraft202012().getId()));
    String input = "{\r\n" + "  \"type\": \"object\",\r\n" + "  \"properties\": {\r\n"
        + "    \"key\": {\r\n" + "      \"title\" : \"My key\",\r\n"
        + "      \"type\": \"invalidtype\"\r\n" + "    }\r\n" + "  }\r\n" + "}";
    List<Error> errors = schema.validate(input, InputFormat.JSON, executionContext -> {
      /*
       * By default since Draft 2019-09 the format keyword only generates annotations and not
       * assertions.
       */
      executionContext
          .executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true));
    });
  }

  @Test
  void testSomething() throws Exception {
    LOGGER.info("Started tests.");
    /*
     * The SchemaRegistryConfig can be optionally used to configure certain aspects of how the
     * validation is performed.
     *
     * By default the JDK regular expression implementation which is not ECMA 262 compliant is used.
     * The GraalJSRegularExpressionFactory.getInstance() offers the best compliance followed by
     * JoniRegularExpressionFactory.getInstance() but both require additional optional dependencies.
     */
    SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder()
        .regularExpressionFactory(JoniRegularExpressionFactory.getInstance()).build();

    /*
     * This creates a schema registry that supports all the standard dialects for cross-dialect
     * validation and will use Draft 2020-12 as the default if $schema is not specified in the
     * schema data. If $schema is specified in the schema data then that schema dialect will be used
     * instead and this version is ignored.
     */
    SchemaRegistry schemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
            builder -> builder.schemaRegistryConfig(schemaRegistryConfig)
                /*
                 * This creates a mapping from $id which starts with https://www.example.org/schema
                 * to the retrieval IRI classpath:schema.
                 */
                .schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
                    .mapPrefix("https://www.example.com/schema", "classpath:schema")));

    /*
     * Due to the mapping the schema will be retrieved from the classpath at
     * classpath:schema/example-main.json. If the schema data does not specify an $id the absolute
     * IRI of the schema location will be used as the $id. If the schema data does not specify a
     * dialect using $schema the default dialect specified when creating the schema registry.
     */
    Schema schema = schemaRegistry
        .getSchema(SchemaLocation.of("https://www.example.com/schema/example-main.json"));
    String input = "{\r\n" + "  \"main\": {\r\n" + "    \"common\": {\r\n"
        + "      \"field\": \"invalidfield\"\r\n" + "    }\r\n" + "  }\r\n" + "}";

    List<Error> errors = schema.validate(input, InputFormat.JSON, executionContext -> {
      /*
       * By default since Draft 2019-09 the format keyword only generates annotations and not
       * assertions.
       */
      executionContext
          .executionConfig(executionConfig -> executionConfig.formatAssertionsEnabled(true));
    });
  }
}
