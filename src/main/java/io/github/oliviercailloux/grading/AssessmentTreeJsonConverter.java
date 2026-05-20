package io.github.oliviercailloux.grading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class AssessmentTreeJsonConverter {
  public static AssessmentTreeJsonConverter using(ObjectMapper mapper) {
    return new AssessmentTreeJsonConverter(mapper);
  }

  public static AssessmentTreeJsonConverter usingDefault() {
    return new AssessmentTreeJsonConverter(new ObjectMapper());
  }

  private final ObjectMapper mapper;

  private AssessmentTreeJsonConverter(ObjectMapper mapper) {
    this.mapper = checkNotNull(mapper);
  }

  public String toJson(AssessmentTree tree) throws JacksonException {
    checkNotNull(tree);
    JsonNode node = toJsonNode(tree);
    return mapper.writeValueAsString(node);
  }

  public AssessmentTree fromJson(String json) throws JacksonException {
    checkNotNull(json);
    JsonNode node = mapper.readTree(json);
    return fromJsonNode(node);
  }

  private JsonNode toJsonNode(AssessmentTree tree) {
    return switch (checkNotNull(tree)) {
      case Assessment assessment -> {
        ObjectNode node = mapper.createObjectNode();
        node.put("mark", assessment.mark());
        node.put("feedback", assessment.feedback());
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
