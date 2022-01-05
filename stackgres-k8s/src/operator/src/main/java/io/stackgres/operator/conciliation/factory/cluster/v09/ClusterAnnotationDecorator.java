/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.v09;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.AbstractClusterAnnotationDecorator;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class ClusterAnnotationDecorator extends AbstractClusterAnnotationDecorator {

  @Override
  protected void decorateSts(@NotNull StackGresClusterContext context,
      @NotNull HasMetadata resource) {
    StatefulSet statefulSet = (StatefulSet) resource;

    var allResourceAnnotations = getAllResourcesAnnotations(context);

    var stsAnnotations = Optional
        .ofNullable(statefulSet.getMetadata().getAnnotations())
        .orElse(new HashMap<>());

    stsAnnotations.putAll(allResourceAnnotations);
    resource.getMetadata().setAnnotations(stsAnnotations);

    Map<String, String> podTemplateAnnotations = Optional.ofNullable(statefulSet.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    podTemplateAnnotations.putAll(getPodAnnotations(context));
    Optional.ofNullable(statefulSet.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(podTemplateAnnotations);
          template.setMetadata(metadata);
        });

    Optional.ofNullable(statefulSet.getSpec())
        .map(StatefulSetSpec::getVolumeClaimTemplates)
        .ifPresent(cvt -> decorate(context, cvt));
  }

}
