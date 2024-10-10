/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.docir.model.Extension;
import io.stackgres.common.docir.model.ExtensionMetadata;
import io.stackgres.common.docir.model.ExtensionVersion;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DocirExtension {

  private String repository;

  private String name;

  @JsonProperty("licenses")
  private String license;

  @JsonProperty("description")
  private String abstractDescription;

  @JsonProperty("longDescription")
  private String description;

  @JsonProperty("extensionVersions")
  private List<DocirExtensionVersion> versions;

  private String url;

  private String source;

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public List<DocirExtensionVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<DocirExtensionVersion> versions) {
    this.versions = versions;
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
    return Objects.hash(abstractDescription, description, license, name, repository,
        source, url, versions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirExtension)) {
      return false;
    }
    DocirExtension other = (DocirExtension) obj;
    return Objects.equals(abstractDescription, other.abstractDescription)
        && Objects.equals(description, other.description)
        && Objects.equals(license, other.license)
        && Objects.equals(name, other.name)
        && Objects.equals(repository, other.repository)
        && Objects.equals(source, other.source)
        && Objects.equals(url, other.url)
        && Objects.equals(versions, other.versions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public static DocirExtension fromMetadata(
      String repository,
      Extension extension,
      List<DocirExtensionVersion> versions) {
    final Optional<ExtensionVersion> firstVersionMetadata =
        Optional.ofNullable(extension.extensionVersions())
        .stream()
        .flatMap(Stream::of)
        .findFirst();
    return new DocirExtensionBuilder()
        .withRepository(repository)
        .withName(extension.name())
        .withAbstractDescription(
            firstVersionMetadata
            .map(ExtensionVersion::metadata)
            .map(ExtensionMetadata::description)
            .orElse(null))
        .withDescription(
            firstVersionMetadata
            .map(ExtensionVersion::metadata)
            .map(ExtensionMetadata::longDescription)
            .orElse(null))
        .withLicense(
            firstVersionMetadata
            .map(ExtensionVersion::metadata)
            .map(ExtensionMetadata::licenses)
            .orElse(null))
        .withVersions(versions)
        .build();
  }
    
}
