package io.github.oliviercailloux.grading.assessment;

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

public class AssessmentTreeJsonConverter {
  public static Schema assessmentTreeSchema() {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(),
        builder -> builder.schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers.mapPrefix(
            "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/assessment-tree.schema.json",
            "classpath:/io/github/oliviercailloux/grading/schemas/assessment-tree.schema.json")));
    Schema assessmentTreeSchema = schemaRegistry.getSchema(SchemaLocation.of(
        "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/assessment-tree.schema.json"));
    return assessmentTreeSchema;
  }

  public static AssessmentTreeJsonConverter using(ObjectMapper mapper, Schema assessmentTreeSchema) {
    return new AssessmentTreeJsonConverter(mapper, assessmentTreeSchema);
  }

  public static AssessmentTreeJsonConverter usingDefault() {
    return new AssessmentTreeJsonConverter(new ObjectMapper(), assessmentTreeSchema());
  }

  private final ObjectMapper mapper;
  private final Schema assessmentTreeSchema;

  private AssessmentTreeJsonConverter(ObjectMapper mapper, Schema assessmentTreeSchema) {
    this.mapper = checkNotNull(mapper);
    this.assessmentTreeSchema = checkNotNull(assessmentTreeSchema);
  }

  public String toJson(AssessmentTree tree) throws JacksonException {
    ObjectNode node = toJsonNode(tree);
    verify(assessmentTreeSchema.validate(node).isEmpty(),
        "Generated JSON does not conform to schema.");
    return mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValueAsString(node);
  }

  public AssessmentTree fromJson(String json) throws JacksonException {
    JsonNode node = mapper.readTree(checkNotNull(json));
    checkArgument(assessmentTreeSchema.validate(node).isEmpty(),
        "Provided JSON does not conform to schema.");
    return fromJsonNode(node);
  }

  private ObjectNode toJsonNode(AssessmentTree tree) {
    return switch (tree) {
      case Assessment assessment -> {
        ObjectNode node = mapper.createObjectNode();
        node.put("mark", assessment.mark());
        if (!assessment.feedback().isEmpty()) {
          node.put("feedback", assessment.feedback());
        }
        yield node;
      }
      case CompositeAssessmentTree composite -> {
        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<Criterion, AssessmentTree> entry : composite.getChildren().entrySet()) {
          node.set(entry.getKey().getName(), toJsonNode(entry.getValue()));
        }
        yield node;
      }
    };
  }

  private AssessmentTree fromJsonNode(JsonNode node) {
    checkNotNull(node);
    if (!(node instanceof ObjectNode objectNode)) {
      throw new IllegalArgumentException("Assessment tree node must be a JSON object.");
    }
    boolean assessment = objectNode.has("mark") && objectNode.get("mark").isNumber();
    if (assessment) {
      objectNode.propertyNames().stream()
          .filter(propertyName -> !propertyName.equals("mark") && !propertyName.equals("feedback"))
          .findAny().ifPresent(invalidProperty -> {
            throw new IllegalArgumentException("Assessment node must not contain property '"
                + invalidProperty + "' besides mark and feedback.");
          });
      JsonNode markNode = objectNode.get("mark");
      double mark = markNode.asDouble();
      String feedback;
      if (objectNode.has("feedback")) {
        JsonNode feedbackNode = objectNode.get("feedback");
        checkArgument(feedbackNode.isString(), "Assessment feedback must be a string.");
        feedback = feedbackNode.stringValue();
      } else {
        feedback = "";
      }
      return new Assessment(mark, feedback);
    }
    Map<Criterion, AssessmentTree> children = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
      Criterion criterion = Criterion.given(property.getKey());
      AssessmentTree child = fromJsonNode(property.getValue());
      children.put(criterion, child);
    }
    return CompositeAssessmentTree.given(children);
  }
}
