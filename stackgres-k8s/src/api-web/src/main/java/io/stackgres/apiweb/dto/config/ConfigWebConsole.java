/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigWebConsole {

  private ConfigImage image;

  private ConfigWebConsoleService service;

  public ConfigImage getImage() {
    return image;
  }

  public void setImage(ConfigImage image) {
    this.image = image;
  }

  public ConfigWebConsoleService getService() {
    return service;
  }

  public void setService(ConfigWebConsoleService service) {
    this.service = service;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
