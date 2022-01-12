/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.v09;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple4;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class PatroniScriptsConfigMap implements
    VolumeFactory<StackGresClusterContext> {

  public static final String INTERNAL_SCRIPT = "INTERNAL_SCRIPT";
  public static final String SCRIPT_BASIC_NAME = "%05d";
  public static final String SCRIPT_BASIC_NAME_FOR_DATABASE = "%05d.%s";
  public static final String SCRIPT_NAME = "%05d-%s";
  public static final String SCRIPT_NAME_FOR_DATABASE = "%05d-%s.%s";

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PatroniScriptsConfigMap(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresClusterContext clusterContext,
                            Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return ResourceUtil.cutVolumeName(
        ResourceUtil.resourceName(clusterContext.getSource().getMetadata().getName()
            + "-" + normalizedResourceName(indexedScript)));
  }

  public static String scriptName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return normalizedKeyName(indexedScript) + ".sql";
  }

  private static String normalizedResourceName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return (INTERNAL_SCRIPT.equals(indexedScript.v3)
        ? "internal-" + baseName(indexedScript.v1, indexedScript.v2)
        : baseName(indexedScript.v1, indexedScript.v4))
        .toLowerCase(Locale.US).replaceAll("[^a-z0-9-]", "-");
  }

  private static String normalizedKeyName(
      Tuple4<StackGresClusterScriptEntry, Long, String, Long> indexedScript) {
    return baseName(indexedScript.v1, indexedScript.v4)
        .toLowerCase(Locale.US).replaceAll("[^a-zA-Z0-9-_.]", "-");
  }

  private static String baseName(StackGresClusterScriptEntry script, Long index) {
    if (script.getName() == null) {
      if (script.getDatabase() == null) {
        return String.format(SCRIPT_BASIC_NAME, index);
      }
      return String.format(SCRIPT_BASIC_NAME_FOR_DATABASE,
          index, script.getDatabase());
    }
    if (script.getDatabase() == null) {
      return String.format(SCRIPT_NAME, index, script.getName());
    }
    return String.format(SCRIPT_NAME_FOR_DATABASE,
        index, script.getName(), script.getDatabase());
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();
    var indexedScripts = context.getIndexedScripts();

    List<VolumePair> inlineScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withName(name(context, t))
                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                    .withName(name(context, t))
                    .withOptional(false)
                    .withDefaultMode(420)
                    .build()
                )
                .build()
            ).source(
                new ConfigMapBuilder()
                    .withNewMetadata()
                    .withNamespace(cluster.getMetadata().getNamespace())
                    .withName(name(context, t))
                    .withLabels(labelFactory.patroniClusterLabels(cluster))
                    .endMetadata()
                    .withData(ImmutableMap.of(scriptName(t), t.v1.getScript()))
                    .build()
            ).build()
        ).collect(Collectors.toUnmodifiableList());

    List<VolumePair> configMapScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withName(name(context, t))
                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                    .withName(t.v1.getScriptFrom().getConfigMapKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build())
            .build()
        ).collect(Collectors.toUnmodifiableList());

    List<VolumePair> secretScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getSecretKeyRef() != null)
        .map(t -> ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withName(name(context, t))
                .withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(t.v1.getScriptFrom().getSecretKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build()).build())
        .collect(Collectors.toUnmodifiableList());

    return ImmutableList.<VolumePair>builder()
        .addAll(inlineScripts)
        .addAll(configMapScripts)
        .addAll(secretScripts)
        .build().stream();
  }

  public @NotNull Stream<Volume> buildVolume(StackGresClusterContext context) {
    var indexedScripts = context.getIndexedScripts();
    var inlineScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> new VolumeBuilder()
            .withName(name(context, t))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(name(context, t))
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());
    var configMapScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> new VolumeBuilder()
            .withName(name(context, t))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(t.v1.getScriptFrom().getConfigMapKeyRef().getName())
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());

    var secretScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getSecretKeyRef() != null)
        .map(t -> new VolumeBuilder()
            .withName(name(context, t))
            .withSecret(new SecretVolumeSourceBuilder()
                .withSecretName(t.v1.getScriptFrom().getSecretKeyRef().getName())
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());
    return ImmutableList.<Volume>builder()
        .addAll(inlineScripts)
        .addAll(configMapScripts)
        .addAll(secretScripts)
        .build().stream();
  }

}
