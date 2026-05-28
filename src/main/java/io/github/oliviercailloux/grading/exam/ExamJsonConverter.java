package io.github.oliviercailloux.grading.exam;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;

public class ExamJsonConverter {
  public static Schema examSchema() {
    SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft202012(),
        builder -> builder.schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers.mapPrefix(
            "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/exam.schema.json",
            "classpath:/io/github/oliviercailloux/grading/schemas/exam.schema.json")));
    Schema examSchema = schemaRegistry.getSchema(SchemaLocation.of(
        "https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/exam.schema.json"));
    return examSchema;
  }

  
}
