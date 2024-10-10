/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.OsDetector;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.docir.model.BaseImage;
import io.stackgres.common.docir.model.PlatformWithRevision;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DocirBase {

  private String repository;

  private String name;

  private Integer major;

  private Integer minor;

  private String revision;

  private String os;

  private String arch;

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

  public Integer getMajor() {
    return major;
  }

  public void setMajor(Integer major) {
    this.major = major;
  }

  public Integer getMinor() {
    return minor;
  }

  public void setMinor(Integer minor) {
    this.minor = minor;
  }

  @JsonIgnore
  public String getVersion() {
    return major + "." + minor;
  }

  public String getRevision() {
    return revision;
  }

  @JsonIgnore
  public String getBaseIdentity() {
    return DocirUtil.getBaseIdentity(name, major, minor);
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getOs() {
    return os;
  }

  @JsonIgnore
  public String getOsOrDefault() {
    return Optional.ofNullable(os).orElse(DocirUtil.DEFAULT_OS);
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getArch() {
    return arch;
  }

  @JsonIgnore
  public String getArchOrDefault() {
    return Optional.ofNullable(arch)
        .map(OsDetector::normalizeArch)
        .orElse(DocirUtil.DEFAULT_ARCH);
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arch, name, os, repository, revision, major, minor);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirBase)) {
      return false;
    }
    DocirBase other = (DocirBase) obj;
    return Objects.equals(arch, other.arch)
        && Objects.equals(name, other.name)
        && Objects.equals(os, other.os)
        && Objects.equals(repository, other.repository)
        && Objects.equals(revision, other.revision)
        && Objects.equals(major, other.major)
        && Objects.equals(minor, other.minor);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public static DocirBase fromMetadata(
      String repository,
      BaseImage baseImage,
      PlatformWithRevision platform) {
    return new DocirBaseBuilder()
        .withName(baseImage.name())
        .withMajor(baseImage.major())
        .withMinor(baseImage.minor())
        .withRevision(platform.revision())
        .withOs(platform.os())
        .withArch(platform.architecture())
        .withRepository(repository)
        .build();
  }

}
