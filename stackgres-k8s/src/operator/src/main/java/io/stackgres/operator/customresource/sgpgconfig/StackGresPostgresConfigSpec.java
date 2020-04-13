/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgpgconfig;

import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPostgresConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("postgresVersion")
  @NotBlank(message = "The PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("postgresql.conf")
  @NotEmpty(message = "postgresql.conf should not be empty")
  private Map<String, String> postgresqlConf;

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public Map<String, String> getPostgresqlConf() {
    return postgresqlConf;
  }

  public void setPostgresqlConf(Map<String, String> postgresqlConf) {
    this.postgresqlConf = postgresqlConf;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("pgVersion", postgresVersion)
        .add("postgresql.conf", postgresqlConf)
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
    StackGresPostgresConfigSpec that = (StackGresPostgresConfigSpec) o;
    return Objects.equals(postgresVersion, that.postgresVersion)
        && Objects.equals(postgresqlConf, that.postgresqlConf);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postgresVersion, postgresqlConf);
  }
}
