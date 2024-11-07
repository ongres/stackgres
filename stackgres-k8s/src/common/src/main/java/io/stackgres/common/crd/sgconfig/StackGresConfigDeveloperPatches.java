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
public class StackGresConfigDeveloperPatches {

  private StackGresConfigDeveloperContainerPatches operator;

  private StackGresConfigDeveloperContainerPatches restapi;

  private StackGresConfigDeveloperContainerPatches adminui;

  private StackGresConfigDeveloperContainerPatches jobs;

  private StackGresConfigDeveloperContainerPatches clusterController;

  private StackGresConfigDeveloperContainerPatches stream;

  public StackGresConfigDeveloperContainerPatches getOperator() {
    return operator;
  }

  public void setOperator(StackGresConfigDeveloperContainerPatches operator) {
    this.operator = operator;
  }

  public StackGresConfigDeveloperContainerPatches getRestapi() {
    return restapi;
  }

  public void setRestapi(StackGresConfigDeveloperContainerPatches restapi) {
    this.restapi = restapi;
  }

  public StackGresConfigDeveloperContainerPatches getAdminui() {
    return adminui;
  }

  public void setAdminui(StackGresConfigDeveloperContainerPatches adminui) {
    this.adminui = adminui;
  }

  public StackGresConfigDeveloperContainerPatches getJobs() {
    return jobs;
  }

  public void setJobs(StackGresConfigDeveloperContainerPatches jobs) {
    this.jobs = jobs;
  }

  public StackGresConfigDeveloperContainerPatches getClusterController() {
    return clusterController;
  }

  public void setClusterController(StackGresConfigDeveloperContainerPatches clusterController) {
    this.clusterController = clusterController;
  }

  public StackGresConfigDeveloperContainerPatches getStream() {
    return stream;
  }

  public void setStream(StackGresConfigDeveloperContainerPatches stream) {
    this.stream = stream;
  }

  @Override
  public int hashCode() {
    return Objects.hash(adminui, clusterController, jobs, operator,
        restapi, stream);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigDeveloperPatches)) {
      return false;
    }
    StackGresConfigDeveloperPatches other = (StackGresConfigDeveloperPatches) obj;
    return Objects.equals(adminui, other.adminui)
        && Objects.equals(clusterController, other.clusterController)
        && Objects.equals(jobs, other.jobs) && Objects.equals(operator, other.operator)
        && Objects.equals(restapi, other.restapi) && Objects.equals(stream, other.stream);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
