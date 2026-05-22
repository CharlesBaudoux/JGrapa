package io.github.oliviercailloux.grading.assessment;

import com.google.common.base.Strings;
import java.util.Objects;

/**
 * A leaf assessment with a mark and textual feedback.
 */
public record Assessment(double mark, String feedback) implements AssessmentTree {
  public static Assessment given(double mark) {
    return new Assessment(mark, "");
  }

  public static Assessment given(double mark, String feedback) {
    return new Assessment(mark, feedback);
  }

  public Assessment {
    feedback = Objects.requireNonNull(feedback);
  }

}
