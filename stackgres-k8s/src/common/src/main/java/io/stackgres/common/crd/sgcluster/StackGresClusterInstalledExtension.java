/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterInstalledExtension {

  @JsonProperty("name")
  @NotNull(message = "name cannot be null")
  private String name;

  @JsonProperty("publisher")
  @NotNull(message = "publisher cannot be null")
  private String publisher;

  @JsonProperty("version")
  @NotNull(message = "version cannot be null")
  private String version;

  @JsonProperty("repository")
  @NotNull(message = "repository cannot be null")
  private String repository;

  @JsonProperty("postgresVersion")
  @NotNull(message = "postgresVersion cannot be null")
  private String postgresVersion;

  @JsonProperty("build")
  private String build;

  @JsonProperty("extraMounts")
  private List<String> extraMounts;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public List<String> getExtraMounts() {
    return extraMounts;
  }

  public void setExtraMounts(List<String> extraMounts) {
    this.extraMounts = extraMounts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(build, extraMounts, name, postgresVersion, publisher, repository,
        version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterInstalledExtension)) {
      return false;
    }
    StackGresClusterInstalledExtension other = (StackGresClusterInstalledExtension) obj;
    return Objects.equals(build, other.build) && Objects.equals(extraMounts, other.extraMounts)
        && Objects.equals(name, other.name)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(publisher, other.publisher)
        && Objects.equals(repository, other.repository) && Objects.equals(version, other.version);
  }

  public boolean same(StackGresClusterInstalledExtension other) {
    return Objects.equals(name, other.name);
  }

  public boolean same(StackGresClusterExtension other) {
    return Objects.equals(name, other.getName());
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
