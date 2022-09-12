/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.extension.ExtensionUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterExtension {

  @JsonProperty("name")
  @NotNull(message = "name cannot be null")
  private String name;

  @JsonProperty("publisher")
  private String publisher;

  @JsonProperty("version")
  private String version;

  @JsonProperty("repository")
  private String repository;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPublisher() {
    return publisher;
  }

  @JsonIgnore
  public String getPublisherOrDefault() {
    return Optional.ofNullable(publisher).orElse(ExtensionUtil.DEFAULT_PUBLISHER);
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  public String getVersion() {
    return version;
  }

  @JsonIgnore
  public String getVersionOrDefaultChannel() {
    return Optional.ofNullable(version).orElse(ExtensionUtil.DEFAULT_CHANNEL);
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

  @Override
  public int hashCode() {
    return Objects.hash(name, publisher, repository, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterExtension)) {
      return false;
    }
    StackGresClusterExtension other = (StackGresClusterExtension) obj;
    return Objects.equals(name, other.name) && Objects.equals(publisher, other.publisher)
        && Objects.equals(repository, other.repository) && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
