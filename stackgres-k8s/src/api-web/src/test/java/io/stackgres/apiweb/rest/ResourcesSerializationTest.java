/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResourcesSerializationTest {

  private static final ClassLoader LOADER = Thread.currentThread().getContextClassLoader();

  private static ClassPath CLASSPATH_SCANNER;

  @BeforeAll
  static void setUp() throws IOException {
    CLASSPATH_SCANNER = ClassPath.from(LOADER);
  }

  @Test
  void returnTypesOfRestResponses_mustBeAnnotatedWithRegisterForReflection() {

    getRestMethods()
        .filter(t -> !t.v2.getReturnType().equals(Void.TYPE))
        .forEach(t -> {
          Class<?> returnType =
              TypeToken.of(t.v1).resolveType(t.v2.getGenericReturnType()).getRawType();
          if (returnType.getPackage().getName().startsWith("io.stackgres.")) {
            assertNotNull(returnType.getAnnotation(RegisterForReflection.class), "class "
                + returnType.getName() + " must be annotated with register for reflection");
          }

        });
  }

  @Test
  void parametersOfGenericReturnTypesOfRestResponses_mustBeAnnotatedWithRegisterForReflection() {

    getRestMethods()
        .filter(t -> !t.v2.getReturnType().equals(Void.TYPE))
        .forEach(t -> {
          Class<?> returnType = t.v2.getReturnType();

          if (Collection.class.isAssignableFrom(returnType)) {
            Arrays
                .stream(((ParameterizedType) t.v2.getGenericReturnType()).getActualTypeArguments())
                .forEach(gt -> {
                  if (gt instanceof Class
                      && ((Class<?>) gt).getName().startsWith("io.stackgres.")) {
                    Class<?> type = (Class<?>) gt;
                    assertNotNull(type.getAnnotation(RegisterForReflection.class), "class "
                        + type.getName() + " must be annotated with register for reflection");
                  }
                });
          }

        });
  }

  @Test
  void parametersOfRestRequests_mustBeAnnotatedWithRegisterForReflection() {
    getRestMethods()
        .forEach(t -> {
          Arrays.stream(t.v2.getGenericParameterTypes())
              .map(p -> TypeToken.of(t.v1).resolveType(p).getRawType())
              .filter(p -> p.getName().startsWith("io.stackgres."))
              .forEach(p -> {
                assertNotNull(p.getAnnotation(RegisterForReflection.class), "class "
                    + p.getName() + " must be annotated with register for reflection");
              });
        });
  }

  static Stream<Tuple2<Class<?>, Method>> getRestMethods() {
    return getClassesInStackGres()
        .filter(classInfo -> {
          final Class<?> clazz = classInfo.load();
          return clazz.getAnnotation(Path.class) != null;
        })
        .filter(classInfo -> {
          final Class<?> clazz = classInfo.load();
          return clazz.getAnnotation(Produces.class) != null;
        })
        .flatMap(classInfo -> {
          final Class<?> clazz = classInfo.load();
          return Arrays.stream(clazz.getMethods())
              .map(method -> Tuple.<Class<?>, Method>tuple(clazz, method))
              .filter(t -> t.v2.getAnnotation(POST.class) != null
                  || t.v2.getAnnotation(GET.class) != null
                  || t.v2.getAnnotation(PUT.class) != null
                  || t.v2.getAnnotation(DELETE.class) != null
                  || t.v2.getAnnotation(OPTIONS.class) != null
                  || t.v2.getAnnotation(PATCH.class) != null
                  || t.v2.getAnnotation(HEAD.class) != null);
        });
  }

  private static Stream<ClassPath.ClassInfo> getClassesInStackGres() {
    return CLASSPATH_SCANNER.getTopLevelClasses().stream()
        .filter(classInfo -> classInfo.getPackageName().startsWith("io.stackgres."));
  }

}
