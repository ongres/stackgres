/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminui;
import io.stackgres.common.crd.sgconfig.StackGresConfigAdminuiService;
import io.stackgres.common.crd.sgconfig.StackGresConfigCert;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
  public @Nonnull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getRestapi)
        .orElse(true)
        || !Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCreateForWebApi)
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
