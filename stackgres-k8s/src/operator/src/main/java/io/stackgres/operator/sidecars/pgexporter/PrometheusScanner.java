/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.resource.AbstractKubernetesCustomResourceScanner;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfig;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigDefinition;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigDoneable;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigList;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple5;

@ApplicationScoped
public class PrometheusScanner
    extends AbstractKubernetesCustomResourceScanner<PrometheusConfig, PrometheusConfigList> {

  private final KubernetesClient client;

  /**
   * Create a {@code PrometheusScanner} instance.
   */
  @Inject
  public PrometheusScanner(KubernetesClient client) {
    this.client = client;
  }

  @Override
  protected Tuple5<KubernetesClient, String, Class<PrometheusConfig>,
      Class<PrometheusConfigList>, Class<? extends Doneable<PrometheusConfig>>> arguments() {
    return Tuple.tuple(client, PrometheusConfigDefinition.NAME,
        PrometheusConfig.class, PrometheusConfigList.class,
        PrometheusConfigDoneable.class);
  }

}
