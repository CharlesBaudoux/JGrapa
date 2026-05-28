package io.github.oliviercailloux.grading.exam;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.aggregator.Aggregator;
import io.github.oliviercailloux.grading.assessment.AssessmentTree;
import java.util.Map;

public class Exam {
  public static Exam given(Map<StudentId, AssessmentTree> assessments, Aggregator aggregator) {
    return new Exam(assessments, aggregator);
  }

  private final ImmutableMap<StudentId, AssessmentTree> assessments;
  private final Aggregator aggregator;

  private Exam(Map<StudentId, AssessmentTree> assessments, Aggregator aggregator) {
    this.assessments = ImmutableMap.copyOf(assessments);
    this.aggregator = checkNotNull(aggregator);
  }
}
