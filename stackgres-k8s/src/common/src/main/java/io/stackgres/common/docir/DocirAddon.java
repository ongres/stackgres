/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.docir.model.Addon;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DocirAddon {

  private String repository;

  private String name;

  private List<DocirAddonVersion> versions;

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

  public List<DocirAddonVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<DocirAddonVersion> versions) {
    this.versions = versions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, repository, versions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirAddon)) {
      return false;
    }
    DocirAddon other = (DocirAddon) obj;
    return Objects.equals(name, other.name)
        && Objects.equals(repository, other.repository)
        && Objects.equals(versions, other.versions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public static DocirAddon fromMetadata(
      String repository,
      Addon addon,
      List<DocirAddonVersion> versions) {
    return new DocirAddonBuilder()
        .withRepository(repository)
        .withName(addon.name())
        .withVersions(versions)
        .build();
  }

}
