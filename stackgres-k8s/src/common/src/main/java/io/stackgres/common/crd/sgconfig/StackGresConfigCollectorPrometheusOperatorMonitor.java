/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.external.prometheus.PodMonitorSpec;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigCollectorPrometheusOperatorMonitor {

  private String name;

  private String namespace;

  private ObjectMeta metadata;

  private PodMonitorSpec spec;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public ObjectMeta getMetadata() {
    return metadata;
  }

  public void setMetadata(ObjectMeta metadata) {
    this.metadata = metadata;
  }

  public PodMonitorSpec getSpec() {
    return spec;
  }

  public void setSpec(PodMonitorSpec spec) {
    this.spec = spec;
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, name, namespace, spec);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollectorPrometheusOperatorMonitor)) {
      return false;
    }
    StackGresConfigCollectorPrometheusOperatorMonitor other = (StackGresConfigCollectorPrometheusOperatorMonitor) obj;
    return Objects.equals(metadata, other.metadata) && Objects.equals(name, other.name)
        && Objects.equals(namespace, other.namespace) && Objects.equals(spec, other.spec);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
