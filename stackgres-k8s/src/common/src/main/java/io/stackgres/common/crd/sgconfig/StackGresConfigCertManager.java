/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigCertManager {

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
  public int hashCode() {
    return Objects.hash(autoConfigure, duration, encoding, renewBefore, size);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCertManager)) {
      return false;
    }
    StackGresConfigCertManager other = (StackGresConfigCertManager) obj;
    return Objects.equals(autoConfigure, other.autoConfigure)
        && Objects.equals(duration, other.duration)
        && Objects.equals(encoding, other.encoding)
        && Objects.equals(renewBefore, other.renewBefore)
        && Objects.equals(size, other.size);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
