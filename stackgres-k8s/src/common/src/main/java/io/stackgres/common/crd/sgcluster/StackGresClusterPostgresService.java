/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jooq.lambda.Seq;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterPostgresService {

  private Boolean enabled;

  private String type;

  private Map<String, String> annotations;

  @JsonIgnore
  @AssertTrue(message = "type must be one of ClusterIP, LoadBalancer or NodePort")
  public boolean isTypeValid() {
    return type == null || Seq.of(StackGresClusterPostgresServiceType.values())
        .map(StackGresClusterPostgresServiceType::type)
        .anyMatch(type -> type.equals(this.type));
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPostgresService)) {
      return false;
    }
    StackGresClusterPostgresService other = (StackGresClusterPostgresService) obj;
    return Objects.equals(annotations, other.annotations) && Objects.equals(enabled, other.enabled)
        && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations, enabled, type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("enabled", enabled)
        .add("type", type)
        .add("annotations", annotations)
        .toString();
  }
}
