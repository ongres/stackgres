/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.app.postgres.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PostgresService implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  private Boolean enabled;

  private String type;

  private List<String> externalIPs;

  public PostgresService() {}

  public PostgresService(Boolean enabled, String type, List<String> externalIPs) {
    this.enabled = enabled;
    this.type = type;
    this.externalIPs = externalIPs;
  }

  public PostgresService(Boolean enabled, String type) {
    this.enabled = enabled;
    this.type = type;
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

  public List<String> getExternalIPs() {
    if (this.externalIPs == null) {
      this.externalIPs = new ArrayList<String>();
    }
    return externalIPs;
  }

  public void setExternalIPs(List<String> externalIPs) {
    this.externalIPs = externalIPs;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostgresService that = (PostgresService) o;
    return Objects.equals(enabled, that.enabled)
        && Objects.equals(type, that.type)
        && Objects.equals(externalIPs, that.externalIPs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, type, externalIPs);
  }
}
