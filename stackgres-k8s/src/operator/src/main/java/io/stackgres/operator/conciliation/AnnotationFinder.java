/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.lang.annotation.Annotation;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.arc.ClientProxy;

public interface AnnotationFinder {

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  default <A extends Annotation> A getAnnotation(
      Object targetInstance,
      Class<A> targetAnnotationClass) {
    final Class<?> targetClass;
    if (targetInstance instanceof ClientProxy targetClientProxy) {
      targetInstance = targetClientProxy.arc_contextualInstance();
      targetClass = targetInstance.getClass();
    } else {
      targetClass = targetInstance.getClass();
    }
    return findAnnotation(targetInstance, targetAnnotationClass)
        .orElseThrow(() -> new RuntimeException(
            "Class " + targetClass.getName()
            + " or any of its superclasses are not annotated with "
            + targetAnnotationClass.getName()));
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  default <A extends Annotation> Optional<A> findAnnotation(
      Object targetInstance,
      Class<A> targetAnnotationClass) {
    if (targetInstance instanceof ClientProxy targetClientProxy) {
      targetInstance = targetClientProxy.arc_contextualInstance();
    }
    Class<?> currentClass = targetInstance.getClass();
    while (currentClass != Object.class) {
      Optional<A> foundAnnotation = Optional.ofNullable(
          currentClass.getAnnotation(targetAnnotationClass));
      if (foundAnnotation.isPresent()) {
        return foundAnnotation;
      }
      currentClass = currentClass.getSuperclass();
    }
    return Optional.empty();
  }

}
