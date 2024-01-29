/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.prometheus.Prometheus;
import io.stackgres.common.prometheus.PrometheusList;
import io.stackgres.common.resource.AbstractCustomResourceScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PrometheusScanner
    extends AbstractCustomResourceScanner<Prometheus, PrometheusList> {

  /**
   * Create a {@code PrometheusScanner} instance.
   */
  @Inject
  public PrometheusScanner(KubernetesClient client) {
    super(client, Prometheus.class, PrometheusList.class);
  }

}
