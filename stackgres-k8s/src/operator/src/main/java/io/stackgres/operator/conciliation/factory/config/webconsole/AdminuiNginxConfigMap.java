/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.PostgresExporter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class AdminuiNginxConfigMap
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return WebConsoleDeployment.name(config) + "-nginx";
  }

  @Inject
  public AdminuiNginxConfigMap(LabelFactoryForConfig labelFactory) {
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

    final Map<String, String> data = new HashMap<>();
    data.put("start-nginx.sh", Unchecked.supplier(() -> Resources
        .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
            "/webconsole/start-nginx.sh")),
            StandardCharsets.UTF_8)
        .read()).get());
    data.put("nginx.conf", Unchecked.supplier(() -> Resources
        .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
            "/webconsole/nginx.conf")),
            StandardCharsets.UTF_8)
        .read()).get());
    data.put("stackgres-restapi.template", Unchecked.supplier(() -> Resources
        .asCharSource(Objects.requireNonNull(PostgresExporter.class.getResource(
            "/webconsole/stackgres-restapi.template")),
            StandardCharsets.UTF_8)
        .read()).get());

    return Stream.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build());
  }

}
