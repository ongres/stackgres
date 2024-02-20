/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.JsonObject;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackGresClusterPatroniConfig extends JsonObject {

  public StackGresClusterPatroniConfig() {
    super();
  }

  public StackGresClusterPatroniConfig(Map<String, Object> m) {
    super(m);
  }

  @JsonIgnore
  public String getScope() {
    return (String) get("scope");
  }

  @JsonIgnore
  public Optional<Integer> getCitusGroup() {
    return Optional.of(this)
        .filter(config -> config.hasObject("citus"))
        .map(config -> config.getObject("citus"))
        .map(config -> config.get("group"))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast);
  }

  @JsonIgnore
  public Optional<JsonObject> getPostgresql() {
    return Optional.of(this)
        .filter(config -> config.hasObject("postgresql"))
        .map(config -> config.getObject("postgresql"));
  }

  @JsonIgnore
  public Optional<Integer> getPgCtlTimeout() {
    return getPostgresql()
        .map(config -> config.get("pg_ctl_timeout"))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast);
  }

  @JsonIgnore
  public void setPgCtlTimeout(Integer pgCtlTimeout) {
    Optional.of(this)
        .filter(config -> config.hasWritableObject("postgresql"))
        .or(() -> Optional.of(this)
            .map(config -> {
              config.put("postgresql", new JsonObject());
              return this;
            }))
        .map(config -> config.getObject("postgresql"))
        .ifPresent(postgresql -> postgresql.put("pg_ctl_timeout", pgCtlTimeout));
  }

  @JsonIgnore
  public void removePostgresql() {
    Optional.of(this)
        .filter(config -> config.hasObject("postgresql"))
        .ifPresent(config -> config.remove("postgresql"));
  }

  @JsonIgnore
  public boolean isPatroniOnKubernetes() {
    return !(hasObject("consul")
        || hasObject("etcd")
        || hasObject("etcd3")
        || hasObject("zookeeper")
        || hasObject("exhibitor")
        || hasObject("raft"));
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
