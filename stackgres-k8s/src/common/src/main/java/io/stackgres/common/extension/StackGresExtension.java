/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresExtension {

  @NotNull(message = "name is required")
  private String name;

  private String repository;

  private String publisher;

  private String license;

  @JsonProperty("abstract")
  private String abstractDescription;

  private String description;

  private List<String> tags;

  @NotEmpty(message = "versions is required and must not be empty")
  @Valid
  private List<StackGresExtensionVersion> versions;

  private Map<String, String> channels;

  private String url;

  private String source;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getPublisher() {
    return publisher;
  }

  public void setPublisher(String publisher) {
    this.publisher = publisher;
  }

  @JsonIgnore
  public String getPublisherOrDefault() {
    return Optional.ofNullable(publisher).orElse(ExtensionUtil.DEFAULT_PUBLISHER);
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public String getAbstractDescription() {
    return abstractDescription;
  }

  public void setAbstractDescription(String abstractDescription) {
    this.abstractDescription = abstractDescription;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<StackGresExtensionVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<StackGresExtensionVersion> versions) {
    this.versions = versions;
  }

  public Map<String, String> getChannels() {
    return channels;
  }

  public void setChannels(Map<String, String> channels) {
    this.channels = channels;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, getPublisherOrDefault());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtension)) {
      return false;
    }
    StackGresExtension other = (StackGresExtension) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(getPublisherOrDefault(), other.getPublisherOrDefault());
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
