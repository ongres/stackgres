/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgcluster;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterPod {

  @JsonProperty("persistentVolume")
  @Valid
  @NotNull(message = "Pod's persistent volume must be specified")
  private StackGresPodPersistenceVolume persistentVolume;

  public StackGresPodPersistenceVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresPodPersistenceVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }
}
