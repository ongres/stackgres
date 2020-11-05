/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.VolumePath;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public class VolumeConfig {

  private static final BiFunction<StackGresClusterContext, VolumeBuilder, VolumeBuilder>
      UNCHANGED_VOLUME_BUILDER = (context, volumeBuilder) -> volumeBuilder;

  private final String name;
  private final List<VolumePath> paths;
  private final Function<StackGresClusterContext, List<VolumeMount>> volumeMountFactory;
  private final Function<StackGresClusterContext, Optional<Volume>> volumeFactory;

  private VolumeConfig(String name, VolumePath path,
      Function<VolumeConfig, Optional<VolumeBuilder>> volumeFactory,
      Function<StackGresClusterContext, String> getName,
      BiFunction<StackGresClusterContext, VolumeBuilder, VolumeBuilder> volumeBuilderConsumer,
      Predicate<StackGresClusterContext> filter) {
    this.name = name;
    this.paths = ImmutableList.of(path);
    this.volumeMountFactory = context -> Seq.of(Optional.of(context)
        .filter(filter)
        .map(c -> new VolumeMountBuilder()
            .withName(getName.apply(context))
            .withMountPath(path.path(context))
            .build()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    this.volumeFactory = context -> Optional.of(context)
        .filter(filter)
        .flatMap(c -> volumeFactory.apply(this))
        .map(volumeBuilder -> volumeBuilderConsumer.apply(context, volumeBuilder))
        .map(VolumeBuilder::build);
  }

  private VolumeConfig(String name,
      List<VolumePathConfig> paths,
      Function<VolumeConfig, Optional<VolumeBuilder>> volumeFactory,
      Function<StackGresClusterContext, String> getName,
      BiFunction<StackGresClusterContext, VolumeBuilder, VolumeBuilder> volumeBuilderConsumer) {
    Preconditions.checkArgument(paths.size() > 0);
    this.name = name;
    this.paths = Seq.seq(paths).map(VolumePathConfig::volumePath).toList();
    this.volumeMountFactory = context -> Seq.seq(paths)
        .map(volumePathConfig -> Optional.of(context)
            .filter(volumePathConfig.filter())
            .map(c -> new VolumeMountBuilder()
                .withName(getName.apply(context))
                .withSubPath(volumePathConfig.volumePath().subPath(
                    context.getEnvironmentVariables()))
                .withMountPath(volumePathConfig.volumePath().path(
                    context.getEnvironmentVariables())))
            .map(volumePathConfig.volumeMounthOverwrite())
            .map(VolumeMountBuilder::build))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    this.volumeFactory = context -> Optional.of(context)
        .filter(c -> paths.stream().anyMatch(t -> t.v2.test(context)))
        .flatMap(c -> volumeFactory.apply(this))
        .map(volumeBuilder -> volumeBuilderConsumer.apply(context, volumeBuilder))
        .map(VolumeBuilder::build);
  }

  public static class VolumePathConfig
      extends Tuple3<VolumePath,
        Predicate<StackGresClusterContext>,
        Function<VolumeMountBuilder, VolumeMountBuilder>> {

    private static final long serialVersionUID = 1L;

    private VolumePathConfig(Tuple3<VolumePath,
        Predicate<StackGresClusterContext>,
        Function<VolumeMountBuilder, VolumeMountBuilder>> tuple) {
      super(tuple);
    }

    public VolumePath volumePath() {
      return v1;
    }

    public Predicate<StackGresClusterContext> filter() {
      return v2;
    }

    public Function<VolumeMountBuilder, VolumeMountBuilder> volumeMounthOverwrite() {
      return v3;
    }

    public static VolumePathConfig of(VolumePath path) {
      return new VolumePathConfig(Tuple.tuple(
          path, context -> true, volumeMountBuilder -> volumeMountBuilder));
    }

    public static VolumePathConfig of(VolumePath path,
        Predicate<StackGresClusterContext> filter) {
      return new VolumePathConfig(Tuple.tuple(
          path, filter, volumeMountBuilder -> volumeMountBuilder));
    }

    public static VolumePathConfig of(VolumePath path,
        Predicate<StackGresClusterContext> filter,
        Function<VolumeMountBuilder, VolumeMountBuilder> volumeMounthOverwrite) {
      return new VolumePathConfig(Tuple.tuple(
          path, filter, volumeMounthOverwrite));
    }
  }

  public static VolumeConfig persistentVolumeClaim(String name, VolumePath path,
      Function<StackGresClusterContext, String> getName) {
    return new VolumeConfig(name, path, config ->  Optional.empty(), getName,
        UNCHANGED_VOLUME_BUILDER, context -> true);
  }

  public static VolumeConfig inMemoryEmptyDir(String name, VolumePath path) {
    return new VolumeConfig(name, path, VolumeConfig::createInMemoryEmptyDirVolume, context -> name,
        UNCHANGED_VOLUME_BUILDER, context -> true);
  }

  public static VolumeConfig onDiskEmptyDir(String name, VolumePath path) {
    return new VolumeConfig(name, path, VolumeConfig::createOnDiskEmptyDirVolume, context -> name,
        UNCHANGED_VOLUME_BUILDER, context -> true);
  }

  public static VolumeConfig onDiskEmptyDir(String name,
      List<VolumePathConfig> paths) {
    return new VolumeConfig(name, paths, VolumeConfig::createOnDiskEmptyDirVolume, context -> name,
        UNCHANGED_VOLUME_BUILDER);
  }

  public static VolumeConfig configMap(String name, VolumePath path,
      Function<StackGresClusterContext, String> getConfigMapName) {
    return new VolumeConfig(name, path, VolumeConfig::createConfigMapVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.editConfigMap()
        .withName(getConfigMapName.apply(context)).endConfigMap(),
        context -> true);
  }

  public static VolumeConfig configMap(String name, VolumePath path,
      Function<StackGresClusterContext, String> getConfigMapName,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, VolumeConfig::createConfigMapVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.editConfigMap()
        .withName(getConfigMapName.apply(context)).endConfigMap(),
        filter);
  }

  public static VolumeConfig secret(String name, VolumePath path,
      Function<StackGresClusterContext, String> getSecretName) {
    return new VolumeConfig(name, path, VolumeConfig::createSecretVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.editSecret()
        .withSecretName(getSecretName.apply(context)).endSecret(),
        context -> true);
  }

  public static VolumeConfig secret(String name, VolumePath path,
      Function<StackGresClusterContext, String> getSecretName,
      Predicate<StackGresClusterContext> filter) {
    return new VolumeConfig(name, path, VolumeConfig::createSecretVolume, context -> name,
        (context, volumeBuilder) -> volumeBuilder.editSecret()
        .withSecretName(getSecretName.apply(context)).endSecret(),
        filter);
  }

  private static Optional<VolumeBuilder> createInMemoryEmptyDirVolume(VolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.name)
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir());
  }

  private static Optional<VolumeBuilder> createOnDiskEmptyDirVolume(VolumeConfig config) {
    return Optional.of(new VolumeBuilder()
        .withName(config.name)
        .withNewEmptyDir()
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

  public String path(Map<String, String> envVars) {
    return paths.get(0).path(envVars);
  }

  public List<VolumePath> paths() {
    return paths;
  }

  public Optional<VolumeMount> volumeMount(VolumePath path, StackGresClusterContext context) {
    return volumeMountFactory.apply(context).stream()
        .filter(volumeMount -> paths.contains(path))
        .skip(paths.indexOf(path))
        .findFirst();
  }

  public List<VolumeMount> volumeMounts(StackGresClusterContext context) {
    return volumeMountFactory.apply(context);
  }

  public List<VolumeMount> volumeMounts(StackGresClusterContext context, String subPath) {
    return volumeMountFactory.apply(context);
  }

  public Optional<Volume> volume(StackGresClusterContext context) {
    return volumeFactory.apply(context);
  }

}
