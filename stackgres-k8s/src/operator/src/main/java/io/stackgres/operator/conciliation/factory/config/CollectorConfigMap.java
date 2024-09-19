/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config;

import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.labels.LabelFactory;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class CollectorConfigMap extends AbstractTemplatesConfigMap
    implements VolumeFactory<StackGresConfigContext> {

  private LabelFactory<StackGresConfig> labelFactory;

  public static String name(ConfigContext context) {
    final String configName = context.getConfig().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(configName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresConfigContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  public @NotNull Volume buildVolume(StackGresConfigContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresConfigContext context) {
    Map<String, String> data = getConfigTemplates();

    final StackGresConfig config = context.getSource();
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(config.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(config))
        .endMetadata()
        .withData(data)
        .build();
  }

  public Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    Map<String, String> data = getConfigTemplates();

    final StackGresConfig config = context.getSource();
    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(config.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(config))
        .endMetadata()
        .withData(data)
        .build();
    return Stream.of(configMap);
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresConfig> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
