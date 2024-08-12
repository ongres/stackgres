/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.script;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgscript.StackGresScript;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresScript.class)
public class ScriptDto extends ResourceDto {

  private ScriptSpec spec;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
