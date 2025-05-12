/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

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
public class StackGresPostgresServiceNodePort {

  protected Integer pgport;

  protected Integer replicationport;

  protected Integer babelfish;

  public StackGresPostgresServiceNodePort(
      final Integer pgport,
      final Integer replicationport,
      final Integer babelfish) {
    this.pgport = pgport;
    this.replicationport = replicationport;
    this.babelfish = babelfish;
  }

  public StackGresPostgresServiceNodePort() {}

  public Integer getPgport() {
    return pgport;
  }

  public void setPgport(Integer pgport) {
    this.pgport = pgport;
  }

  public Integer getReplicationport() {
    return replicationport;
  }

  public void setReplicationport(Integer replicationport) {
    this.replicationport = replicationport;
  }

  public Integer getBabelfish() {
    return babelfish;
  }

  public void setBabelfish(Integer babelfish) {
    this.babelfish = babelfish;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (this == o) {
      return true;
    }

    if (!(o instanceof StackGresPostgresServiceNodePort nodePort)) {
      return false;
    }

    return Objects.equals(pgport, nodePort.pgport)
        && Objects.equals(replicationport, nodePort.replicationport)
        && Objects.equals(babelfish, nodePort.babelfish);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pgport, replicationport, babelfish);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
