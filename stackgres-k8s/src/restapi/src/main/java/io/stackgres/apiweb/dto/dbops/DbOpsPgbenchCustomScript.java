/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchCustomScript {

  private String script;

  private DbOpsPgbenchCustomScriptFrom scriptFrom;

  private String builtin;

  private Integer weight;

  public String getScript() {
    return script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public DbOpsPgbenchCustomScriptFrom getScriptFrom() {
    return scriptFrom;
  }

  public void setScriptFrom(DbOpsPgbenchCustomScriptFrom scriptFrom) {
    this.scriptFrom = scriptFrom;
  }

  public String getBuiltin() {
    return builtin;
  }

  public void setBuiltin(String builtin) {
    this.builtin = builtin;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
