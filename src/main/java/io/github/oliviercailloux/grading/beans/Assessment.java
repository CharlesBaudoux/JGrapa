package io.github.oliviercailloux.grading.beans;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.String;

@Serdeable
public record Assessment(
    @NotNull float mark,
    @Size(min = 1) String feedback
) {
}
