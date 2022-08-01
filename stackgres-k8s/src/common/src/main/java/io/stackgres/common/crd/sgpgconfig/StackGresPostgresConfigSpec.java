/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresPostgresConfigSpec {

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
  public int hashCode() {
    return Objects.hash(postgresVersion, postgresqlConf);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPostgresConfigSpec)) {
      return false;
    }
    StackGresPostgresConfigSpec other = (StackGresPostgresConfigSpec) obj;
    return Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(postgresqlConf, other.postgresqlConf);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
