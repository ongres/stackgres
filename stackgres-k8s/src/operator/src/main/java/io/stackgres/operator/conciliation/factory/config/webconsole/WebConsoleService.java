/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminui;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminuiService;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigRestapi;
import io.stackgres.common.crd.sgconfig.StackGresConfigService;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class WebConsoleService
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  @Inject
  public WebConsoleService(LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getRestapi)
        .orElse(true)) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    return Stream.of(new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(WebConsoleDeployment.name(config))
        .withLabels(labels)
        .withAnnotations(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getRestapi)
            .map(StackGresConfigRestapi::getService)
            .map(StackGresConfigService::getAnnotations)
            .orElse(null))
        .endMetadata()
        .withNewSpec()
        .withType(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getAdminui)
            .map(StackGresConfigAdminui::getService)
            .map(StackGresConfigAdminuiService::getType)
            .orElse("ClusterIP"))
        .withLoadBalancerIP(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getAdminui)
            .map(StackGresConfigAdminui::getService)
            .map(StackGresConfigAdminuiService::getLoadBalancerIP)
            .orElse(null))
        .withLoadBalancerSourceRanges(Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getAdminui)
            .map(StackGresConfigAdminui::getService)
            .map(StackGresConfigAdminuiService::getLoadBalancerSourceRanges)
            .orElse(null))
        .withSelector(labelFactory.restapiLabels(config))
        .withPorts(
            new ServicePortBuilder()
            .withName("https")
            .withProtocol("TCP")
            .withPort(443)
            .withTargetPort(new IntOrString("https"))
            .withNodePort(Optional.of(config.getSpec())
                .map(StackGresConfigSpec::getAdminui)
                .map(StackGresConfigAdminui::getService)
                .map(StackGresConfigAdminuiService::getNodePort)
                .orElse(null))
            .build())
        .addAllToPorts(
            Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getAdminui)
            .map(StackGresConfigAdminui::getService)
            .filter(StackGresConfigAdminuiService::getExposeHttp)
            .map(ignore -> new ServicePortBuilder()
                .withName("http")
                .withProtocol("TCP")
                .withPort(80)
                .withTargetPort(new IntOrString("http"))
                .withNodePort(Optional.of(config.getSpec())
                    .map(StackGresConfigSpec::getAdminui)
                    .map(StackGresConfigAdminui::getService)
                    .map(StackGresConfigAdminuiService::getNodePortHttp)
                    .orElse(null))
                .build())
            .stream()
            .toList())
        .endSpec()
        .build());
  }

}
