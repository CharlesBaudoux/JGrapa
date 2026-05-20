package io.github.oliviercailloux.grading;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;

/** Composite node keyed by criterion label. */
public record CompositeAssessmentTree(ImmutableMap<String, AssessmentTree> children) implements AssessmentTree {
  public CompositeAssessmentTree {
    checkArgument(!children.isEmpty(), "Children must be non-empty.");
  }
}
