/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterRestorePitr {

  @JsonProperty("restoreToTimestamp")
  @NotNull
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
