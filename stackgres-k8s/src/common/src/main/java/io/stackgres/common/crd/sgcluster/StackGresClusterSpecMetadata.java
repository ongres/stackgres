/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterSpecMetadata {

  private StackGresClusterSpecAnnotations annotations;

  private StackGresClusterSpecLabels labels;

  public StackGresClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public StackGresClusterSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(StackGresClusterSpecLabels labels) {
    this.labels = labels;
  }

  public void setAnnotations(StackGresClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations, labels);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterSpecMetadata)) {
      return false;
    }
    StackGresClusterSpecMetadata other = (StackGresClusterSpecMetadata) obj;
    return Objects.equals(annotations, other.annotations)
        && Objects.equals(labels, other.labels);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
