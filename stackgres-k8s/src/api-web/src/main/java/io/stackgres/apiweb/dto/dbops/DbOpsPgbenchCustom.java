/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsPgbenchCustom {

  private DbOpsPgbenchCustomScript initialization;

  private List<DbOpsPgbenchCustomScript> scripts;

  public DbOpsPgbenchCustomScript getInitialization() {
    return initialization;
  }

  public void setInitialization(DbOpsPgbenchCustomScript initialization) {
    this.initialization = initialization;
  }

  public List<DbOpsPgbenchCustomScript> getScripts() {
    return scripts;
  }

  public void setScripts(List<DbOpsPgbenchCustomScript> scripts) {
    this.scripts = scripts;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
