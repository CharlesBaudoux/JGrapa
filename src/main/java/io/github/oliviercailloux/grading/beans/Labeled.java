package io.github.oliviercailloux.grading.beans;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import java.lang.String;

@Serdeable
public record Labeled(
    @NotNull String label
) {
}
