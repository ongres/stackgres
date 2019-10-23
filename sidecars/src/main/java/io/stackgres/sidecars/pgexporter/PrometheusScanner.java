/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesScanner;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfig;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigDefinition;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigDoneable;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigList;

@ApplicationScoped
public class PrometheusScanner implements KubernetesScanner<PrometheusConfigList> {

  private KubernetesClient client;

  @Inject
  public PrometheusScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<PrometheusConfigList> findResources() {

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, PrometheusConfigDefinition.NAME);

    return crd.map(cr -> client.customResources(cr,
        PrometheusConfig.class,
        PrometheusConfigList.class,
        PrometheusConfigDoneable.class)
        .inAnyNamespace().list());
  }

  @Override
  public Optional<PrometheusConfigList> findResources(String namespace) {
    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, PrometheusConfigDefinition.NAME);

    return crd.map(cr -> client.customResources(cr,
        PrometheusConfig.class,
        PrometheusConfigList.class,
        PrometheusConfigDoneable.class)
        .inNamespace(namespace).list());
  }
}
