/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class VolumeConfig {

  private static final BiConsumer<StackGresClusterContext, VolumeBuilder> UNCHANGED_VOLUME_BUILDER =
      (context, volumeBuilder) -> { };

  private final String name;
  private final VolumePath path;
  private final Function<StackGresClusterContext, Optional<VolumeMount>> volumeMountFactory;
  private final Function<StackGresClusterContext, Optional<Volume>> volumeFactory;

  private VolumeConfig(String name, VolumePath path,
      Function<VolumeConfig, Optional<VolumeBuilder>> volumeFactory,
      Function<StackGresClusterContext, String> getName,
      BiConsumer<StackGresClusterContext, VolumeBuilder> volumeBuilderConsumer,
      Predicate<StackGresClusterContext> filter) {
    this.name = name;
    this.path = path;
    this.volumeMountFactory = context -> Optional.of(context)
        .filter(filter)
        .map(c -> new VolumeMountBuilder()
            .withName(getName.apply(context))
            .withMountPath(path.path())
            .build());
    this.volumeFactory = context -> Optional.of(context)
        .filter(filter)
        .flatMap(c -> volumeFactory.apply(this))
        .map(volumeBuilder -> {
          volumeBuilderConsumer.accept(context, volumeBuilder);
          return volumeBuilder;
        })
        .map(VolumeBuilder::build);
  }

  public static VolumeConfig persistentVolumeClaim(String name, VolumePath path,
      Function<StackGresClusterContext, String> getName) {
    return new VolumeConfig(name, path, config ->  Optional.empty(), getName,
        UNCHANGED_VOLUME_BUILDER, context -> true);
  }

  public static VolumeConfig persistentVolumeClaim(String name, VolumePath path,
      Function<StackGresClusterContext, String> getName,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, config ->  Optional.empty(), context -> name,
        UNCHANGED_VOLUME_BUILDER, filter);
  }

  public static VolumeConfig emptyDir(String name, VolumePath path) {
    return new VolumeConfig(name, path, VolumeConfig::createEmptyDirVolume, context -> name,
        UNCHANGED_VOLUME_BUILDER, context -> true);
  }

  public static VolumeConfig emptyDir(String name, VolumePath path,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, VolumeConfig::createEmptyDirVolume, context -> name,
        UNCHANGED_VOLUME_BUILDER, filter);
  }

  public static VolumeConfig configMap(String name, VolumePath path,
      Function<StackGresClusterContext, String> getConfigMapName) {
    return new VolumeConfig(name, path, VolumeConfig::createConfigMapVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.buildConfigMap()
        .setName(getConfigMapName.apply(context)), context -> true);
  }

  public static VolumeConfig configMap(String name, VolumePath path,
      Function<StackGresClusterContext, String> getConfigMapName,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, VolumeConfig::createConfigMapVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.buildConfigMap()
        .setName(getConfigMapName.apply(context)), filter);
  }

  public static VolumeConfig secret(String name, VolumePath path,
      Function<StackGresClusterContext, String> getSecretName) {
    return new VolumeConfig(name, path, VolumeConfig::createSecretVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.buildSecret()
        .setSecretName(getSecretName.apply(context)), context -> true);
  }

  public static VolumeConfig secret(String name, VolumePath path,
      Function<StackGresClusterContext, String> getSecretName,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, VolumeConfig::createSecretVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.buildSecret()
        .setSecretName(getSecretName.apply(context)), filter);
  }

  private static Optional<VolumeBuilder> createEmptyDirVolume(VolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.name)
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir());
  }

  private static Optional<VolumeBuilder> createConfigMapVolume(VolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.name)
        .withNewConfigMap()
        .withDefaultMode(444)
        .endConfigMap());
  }

  private static Optional<VolumeBuilder> createSecretVolume(VolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.name)
        .withNewSecret()
        .withDefaultMode(444)
        .endSecret());
  }

  public String name() {
    return name;
  }

  public String path() {
    return path.path();
  }

  public Optional<VolumeMount> volumeMount(StackGresClusterContext context) {
    return volumeMountFactory.apply(context);
  }

  public Optional<Volume> volume(StackGresClusterContext context) {
    return volumeFactory.apply(context);
  }

}
