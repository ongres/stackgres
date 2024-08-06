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
public class ConfigCertManager {

  private Boolean autoConfigure;

  private String duration;

  private String renewBefore;

  private String encoding;

  private Integer size;

  public Boolean getAutoConfigure() {
    return autoConfigure;
  }

  public void setAutoConfigure(Boolean autoConfigure) {
    this.autoConfigure = autoConfigure;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getRenewBefore() {
    return renewBefore;
  }

  public void setRenewBefore(String renewBefore) {
    this.renewBefore = renewBefore;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
