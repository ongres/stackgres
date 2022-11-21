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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import org.jetbrains.annotations.NotNull;

public abstract class AnnotationDecorator<T> implements Decorator<T> {

  protected abstract @NotNull Map<String, String> getAllResourcesAnnotations(@NotNull T context);

  protected abstract @NotNull Map<String, String> getServiceAnnotations(@NotNull T context);

  protected abstract @NotNull Map<String, String> getPodAnnotations(@NotNull T context);

  protected @NotNull Map<Class<?>, BiConsumer<T, HasMetadata>> getCustomDecorators() {
    return Map.of(
        Service.class, this::decorateService,
        Pod.class, this::decoratePod,
        StatefulSet.class, this::decorateSts);
  }

  protected void decorateService(@NotNull T context, @NotNull HasMetadata service) {
    decorateResource(service, getServiceAnnotations(context));
  }

  // TODO review if we need this
  protected void decoratePod(@NotNull T context, @NotNull HasMetadata pod) {
    decorateResource(pod, getPodAnnotations(context));
  }

  protected void decorateSts(@NotNull T context, @NotNull HasMetadata resource) {
    StatefulSet sts = (StatefulSet) resource;

    Map<String, String> podTemplateAnnotations = Optional.ofNullable(sts.getSpec())
        .map(StatefulSetSpec::getTemplate)
        .map(PodTemplateSpec::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .orElse(new HashMap<>());

    Map<String, String> podAnnotations = getPodAnnotations(context);
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
        .ifPresent(cvt -> decorate(context, cvt));

    decorateResource(resource, getAllResourcesAnnotations(context));
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

  protected void defaultDecorator(@NotNull T context, @NotNull HasMetadata resources) {
    decorateResource(resources, getAllResourcesAnnotations(context));
  }

  @Override
  public void decorate(T context, Iterable<? extends HasMetadata> resources) {
    var decoratorMap = getCustomDecorators();

    resources.forEach(resource -> {
      var decorator = decoratorMap.getOrDefault(resource.getClass(), this::defaultDecorator);
      decorator.accept(context, resource);
    });
  }

}
