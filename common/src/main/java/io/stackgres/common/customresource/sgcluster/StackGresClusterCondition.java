/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionCondition;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterCondition extends CustomResourceDefinitionCondition {

  private static final long serialVersionUID = -175006392037845752L;

  public StackGresClusterCondition() {
    super();
  }

  public StackGresClusterCondition(String type, String status, String reason) {
    super(null, null, reason, status, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final StackGresClusterCondition other = (StackGresClusterCondition) obj;
    return Objects.equals(this.getType(), other.getType())
        && Objects.equals(this.getStatus(), other.getStatus())
        && Objects.equals(this.getReason(), other.getReason());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getType(), this.getStatus(), this.getReason());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("type", getType())
        .add("status", getStatus())
        .add("reason", getReason())
        .toString();
  }

}
