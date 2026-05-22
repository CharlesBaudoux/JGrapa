package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.github.oliviercailloux.grading.Criterion;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.node.ObjectNode;

public class AggregatorJsonConverter {
  static Schema aggregatorSchema() {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(),
        builder -> builder.schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers.mapPrefix(
            "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/aggregator.schema.json",
            "classpath:/io/github/oliviercailloux/grading/schemas/aggregator.schema.json")));
    Schema aggregatorSchema = schemaRegistry.getSchema(SchemaLocation.of(
        "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/aggregator.schema.json"));
    return aggregatorSchema;
  }

  public static AggregatorJsonConverter using(ObjectMapper mapper, Schema aggregatorSchema) {
    return new AggregatorJsonConverter(mapper, aggregatorSchema);
  }

  public static AggregatorJsonConverter usingDefault() {
    return new AggregatorJsonConverter(new ObjectMapper(), aggregatorSchema());
  }

  private final ObjectMapper mapper;
  private final Schema aggregatorSchema;

  private AggregatorJsonConverter(ObjectMapper mapper, Schema aggregatorSchema) {
    this.mapper = checkNotNull(mapper);
    this.aggregatorSchema = checkNotNull(aggregatorSchema);
  }


}
