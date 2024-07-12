/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamPodsPersistentVolume;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class StreamPersistentVolumeClaim
    implements ResourceGenerator<StackGresStreamContext> {

  private final LabelFactoryForStream streamLabelFactory;

  public static String name(StackGresStreamContext context) {
    return name(context.getSource());
  }

  public static String name(StackGresStream stream) {
    return ResourceUtil.resourceName(stream.getMetadata().getName());
  }

  @Inject
  public StreamPersistentVolumeClaim(
      LabelFactoryForStream streamLabelFactory) {
    this.streamLabelFactory = streamLabelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresStreamContext config) {
    return Seq.of(config.getSource())
        .filter(stream -> !StreamUtil.isAlreadyCompleted(stream))
        .map(stream -> {
          return createPersistentVolumeClaim(config);
        });
  }

  private PersistentVolumeClaim createPersistentVolumeClaim(StackGresStreamContext context) {
    StackGresStream stream = context.getSource();

    final StackGresStreamPodsPersistentVolume persistentVolume = stream
        .getSpec().getPods().getPersistentVolume();

    final StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(persistentVolume.getSize())
        .storageClass(Optional.ofNullable(
            persistentVolume.getStorageClass())
            .orElse(null))
        .build();

    String namespace = stream.getMetadata().getNamespace();
    final Map<String, String> labels = streamLabelFactory.genericLabels(context.getSource());
    return new PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(stream))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass())
        .endSpec()
        .build();
  }

}
