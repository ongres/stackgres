/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter.customresources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPostgresExporterConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 2000013861182789247L;

  @JsonProperty("postgres_exporter_version")
  private String postgresExporterVersion;

  @JsonProperty("create_service_monitor")
  private Boolean createServiceMonitor;

  @JsonProperty("prometheus_installations")
  private List<PrometheusInstallation> prometheusInstallations;

  public String getPostgresExporterVersion() {
    return postgresExporterVersion;
  }

  public void setPostgresExporterVersion(String postgresExporterVersion) {
    this.postgresExporterVersion = postgresExporterVersion;
  }

  public Boolean getCreateServiceMonitor() {
    return createServiceMonitor;
  }

  public void setCreateServiceMonitor(Boolean createServiceMonitor) {
    this.createServiceMonitor = createServiceMonitor;
  }

  public List<PrometheusInstallation> getPrometheusInstallations() {
    return prometheusInstallations;
  }

  public void setPrometheusInstallations(List<PrometheusInstallation> prometheusInstallations) {
    this.prometheusInstallations = prometheusInstallations;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("postgres_exporter_version", postgresExporterVersion)
        .add("create_service_monitor", createServiceMonitor)
        .toString();
  }

}
