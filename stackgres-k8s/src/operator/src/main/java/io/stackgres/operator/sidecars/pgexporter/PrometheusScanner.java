/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.AbstractKubernetesCustomResourceScanner;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfig;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigDefinition;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigDoneable;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigList;

@ApplicationScoped
public class PrometheusScanner
    extends AbstractKubernetesCustomResourceScanner<PrometheusConfig, PrometheusConfigList,
    PrometheusConfigDoneable> {

  /**
   * Create a {@code PrometheusScanner} instance.
   */
  @Inject
  public PrometheusScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, PrometheusConfigDefinition.NAME,
        PrometheusConfig.class, PrometheusConfigList.class,
        PrometheusConfigDoneable.class);
  }

  public PrometheusScanner() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
