package io.github.oliviercailloux.grading.beans;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import java.lang.Object;
import java.util.List;

@Serdeable
public record CompositeMark(
    @NotNull List<Object> children
) {
}
