/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.Kind;
import io.stackgres.operator.common.ResourceCreator;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitor;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorDefinition;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorDoneable;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Kind("ServiceMonitor")
@ApplicationScoped
public class PrometheusServiceMonitorCreator implements ResourceCreator {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrometheusServiceMonitorCreator.class);

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Prometheus service monitor creator registered");
    KubernetesDeserializer.registerCustomKind(ServiceMonitorDefinition.APIVERSION,
        ServiceMonitorDefinition.KIND, ServiceMonitor.class);
  }

  @Override
  public void createOrReplace(KubernetesClient client, HasMetadata resource) {

    ServiceMonitor serviceMonitor = (ServiceMonitor) resource;

    Optional<CustomResourceDefinition> crd =
        ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME);

    crd.ifPresent(cr -> {
      MixedOperation<ServiceMonitor,
          ServiceMonitorList,
          ServiceMonitorDoneable,
          Resource<ServiceMonitor,
              ServiceMonitorDoneable>> prometheusCli = client
          .customResources(cr,
              ServiceMonitor.class,
              ServiceMonitorList.class,
              ServiceMonitorDoneable.class);
      prometheusCli.inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(serviceMonitor);
    });

  }
}
