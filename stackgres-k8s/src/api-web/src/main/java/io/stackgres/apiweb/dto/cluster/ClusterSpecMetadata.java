/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterSpecMetadata {

  private ClusterSpecAnnotations annotations;

  private ClusterSpecLabels labels;

  public ClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(ClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  public ClusterSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(ClusterSpecLabels labels) {
    this.labels = labels;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterSpecMetadata that = (ClusterSpecMetadata) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(labels, that.labels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations, labels);
  }
}
