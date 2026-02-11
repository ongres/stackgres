/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

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
public class StackGresShardedClusterShardingSphereProperties extends JsonObject {

  public StackGresShardedClusterShardingSphereProperties() {
    super();
  }

  public StackGresShardedClusterShardingSphereProperties(Map<String, Object> m) {
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
        .map(citus -> citus.get("group"))
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
        .map(postgresql -> postgresql.get("pg_ctl_timeout"))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast);
  }

  @JsonIgnore
  public void setPgCtlTimeout(Integer pgCtlTimeout) {
    getWritablePostgresql()
        .ifPresent(postgresql -> postgresql.put("pg_ctl_timeout", pgCtlTimeout));
  }

  @JsonIgnore
  public Optional<Map<String, Object>> getCallbacks() {
    return getPostgresql()
        .filter(config -> config.hasObject("callbacks"))
        .map(config -> config.getObject("callbacks"));
  }

  @JsonIgnore
  public void setCallbacks(Map<String, Object> callbacks) {
    getWritablePostgresql()
        .ifPresent(postgresql -> postgresql.put("callbacks", new JsonObject(callbacks)));
  }

  @JsonIgnore
  public Optional<String> getPrePromote() {
    return getPostgresql()
        .map(postgresql -> postgresql.get("pre_promote"))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }

  @JsonIgnore
  public void setPrePromote(String prePromote) {
    getWritablePostgresql()
        .ifPresent(postgresql -> postgresql.put("pre_promote", prePromote));
  }

  @JsonIgnore
  public Optional<String> getBeforeStop() {
    return getPostgresql()
        .map(postgresql -> postgresql.get("before_stop"))
        .filter(String.class::isInstance)
        .map(String.class::cast);
  }

  @JsonIgnore
  public void setBeforeStop(String beforeStop) {
    getWritablePostgresql()
        .ifPresent(postgresql -> postgresql.put("before_stop", beforeStop));
  }

  private Optional<JsonObject> getWritablePostgresql() {
    return Optional.of(this)
        .filter(config -> config.hasWritableObject("postgresql"))
        .or(() -> Optional.of(this)
            .map(config -> {
              var oldPostgresql = Optional.of(config)
                  .filter(c -> c.hasObject("postgresql"))
                  .map(c -> c.getObject("postgresql"));
              config.put("postgresql", new JsonObject());
              oldPostgresql.ifPresent(postgresql -> config.getObject("postgresql").putAll(postgresql));
              return this;
            }))
        .map(config -> config.getObject("postgresql"));
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
