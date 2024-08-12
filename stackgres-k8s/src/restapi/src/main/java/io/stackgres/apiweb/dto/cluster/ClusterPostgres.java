/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterPostgres {

  private String version;

  private String flavor;

  private List<ClusterExtension> extensions;

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

}
