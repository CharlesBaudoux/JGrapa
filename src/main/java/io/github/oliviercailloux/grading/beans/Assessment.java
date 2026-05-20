package io.github.oliviercailloux.grading.beans;

import com.google.common.base.Strings;
import java.util.Objects;

/**
 * A leaf assessment with a mark and textual feedback.
 *
 * <p>In Java, feedback is always present. Use {@link #fromOptionalFeedback(double, String)} when
 * mapping from JSON where feedback may be absent.
 */
public record Assessment(double mark, String feedback) implements AssessmentTree {
  public Assessment {
    feedback = Objects.requireNonNull(feedback);
  }

  /**
   * Normalizes missing feedback to the empty string as the canonical internal "no feedback" value.
   */
  public static Assessment fromOptionalFeedback(double mark, String feedback) {
    return new Assessment(mark, Strings.nullToEmpty(feedback));
  }
}
