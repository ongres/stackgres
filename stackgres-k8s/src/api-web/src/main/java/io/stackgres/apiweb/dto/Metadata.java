/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"annotations", "clusterName", "creationTimestamp", "deletionGracePeriodSeconds",
        "deletionTimestamp", "finalizers", "generateName", "generation", "labels", "managedFields",
        "ownerReferences", "resourceVersion", "selfLink"})
public class Metadata {

  @NotNull
  private String namespace;

  @NotNull
  private String name;

  private String uid;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
