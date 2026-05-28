package io.github.oliviercailloux.grading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class Criterion implements Serializable {

  public static Criterion given(String name) {
    return new Criterion(name);
  }

  private final String name;

  Criterion(String name) {
    this.name = checkNotNull(name);
  }

  /**
   * Returns the name of this criterion.
   */
  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o2) {
    if (!(o2 instanceof Criterion)) {
      return false;
    }
    final Criterion c2 = (Criterion) o2;
    return name().equals(c2.name());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(name).toString();
  }
}
