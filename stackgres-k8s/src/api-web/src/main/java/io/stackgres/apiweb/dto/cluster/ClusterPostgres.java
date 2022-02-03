/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPostgres {

  @JsonProperty("version")
  private String version;

  @JsonProperty("flavor")
  private String flavor;

  @JsonProperty("extensions")
  private List<ClusterExtension> extensions;

  @JsonProperty("ssl")
  private ClusterSsl ssl;

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

  public List<ClusterExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<ClusterExtension> extensions) {
    this.extensions = extensions;
  }

  public ClusterSsl getSsl() {
    return ssl;
  }

  public void setSsl(ClusterSsl ssl) {
    this.ssl = ssl;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterPostgres that = (ClusterPostgres) o;
    return Objects.equals(version, that.version)
        && Objects.equals(flavor, that.flavor)
        && Objects.equals(extensions, that.extensions)
        && Objects.equals(ssl, that.ssl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, flavor, extensions, ssl);
  }
}
