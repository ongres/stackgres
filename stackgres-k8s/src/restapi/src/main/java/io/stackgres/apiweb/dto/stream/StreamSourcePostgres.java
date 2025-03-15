/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamSourcePostgres {

  private String host;

  private Integer port;

  private String database;

  private SecretKeySelector username;

  private SecretKeySelector password;

  private List<String> includes;

  private List<String> excludes;

  private Boolean skipDropReplicationSlotAndPublicationOnTombstone;

  private StreamSourcePostgresDebeziumProperties debeziumProperties;

  private String usernameValue;

  private String passwordValue;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public SecretKeySelector getUsername() {
    return username;
  }

  public void setUsername(SecretKeySelector username) {
    this.username = username;
  }

  public SecretKeySelector getPassword() {
    return password;
  }

  public void setPassword(SecretKeySelector password) {
    this.password = password;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public Boolean getSkipDropReplicationSlotAndPublicationOnTombstone() {
    return skipDropReplicationSlotAndPublicationOnTombstone;
  }

  public void setSkipDropReplicationSlotAndPublicationOnTombstone(
      Boolean skipDropReplicationSlotAndPublicationOnTombstone) {
    this.skipDropReplicationSlotAndPublicationOnTombstone = skipDropReplicationSlotAndPublicationOnTombstone;
  }

  public StreamSourcePostgresDebeziumProperties getDebeziumProperties() {
    return debeziumProperties;
  }

  public void setDebeziumProperties(
      StreamSourcePostgresDebeziumProperties debeziumProperties) {
    this.debeziumProperties = debeziumProperties;
  }

  public String getUsernameValue() {
    return usernameValue;
  }

  public void setUsernameValue(String usernameValue) {
    this.usernameValue = usernameValue;
  }

  public String getPasswordValue() {
    return passwordValue;
  }

  public void setPasswordValue(String passwordValue) {
    this.passwordValue = passwordValue;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
