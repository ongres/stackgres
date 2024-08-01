/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.stackgres.common.StackGresModules;
import io.stackgres.common.StreamUtil;
import io.stackgres.common.crd.external.knative.Service;
import io.stackgres.common.crd.external.knative.ServiceBuilder;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetPgLambda;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetPgLambdaKnative;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetPgLambdaScriptFrom;
import io.stackgres.common.crd.sgstream.StreamTargetPgLambdaScriptType;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class StreamPgLamdaService implements ResourceGenerator<StackGresStreamContext> {

  private LabelFactoryForStream labelFactory;

  public static String name(StackGresStreamContext context) {
    return StreamUtil.pglambdaServiceName(context.getSource());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresStreamContext context) {
    return Optional.ofNullable(context.getSource().getSpec().getTarget().getPgLambda())
        .map(ignored -> createService(context))
        .map(HasMetadata.class::cast)
        .stream();
  }

  private Service createService(StackGresStreamContext context) {
    final StackGresStream stream = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());

    final String serviceName = name(context);
    final String serviceNamespace = stream.getMetadata().getNamespace();
    return new ServiceBuilder()
        .withNewMetadata()
        .withName(serviceName)
        .withNamespace(serviceNamespace)
        .withLabels(labels)
        .addToLabels(Optional.of(stream.getSpec().getTarget().getPgLambda())
            .map(StackGresStreamTargetPgLambda::getKnative)
            .map(StackGresStreamTargetPgLambdaKnative::getLabels)
            .orElse(Map.of()))
        .withAnnotations(Optional.of(stream.getSpec().getTarget().getPgLambda())
            .map(StackGresStreamTargetPgLambda::getKnative)
            .map(StackGresStreamTargetPgLambdaKnative::getAnnotations)
            .orElse(Map.of()))
        .endMetadata()
        .withNewSpec()
        .withTemplate(new PodTemplateSpecBuilder()
            .withNewMetadata()
            .withLabels(labelFactory.genericLabels(stream))
            .endMetadata()
            .withNewSpec()
            .withContainers(new ContainerBuilder()
                .withName("pglambda")
                .withImage(StackGresModules.PGLAMBDA.getImageName(
                    Optional.ofNullable(stream.getSpec().getTarget().getPgLambda().getScriptType())
                    .orElse(StreamTargetPgLambdaScriptType.JAVASCRIPT.toString())))
                .withImagePullPolicy(getDefaultPullPolicy())
                .withEnv(Optional.ofNullable(stream.getSpec().getTarget().getPgLambda().getScript())
                    .map(script -> new EnvVarBuilder()
                        .withName("SCRIPT")
                        .withValue(script.replace("$", "$$"))
                        .build())
                    .or(() -> Optional.ofNullable(stream.getSpec().getTarget().getPgLambda().getScriptFrom())
                        .map(StackGresStreamTargetPgLambdaScriptFrom::getSecretKeyRef)
                        .map(scriptFromSecretKeyRef -> new EnvVarBuilder()
                                .withName("SCRIPT")
                                .withNewValueFrom()
                                .withNewSecretKeyRef()
                                .withName(scriptFromSecretKeyRef.getName())
                                .withKey(scriptFromSecretKeyRef.getKey())
                                .withOptional(false)
                                .endSecretKeyRef()
                                .endValueFrom()
                                .build()))
                    .or(() -> Optional.ofNullable(stream.getSpec().getTarget().getPgLambda().getScriptFrom())
                        .map(StackGresStreamTargetPgLambdaScriptFrom::getConfigMapKeyRef)
                        .map(stringFromConfigMapKeyRef -> new EnvVarBuilder()
                            .withName("SCRIPT")
                            .withNewValueFrom()
                            .withNewConfigMapKeyRef()
                            .withName(stringFromConfigMapKeyRef.getName())
                            .withKey(stringFromConfigMapKeyRef.getKey())
                            .withOptional(false)
                            .endConfigMapKeyRef()
                            .endValueFrom()
                            .build()))
                    .orElseThrow(() -> new IllegalArgumentException("No script or scriptFrom section specified")))
                .build())
            .endSpec()
            .build())
        .endSpec()
        .build();

  }

  @Inject
  public void setLabelFactory(LabelFactoryForStream labelFactory) {
    this.labelFactory = labelFactory;
  }

}
