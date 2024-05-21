/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;
import static io.stackgres.common.StreamUtil.jobName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StreamPath;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloper;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperContainerPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamPods;
import io.stackgres.common.crd.sgstream.StackGresStreamPodsScheduling;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class StreamDeploymentOrJob implements ResourceGenerator<StackGresStreamContext>, StreamDeploymentOrJobFactory {

  private final LabelFactoryForStream labelFactory;
  private final ResourceFactory<StackGresStreamContext, PodSecurityContext> podSecurityFactory;

  @Inject
  public StreamDeploymentOrJob(
      LabelFactoryForStream streamLabelFactory,
      ResourceFactory<StackGresStreamContext, PodSecurityContext> podSecurityFactory) {
    this.labelFactory = streamLabelFactory;
    this.podSecurityFactory = podSecurityFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresStreamContext config) {
    return Seq.of(config.getSource())
        .filter(stream -> !StreamUtil.isAlreadyCompleted(stream))
        .map(stream -> {
          return createDeploymentOrJob(config);
        });
  }

  private HasMetadata createDeploymentOrJob(StackGresStreamContext context) {
    StackGresStream stream = context.getSource();
    String namespace = stream.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.streamPodLabels(context.getSource());
    final Integer maxRetries = Optional.of(stream)
        .map(StackGresStream::getSpec)
        .map(StackGresStreamSpec::getMaxRetries)
        .orElse(-1);
    PodTemplateSpec podTemplateSpec = new PodTemplateSpecBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(stream))
        .withLabels(labelFactory.streamPodLabels(stream))
        .endMetadata()
        .withNewSpec()
        .withSecurityContext(podSecurityFactory.createResource(context))
        .withRestartPolicy(maxRetries < 0 ? "Always" : "Never")
        .withServiceAccountName(StreamRole.roleName(context))
        .withNodeSelector(Optional.ofNullable(stream)
            .map(StackGresStream::getSpec)
            .map(StackGresStreamSpec::getPods)
            .map(StackGresStreamPods::getScheduling)
            .map(StackGresStreamPodsScheduling::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(stream)
            .map(StackGresStream::getSpec)
            .map(StackGresStreamSpec::getPods)
            .map(StackGresStreamPods::getScheduling)
            .map(StackGresStreamPodsScheduling::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withAffinity(new AffinityBuilder()
            .withNodeAffinity(Optional.of(stream)
                .map(StackGresStream::getSpec)
                .map(StackGresStreamSpec::getPods)
                .map(StackGresStreamPods::getScheduling)
                .map(StackGresStreamPodsScheduling::getNodeAffinity)
                .orElse(null))
            .withPodAffinity(Optional.of(stream)
                .map(StackGresStream::getSpec)
                .map(StackGresStreamSpec::getPods)
                .map(StackGresStreamPods::getScheduling)
                .map(StackGresStreamPodsScheduling::getPodAffinity)
                .orElse(null))
            .withPodAntiAffinity(Optional.of(stream)
                .map(StackGresStream::getSpec)
                .map(StackGresStreamSpec::getPods)
                .map(StackGresStreamPods::getScheduling)
                .map(StackGresStreamPodsScheduling::getPodAntiAffinity)
                .orElse(null))
            .build())
        .withContainers(new ContainerBuilder()
            .withName("stream")
            .withImagePullPolicy(getDefaultPullPolicy())
            .withImage(getImageName())
            .withResources(Optional.of(stream)
                .map(StackGresStream::getSpec)
                .map(StackGresStreamSpec::getPods)
                .map(StackGresStreamPods::getResources)
                .orElse(null))
            .addToEnv(
                new EnvVarBuilder()
                    .withName(OperatorProperty.OPERATOR_NAME.getEnvironmentVariableName())
                    .withValue(OperatorProperty.OPERATOR_NAME.getString())
                    .build(),
                new EnvVarBuilder()
                    .withName(OperatorProperty.OPERATOR_NAMESPACE.getEnvironmentVariableName())
                    .withValue(OperatorProperty.OPERATOR_NAMESPACE.getString())
                    .build(),
                new EnvVarBuilder()
                    .withName("JOB_NAMESPACE")
                    .withValue(namespace)
                    .build(),
                new EnvVarBuilder()
                    .withName(StackGresProperty.OPERATOR_VERSION.getEnvironmentVariableName())
                    .withValue(StackGresProperty.OPERATOR_VERSION.getString())
                    .build(),
                new EnvVarBuilder()
                    .withName("CRD_UPGRADE")
                    .withValue(Boolean.FALSE.toString())
                    .build(),
                new EnvVarBuilder()
                    .withName("CONVERSION_WEBHOOKS")
                    .withValue(Boolean.FALSE.toString())
                    .build(),
                new EnvVarBuilder()
                    .withName("STREAM_NAMESPACE")
                    .withValue(stream.getMetadata().getNamespace())
                    .build(),
                new EnvVarBuilder()
                    .withName("STREAM_NAME")
                    .withValue(stream.getMetadata().getName())
                    .build(),
                new EnvVarBuilder()
                    .withName("SERVICE_ACCOUNT")
                    .withNewValueFrom()
                    .withNewFieldRef()
                    .withFieldPath("spec.serviceAccountName")
                    .endFieldRef()
                    .endValueFrom()
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withNewValueFrom()
                    .withNewFieldRef()
                    .withFieldPath("metadata.name")
                    .endFieldRef()
                    .endValueFrom()
                    .build(),
                new EnvVarBuilder()
                    .withName("APP_OPTS")
                    .withValue(System.getenv("APP_OPTS"))
                    .build(),
                new EnvVarBuilder()
                    .withName("JAVA_OPTS")
                    .withValue(System.getenv("JAVA_OPTS"))
                    .build(),
                new EnvVarBuilder()
                    .withName("DEBUG_STREAM")
                    .withValue(System.getenv("DEBUG_OPERATOR"))
                    .build(),
                new EnvVarBuilder()
                    .withName("DEBUG_STREAM_SUSPEND")
                    .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
                    .build(),
                new EnvVarBuilder()
                    .withName("STREAM_LOCK_DURATION")
                    .withValue(OperatorProperty.LOCK_DURATION.getString())
                    .build(),
                new EnvVarBuilder()
                    .withName("STREAM_LOCK_POLL_INTERVAL")
                    .withValue(OperatorProperty.LOCK_POLL_INTERVAL.getString())
                    .build(),
                new EnvVarBuilder()
                    .withName("LOCK_SERVICE_ACCOUNT_KEY")
                    .withValue(StackGresContext.LOCK_SERVICE_ACCOUNT_KEY)
                    .build(),
                new EnvVarBuilder()
                    .withName("LOCK_POD_KEY")
                    .withValue(StackGresContext.LOCK_POD_KEY)
                    .build(),
                new EnvVarBuilder()
                    .withName("LOCK_TIMEOUT_KEY")
                    .withValue(StackGresContext.LOCK_TIMEOUT_KEY)
                    .build())
            .addToVolumeMounts(
                new VolumeMountBuilder()
                .withName("stream")
                .withMountPath(StreamPath.DEBEZIUM_BASE_PATH.path())
                .build())
            .addAllToVolumeMounts(Optional.of(context.getConfig().getSpec())
                .map(StackGresConfigSpec::getDeveloper)
                .map(StackGresConfigDeveloper::getPatches)
                .map(StackGresConfigDeveloperPatches::getStream)
                .map(StackGresConfigDeveloperContainerPatches::getVolumeMounts)
                .stream()
                .flatMap(List::stream)
                .map(VolumeMount.class::cast)
                .toList())
            .build())
        .addToVolumes(
            new VolumeBuilder()
            .withName("stream")
            .withNewPersistentVolumeClaim()
            .withClaimName(StreamPersistentVolumeClaim.name(context))
            .endPersistentVolumeClaim()
            .build())
        .addAllToVolumes(Seq.seq(
            Optional.of(context.getConfig().getSpec())
            .map(StackGresConfigSpec::getDeveloper)
            .map(StackGresConfigDeveloper::getPatches)
            .map(StackGresConfigDeveloperPatches::getStream)
            .map(StackGresConfigDeveloperContainerPatches::getVolumes)
            .stream()
            .flatMap(List::stream)
            .map(Volume.class::cast))
            .grouped(volume -> volume.getName())
            .flatMap(t -> t.v2.limit(1))
            .toList())
        .endSpec()
        .build();
    if (maxRetries < 0) {
      return new DeploymentBuilder()
          .withNewMetadata()
          .withNamespace(namespace)
          .withName(jobName(stream))
          .withLabels(labels)
          .endMetadata()
          .withNewSpec()
          .withReplicas(1)
          .withNewSelector()
          .withMatchLabels(labelFactory.streamPodLabels(stream))
          .endSelector()
          .withTemplate(podTemplateSpec)
          .endSpec()
          .build();
    }
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(stream))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(maxRetries)
        .withParallelism(1)
        .withTemplate(podTemplateSpec)
        .endSpec()
        .build();
  }

}
