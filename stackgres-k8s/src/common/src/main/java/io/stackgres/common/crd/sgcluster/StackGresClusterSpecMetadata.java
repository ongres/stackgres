/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterSpecMetadata {

  @JsonProperty("annotations")
  @Valid
  private StackGresClusterSpecAnnotations annotations;

  public StackGresClusterSpecAnnotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(StackGresClusterSpecAnnotations annotations) {
    this.annotations = annotations;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterSpecMetadata that = (StackGresClusterSpecMetadata) o;
    return Objects.equals(annotations, that.annotations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("annotations", annotations)
        .toString();
  }
}
