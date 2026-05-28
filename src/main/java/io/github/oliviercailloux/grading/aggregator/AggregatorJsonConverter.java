package io.github.oliviercailloux.grading.aggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.github.oliviercailloux.grading.Criterion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class AggregatorJsonConverter {
  public static Schema aggregatorSchema() {
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

  public String toJson(Aggregator aggregator) throws JacksonException {
    ObjectNode node = toJsonNode(aggregator);
    verify(aggregatorSchema.validate(node).isEmpty(), "Generated JSON does not conform to schema.");
    return mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValueAsString(node);
  }

  public Aggregator fromJson(String json) throws JacksonException {
    JsonNode node = mapper.readTree(checkNotNull(json));
    checkArgument(aggregatorSchema.validate(node).isEmpty(),
        "Provided JSON does not conform to schema.");
    return fromJsonNode(node);
  }

  private ObjectNode toJsonNode(Aggregator aggregator) {
    checkNotNull(aggregator);
    ObjectNode specificNode = switch (aggregator) {
      case Weighter weighter -> {
        ObjectNode node = mapper.createObjectNode();
        if (!weighter.weights().isEmpty()) {
          ObjectNode weightsNode = mapper.createObjectNode();
          for (Map.Entry<Criterion, Double> entry : weighter.weights().entrySet()) {
            weightsNode.put(entry.getKey().name(), entry.getValue());
          }
          node.set("weights", weightsNode);
        }
        yield node;
      }
      case Owa owa -> {
        ObjectNode node = mapper.createObjectNode();
        final ArrayNode weightsNode = node.putArray("weights");
        for (Double weight : owa.weights()) {
          weightsNode.add(weight);
        }
        yield node;
      }
      case Parametric parametric -> {
        ObjectNode node = mapper.createObjectNode();
        node.put("multiplied", parametric.multiplied().name());
        node.put("weighting", parametric.weighting().name());
        yield node;
      }
    };
    addSubsAndDefaultSub(specificNode, aggregator);
    return specificNode;
  }

  private void addSubsAndDefaultSub(ObjectNode node, Aggregator aggregator) {
    if (!aggregator.subs().isEmpty()) {
      ObjectNode subsNode = mapper.createObjectNode();
      for (Map.Entry<Criterion, Aggregator> entry : aggregator.subs().entrySet()) {
        subsNode.set(entry.getKey().name(), toJsonNode(entry.getValue()));
      }
      node.set("subs", subsNode);
    }
    aggregator.defaultSub().ifPresent(defaultSub -> node.set("defaultSub", toJsonNode(defaultSub)));
  }

  private Aggregator fromJsonNode(JsonNode node) {
    checkNotNull(node);
    if (!(node instanceof ObjectNode objectNode)) {
      throw new VerifyException("Aggregator node must be a JSON object.");
    }
    objectNode.propertyNames().stream()
        .filter(property -> !property.equals("weights") && !property.equals("multiplied")
            && !property.equals("weighting") && !property.equals("subs")
            && !property.equals("defaultSub"))
        .findAny().ifPresent(invalid -> {
          throw new VerifyException("Unknown aggregator property '" + invalid + "'.");
        });

    ImmutableMap<Criterion, Aggregator> subs = parseSubs(objectNode.get("subs"));
    Optional<Aggregator> defaultSub = Optional.ofNullable(objectNode.get("defaultSub")).map(this::fromJsonNode);

    boolean hasMultiplied = objectNode.has("multiplied");
    boolean hasWeighting = objectNode.has("weighting");
    if (hasMultiplied || hasWeighting) {
      verify(hasMultiplied && hasWeighting,
          "Parametric aggregator requires both multiplied and weighting.");
      JsonNode multipliedNode = objectNode.get("multiplied");
      JsonNode weightingNode = objectNode.get("weighting");
      verify(multipliedNode.isString(), "Property multiplied must be a string.");
      verify(weightingNode.isString(), "Property weighting must be a string.");
      return Parametric.given(Criterion.given(multipliedNode.stringValue()),
          Criterion.given(weightingNode.stringValue()), subs, defaultSub);
    }

    if (objectNode.has("weights")) {
      JsonNode weightsNode = objectNode.get("weights");
      if (weightsNode.isArray()) {
        List<Double> weights = new ArrayList<>();
        for (JsonNode weightNode : weightsNode) {
          verify(weightNode.isNumber(), "OWA weights entries must be numbers.");
          weights.add(weightNode.doubleValue());
        }
        checkArgument(2 <= weights.stream().distinct().count(), "OWA weights must contain at least two different values.");
        return Owa.given(weights, subs, defaultSub);
      }

      verify(weightsNode.isObject(), "Weighter weights must be an object.");
      return Weighter.given(parseWeighterWeights(weightsNode), subs, defaultSub);
    }

    return Weighter.given(ImmutableMap.of(), subs, defaultSub);
  }

  private ImmutableMap<Criterion, Double> parseWeighterWeights(JsonNode weightsNode) {
    if (!(weightsNode instanceof ObjectNode objectNode)) {
      throw new VerifyException("Weighter weights must be a JSON object.");
    }
    ImmutableMap.Builder<Criterion, Double> weightsBuilder = ImmutableMap.builder();
    for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
      verify(property.getValue().isNumber(), "Weight value for %s must be a number.",
          property.getKey());
      weightsBuilder.put(Criterion.given(property.getKey()), property.getValue().doubleValue());
    }
    return weightsBuilder.build();
  }

  private ImmutableMap<Criterion, Aggregator> parseSubs(JsonNode subsNode) {
    if (subsNode == null) {
      return ImmutableMap.of();
    }
    verify(subsNode instanceof ObjectNode, "Property subs must be an object.");
    ObjectNode objectNode = (ObjectNode) subsNode;
    ImmutableMap.Builder<Criterion, Aggregator> subsBuilder = ImmutableMap.builder();
    for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
      subsBuilder.put(Criterion.given(property.getKey()), fromJsonNode(property.getValue()));
    }
    return subsBuilder.build();
  }
}
