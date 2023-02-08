/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.prometheus.PrometheusConfig;
import io.stackgres.common.prometheus.PrometheusConfigList;
import io.stackgres.common.resource.AbstractCustomResourceScanner;

@ApplicationScoped
public class PrometheusScanner
    extends AbstractCustomResourceScanner<PrometheusConfig, PrometheusConfigList> {

  /**
   * Create a {@code PrometheusScanner} instance.
   */
  @Inject
  public PrometheusScanner(KubernetesClient client) {
    super(client, PrometheusConfig.class, PrometheusConfigList.class);
  }

}
