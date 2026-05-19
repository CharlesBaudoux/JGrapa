package io.github.oliviercailloux.grading.beans;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import java.lang.String;

@Serdeable
public class Child implements MarkTree {
  private @NotNull String label;

  public @NotNull String getLabel() {
    return this.label;
  }

  public void setLabel(@NotNull String label) {
    this.label = label;
  }
}
