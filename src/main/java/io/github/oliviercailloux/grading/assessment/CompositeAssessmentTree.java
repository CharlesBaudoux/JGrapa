package io.github.oliviercailloux.grading.assessment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.github.oliviercailloux.grading.Criterion;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("serial")
public final class CompositeAssessmentTree implements AssessmentTree, Serializable {
  public static CompositeAssessmentTree given(Map<Criterion, ? extends AssessmentTree> children) {
    return new CompositeAssessmentTree(children);
  }

  private final ImmutableMap<Criterion, AssessmentTree> children;

  CompositeAssessmentTree(Map<Criterion, ? extends AssessmentTree> children) {
    checkNotNull(children);
    checkArgument(!children.isEmpty(), "Children must be non-empty.");
    for (Map.Entry<Criterion, ? extends AssessmentTree> entry : children.entrySet()) {
      checkNotNull(entry.getKey());
      checkNotNull(entry.getValue());
    }
    this.children = ImmutableMap.copyOf(children);
  }

  public ImmutableMap<Criterion, AssessmentTree> getChildren() {
    return children;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof CompositeAssessmentTree that))
      return false;
    return children.equals(that.children);
  }

  @Override
  public int hashCode() {
    return Objects.hash(children);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("children", children).toString();
  }
}
