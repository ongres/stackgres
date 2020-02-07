/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.sidecars.pgbouncer.customresources;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPgbouncerConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  @JsonProperty("pgbouncer.ini")
  @NotEmpty(message = "pgbouncer.ini should not be empty")
  private Map<String, String> pgbouncerConf;

  public Map<String, String> getPgbouncerConf() {
    return pgbouncerConf;
  }

  public void setPgbouncerConf(Map<String, String> pgbouncerConf) {
    this.pgbouncerConf = pgbouncerConf;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("pgbouncer.ini", pgbouncerConf)
        .toString();
  }

}
