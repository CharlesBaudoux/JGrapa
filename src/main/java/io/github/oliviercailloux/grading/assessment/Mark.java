package io.github.oliviercailloux.grading.assessment;

import static com.google.common.base.Preconditions.checkArgument;

public record Mark(double value) {
  public Mark {
    checkArgument(-1d <= value);
    checkArgument(value <= 1d);
  }
}
