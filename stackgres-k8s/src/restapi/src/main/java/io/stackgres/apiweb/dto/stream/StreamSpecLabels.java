/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamSpecLabels {

  private Map<String, String> allResources;

  private Map<String, String> pods;

  private Map<String, String> serviceAccount;

  public Map<String, String> getAllResources() {
    return allResources;
  }

  public void setAllResources(Map<String, String> allResources) {
    this.allResources = allResources;
  }

  public Map<String, String> getPods() {
    return pods;
  }

  public void setPods(Map<String, String> pods) {
    this.pods = pods;
  }

  public Map<String, String> getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(Map<String, String> serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
