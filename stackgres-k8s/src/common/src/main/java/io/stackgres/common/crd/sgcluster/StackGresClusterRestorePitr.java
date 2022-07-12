/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterRestorePitr implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("restoreToTimestamp")
  private String restoreToTimestamp;

  @ReferencedField("restoreToTimestamp")
  interface RestoreToTimestamp extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "restoreToTimestamp must be in ISO 8601 date format:"
      + " `YYYY-MM-DDThh:mm:ss.ddZ`.",
      payload = RestoreToTimestamp.class)
  public boolean isRestoreToTimestampValid() {
    try {
      if (restoreToTimestamp != null) {
        Instant.parse(restoreToTimestamp);
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String getRestoreToTimestamp() {
    return restoreToTimestamp;
  }

  public void setRestoreToTimestamp(String restoreToTimestamp) {
    this.restoreToTimestamp = restoreToTimestamp;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("restoreToTimestamp", restoreToTimestamp)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresClusterRestorePitr that = (StackGresClusterRestorePitr) o;
    return Objects.equals(restoreToTimestamp, that.restoreToTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(restoreToTimestamp);
  }
}
