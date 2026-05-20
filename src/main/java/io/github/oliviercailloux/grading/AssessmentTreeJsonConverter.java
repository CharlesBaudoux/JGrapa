package io.github.oliviercailloux.grading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
      default -> throw new IllegalStateException(
          "Unhandled assessment tree implementation: " + tree.getClass());
    };
  }

  private AssessmentTree fromJsonNode(JsonNode node) {
    checkNotNull(node);
    if (!(node instanceof ObjectNode objectNode)) {
      throw new IllegalArgumentException("Assessment tree node must be a JSON object.");
    }
    Set<String> keys = new HashSet<>();
    for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
      keys.add(property.getKey());
    }
    boolean onlyMarkOrFeedback =
        keys.stream().allMatch(k -> k.equals("mark") || k.equals("feedback"));
    if (onlyMarkOrFeedback) {
      checkArgument(objectNode.has("mark"), "Assessment node must contain a mark property.");
      JsonNode markNode = objectNode.get("mark");
      checkArgument(markNode.isNumber(), "Assessment mark must be numeric.");
      double mark = markNode.asDouble();
      String feedback = "";
      if (objectNode.has("feedback")) {
        JsonNode feedbackNode = objectNode.get("feedback");
        checkArgument(feedbackNode.isString(), "Assessment feedback must be a string.");
        feedback = feedbackNode.stringValue();
      }
      return Assessment.fromOptionalFeedback(mark, feedback);
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
