/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class ScriptDto extends ResourceDto {

  @JsonProperty("spec")
  private ScriptSpec spec;

  @JsonProperty("status")
  private ScriptStatus status;

  public ScriptSpec getSpec() {
    return spec;
  }

  public void setSpec(ScriptSpec spec) {
    this.spec = spec;
  }

  public ScriptStatus getStatus() {
    return status;
  }

  public void setStatus(ScriptStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScriptDto)) {
      return false;
    }
    ScriptDto other = (ScriptDto) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
