/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import org.jetbrains.annotations.NotNull;

public abstract class AnnotationDecorator<T> implements Decorator<T> {

  protected abstract @NotNull Map<String, String> getAllResourcesAnnotations(@NotNull T cluster);

  protected abstract @NotNull Map<String, String> getServiceAnnotations(@NotNull T cluster);

  protected abstract @NotNull Map<String, String> getPodAnnotations(@NotNull T cluster);

  protected @NotNull Map<String, BiConsumer<T, HasMetadata>> getCustomDecorators() {
    return Map.of(
        "Service", this::decorateService,
        "Pod", this::decoratePod,
        "StatefulSet", this::decorateSts);
  }

  protected void decorateService(@NotNull T cluster, @NotNull HasMetadata service) {

    decorateResource(service, getServiceAnnotations(cluster));

  }

  // TODO review if we need this
  protected void decoratePod(@NotNull T cluster, @NotNull HasMetadata pod) {

    decorateResource(pod, getPodAnnotations(cluster));

  }

  protected void decorateSts(@NotNull T cluster, @NotNull HasMetadata resource) {

    StatefulSet sts = (StatefulSet) resource;

    Map<String, String> podTemplateAnnotations = Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    Map<String, String> podAnnotations = getPodAnnotations(cluster);
    podTemplateAnnotations.putAll(podAnnotations);

    Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .ifPresent(template -> {
          final ObjectMeta metadata = Optional
              .ofNullable(template.getMetadata())
              .orElse(new ObjectMeta());
          metadata.setAnnotations(podTemplateAnnotations);
          template.setMetadata(metadata);
        });

    Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getVolumeClaimTemplates)
        .ifPresent(cvt -> decorate(cluster, cvt));

    decorateResource(resource, getAllResourcesAnnotations(cluster));

  }

  protected void decorateResource(@NotNull HasMetadata resource,
      @NotNull Map<String, String> customAnnotations) {

    var metadata = Objects.requireNonNull(resource.getMetadata());

    Map<String, String> resourceAnnotations = Optional.of(metadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    resourceAnnotations.putAll(customAnnotations);

    resource.getMetadata().setAnnotations(resourceAnnotations);
  }

  protected void defaultDecorator(@NotNull T cluster, @NotNull HasMetadata resources) {

    decorateResource(resources, getAllResourcesAnnotations(cluster));

  }

  @Override
  public void decorate(T cluster, Iterable<? extends HasMetadata> resources) {

    var decoratorMap = getCustomDecorators();

    resources.forEach(resource -> {
      String kind = resource.getKind();
      var decorator = decoratorMap.getOrDefault(kind, this::defaultDecorator);
      decorator.accept(cluster, resource);
    });

  }
}
