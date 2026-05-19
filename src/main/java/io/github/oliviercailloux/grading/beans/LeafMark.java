package io.github.oliviercailloux.grading.beans;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

@Serdeable
public record LeafMark(
    @NotNull float mark
) {
}
