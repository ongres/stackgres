/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsBenchmark {

  @NotEmpty(message = "spec.benchmark.type must be provided")
  private String type;

  private String database;

  @Valid
  private StackGresDbOpsBenchmarkCredentials credentials;

  @Valid
  private StackGresDbOpsPgbench pgbench;

  @ValidEnum(enumClass = DbOpsBenchmarkConnectionType.class, allowNulls = true,
      message = "connectionType must be one of primary-service or replicas-service")
  private String connectionType;

  @ReferencedField("type")
  interface Type extends FieldReference { }

  @ReferencedField("pgbench")
  interface Pgbench extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "type must be pgbench.",
      payload = Type.class)
  public boolean isTypeValid() {
    return type == null || List.of("pgbench").contains(type);
  }

  @JsonIgnore
  @AssertTrue(message = "pgbench section must be provided.",
      payload = Pgbench.class)
  public boolean isPgbenchSectionProvided() {
    return !Objects.equals(type, "pgbench") || pgbench != null;
  }

  @JsonIgnore
  public boolean isTypePgBench() {
    return Objects.equals(type, "pgbench");
  }

  @JsonIgnore
  public boolean isConnectionTypePrimaryService() {
    return connectionType == null
        || Objects.equals(connectionType, DbOpsBenchmarkConnectionType.PRIMARY_SERVICE.toString());
  }

  @JsonIgnore
  public boolean isConnectionTypeReplicasService() {
    return Objects.equals(connectionType, DbOpsBenchmarkConnectionType.REPLICAS_SERVICE.toString());
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public StackGresDbOpsBenchmarkCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(StackGresDbOpsBenchmarkCredentials credentials) {
    this.credentials = credentials;
  }

  public StackGresDbOpsPgbench getPgbench() {
    return pgbench;
  }

  public void setPgbench(StackGresDbOpsPgbench pgbench) {
    this.pgbench = pgbench;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public void setConnectionType(String connectionType) {
    this.connectionType = connectionType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionType, credentials, database, pgbench, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsBenchmark)) {
      return false;
    }
    StackGresDbOpsBenchmark other = (StackGresDbOpsBenchmark) obj;
    return Objects.equals(connectionType, other.connectionType)
        && Objects.equals(credentials, other.credentials)
        && Objects.equals(database, other.database) && Objects.equals(pgbench, other.pgbench)
        && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
