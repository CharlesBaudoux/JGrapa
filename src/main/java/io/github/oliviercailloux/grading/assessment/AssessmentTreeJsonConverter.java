package io.github.oliviercailloux.grading.assessment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.VerifyException;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;
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

  public static AssessmentTreeJsonConverter usingDefault() {
    return new AssessmentTreeJsonConverter(new ObjectMapper(), assessmentTreeSchema());
  }

  public static AssessmentTreeJsonConverter using(ObjectMapper mapper) {
    return new AssessmentTreeJsonConverter(mapper, assessmentTreeSchema());
  }

  public static AssessmentTreeJsonConverter using(ObjectMapper mapper,
      Schema assessmentTreeSchema) {
    return new AssessmentTreeJsonConverter(mapper, assessmentTreeSchema);
  }

  private final ObjectMapper mapper;
  private final Schema assessmentTreeSchema;

  private AssessmentTreeJsonConverter(ObjectMapper mapper, Schema assessmentTreeSchema) {
    this.mapper = checkNotNull(mapper);
    this.assessmentTreeSchema = checkNotNull(assessmentTreeSchema);
  }

  public String toJsonString(AssessmentTree tree) throws JacksonException {
    ObjectNode node = toJson(tree);
    return mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValueAsString(node);
  }

  public AssessmentTree fromJson(String json) throws JacksonException {
    JsonNode node = mapper.readTree(checkNotNull(json));
    if (!(node instanceof ObjectNode objectNode)) {
      throw new IllegalArgumentException("Assessment tree node must be a JSON object.");
    }
    return fromJson(objectNode);
  }

  public ObjectNode toJson(AssessmentTree tree) {
    ObjectNode main = toJsonInternal(tree);
    verify(assessmentTreeSchema.validate(main).isEmpty(),
        "Generated JSON does not conform to schema.");
    return main;
  }

  private ObjectNode toJsonInternal(AssessmentTree tree) {
    return switch (tree) {
      case Assessment assessment -> {
        ObjectNode node = mapper.createObjectNode();
        double mark = assessment.mark();
        if (DoubleMath.isMathematicalInteger(mark)) {
          long integralMark = (long) mark;
          if (Integer.MIN_VALUE <= mark && mark <= Integer.MAX_VALUE) {
            node.put("mark", Ints.checkedCast(integralMark));
          } else {
            node.put("mark", integralMark);
          }
        } else {
          node.put("mark", mark);
        }
        if (!assessment.feedback().isEmpty()) {
          node.put("feedback", assessment.feedback());
        }
        yield node;
      }
      case CompositeAssessmentTree composite -> {
        ObjectNode node = mapper.createObjectNode();
        for (Map.Entry<Criterion, AssessmentTree> entry : composite.getChildren().entrySet()) {
          node.set(entry.getKey().getName(), toJsonInternal(entry.getValue()));
        }
        yield node;
      }
    };
  }

  public AssessmentTree fromJson(ObjectNode node) {
    checkArgument(assessmentTreeSchema.validate(node).isEmpty(),
        "Provided JSON does not conform to schema.");
    return fromJsonInternal(node);
  }

  private AssessmentTree fromJsonInternal(ObjectNode node) {
    boolean assessment = node.has("mark") && node.get("mark").isNumber();
    if (assessment) {
      node.propertyNames().stream()
          .filter(propertyName -> !propertyName.equals("mark") && !propertyName.equals("feedback"))
          .findAny().ifPresent(invalidProperty -> {
            throw new IllegalArgumentException("Assessment node must not contain property '"
                + invalidProperty + "' besides mark and feedback.");
          });
      JsonNode markNode = node.get("mark");
      double mark = markNode.asDouble();
      String feedback;
      if (node.has("feedback")) {
        JsonNode feedbackNode = node.get("feedback");
        checkArgument(feedbackNode.isString(), "Assessment feedback must be a string.");
        feedback = feedbackNode.stringValue();
      } else {
        feedback = "";
      }
      return new Assessment(mark, feedback);
    }
    Map<Criterion, AssessmentTree> children = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> property : node.properties()) {
      Criterion criterion = Criterion.given(property.getKey());
      JsonNode value = property.getValue();
      if (!(value instanceof ObjectNode objectValue)) {
        throw new VerifyException(
            "Assessment tree node in conforming node should be a JSON object.");
      }
      AssessmentTree child = fromJsonInternal(objectValue);
      children.put(criterion, child);
    }
    return CompositeAssessmentTree.given(children);
  }
}
