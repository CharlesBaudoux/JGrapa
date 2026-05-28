package io.github.oliviercailloux.grading.assessment;

import static com.google.common.base.Preconditions.checkArgument;

public record Mark (double value) implements Comparable<Mark> {
  public static Mark given(double value) {
    return new Mark(value);
  }

  public static Mark max() {
    return new Mark(1d);
  }

  public static Mark min() {
    return new Mark(-1d);
  }

  public Mark {
    checkArgument(-1d <= value);
    checkArgument(value <= 1d);
  }

  @Override
  public int compareTo(Mark other) {
    return Double.compare(this.value, other.value);
  }
}
