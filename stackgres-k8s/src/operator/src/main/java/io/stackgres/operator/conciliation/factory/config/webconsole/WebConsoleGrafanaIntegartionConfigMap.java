/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCert;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.PostgresExporter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class WebConsoleGrafanaIntegartionConfigMap
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        config.getSpec().getCert() != null
        && config.getSpec().getCert().getWebSecretName() != null
        ? config.getSpec().getCert().getWebSecretName()
            : WebConsoleDeployment.name(config) + "-grafana-integration");
  }

  @Inject
  public WebConsoleGrafanaIntegartionConfigMap(LabelFactoryForConfig labelFactory) {
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

    final Map<String, String> data = new HashMap<>();
    data.put("integrate-grafana.sh", Unchecked.supplier(() -> Resources
        .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
            "/webconsole/integrate-grafana.sh")),
            StandardCharsets.UTF_8)
        .read()).get());
    getDashboards().forEach(dashboardFile -> data.put(
            dashboardFile, Unchecked.supplier(() -> Resources
                .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
                    "/webconsole/grafana-dashboard/" + dashboardFile)),
                    StandardCharsets.UTF_8)
                .read()).get()));

    return Stream.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build());
  }

  public static List<String> getDashboards() {
    return Unchecked.supplier(() -> Resources
        .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
            "/webconsole/grafana-dashboard/index.txt")),
            StandardCharsets.UTF_8)
        .read()).get()
        .lines()
        .filter(Predicate.not(String::isBlank))
        .toList();
  }

}
