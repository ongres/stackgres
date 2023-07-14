/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPostgres {

  @JsonProperty("version")
  @NotBlank(message = "PostgreSQL version is required")
  private String version;

  @JsonProperty("flavor")
  @ValidEnum(enumClass = StackGresPostgresFlavor.class, allowNulls = true,
      message = "flavor must be vanilla or babelfish")
  private String flavor;

  @JsonProperty("ssl")
  @Valid
  private StackGresClusterSsl ssl;

  @JsonProperty("extensions")
  @Valid
  private List<StackGresClusterExtension> extensions;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFlavor() {
    return flavor;
  }

  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }

  public StackGresClusterSsl getSsl() {
    return ssl;
  }

  public void setSsl(StackGresClusterSsl ssl) {
    this.ssl = ssl;
  }

  public List<StackGresClusterExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<StackGresClusterExtension> extensions) {
    this.extensions = extensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, flavor, ssl, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPostgres)) {
      return false;
    }
    StackGresClusterPostgres other = (StackGresClusterPostgres) obj;
    return Objects.equals(extensions, other.extensions) && Objects.equals(flavor, other.flavor)
        && Objects.equals(ssl, other.ssl) && Objects.equals(version, other.version);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
