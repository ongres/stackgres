/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

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
public class StackGresStreamSpecMetadata {

  private StackGresStreamSpecAnnotations annotations;

  private StackGresStreamSpecLabels labels;

  public StackGresStreamSpecAnnotations getAnnotations() {
    return annotations;
  }

  public StackGresStreamSpecLabels getLabels() {
    return labels;
  }

  public void setLabels(StackGresStreamSpecLabels labels) {
    this.labels = labels;
  }

  public void setAnnotations(StackGresStreamSpecAnnotations annotations) {
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
    if (!(obj instanceof StackGresStreamSpecMetadata)) {
      return false;
    }
    StackGresStreamSpecMetadata other = (StackGresStreamSpecMetadata) obj;
    return Objects.equals(annotations, other.annotations)
        && Objects.equals(labels, other.labels);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
