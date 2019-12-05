package io.stackgres.operator;


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
import javax.ws.rs.core.Response;

import com.google.common.reflect.ClassPath;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.GenericExceptionMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResourcesSerializationTest {

  private static final ClassLoader loader = Thread.currentThread().getContextClassLoader();

  private static ClassPath classpathScanner;

  @BeforeAll
  static void setUp() throws IOException {
    classpathScanner = ClassPath.from(loader);
  }

  @Test
  void returnTypesOfRestResponses_mustBeAnnotatedWithRegisterForReflection() {

    getRestMethods()
        .filter(method -> !method.getReturnType().equals(Void.TYPE))
        .forEach(method -> {
          Class<?> returnType = method.getReturnType();
          if (returnType.getPackage().getName().startsWith("io.stackgres")) {
            assertNotNull(returnType.getAnnotation(RegisterForReflection.class), "class "
                + returnType.getName() + " must be annotated with register for reflection");
          }

        });
  }

  @Test
  void parametersOfGenericReturnTypesOfRestResponses_mustBeAnnotatedWithRegisterForReflection() {

    getRestMethods()
        .filter(method -> !method.getReturnType().equals(Void.TYPE))
        .forEach(method -> {
          Class<?> returnType = method.getReturnType();

          if (Collection.class.isAssignableFrom(returnType)) {
            Arrays.stream(((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()).forEach(gt -> {
              if (gt instanceof java.lang.Class){
                Class<?> gType = (Class<?>) gt;
                assertNotNull(gType.getAnnotation(RegisterForReflection.class), "class "
                    + gType.getName() + " must be annotated with register for reflection");
              }

            });
          }

        });
  }

  @Test
  void parametersOfRestRequests_mustBeAnnotatedWithRegisterForReflection() {
    getRestMethods()
        .forEach(method -> {
          Arrays.stream(method.getParameterTypes())
              .filter(p -> p.getName().startsWith("io.stackgres"))
              .forEach(p -> {
                assertNotNull(p.getAnnotation(RegisterForReflection.class), "class "
                    + p.getName() + " must be annotated with register for reflection");
              });
        });
  }

  @Test
  void errorResponse_mustBeAnnotatedWithRegisterForReflection(){
    GenericExceptionMapper genericExceptionMapper = new GenericExceptionMapper();
    Response response = genericExceptionMapper.toResponse(new RuntimeException());

    Class<?> entityClazz = response.getEntity().getClass();
    assertNotNull(entityClazz.getAnnotation(RegisterForReflection.class), "class "
        + entityClazz.getName() + " must be annotated with register for reflection");
  }


  static Stream<Method> getRestMethods() {
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
              .filter(method -> method.getAnnotation(POST.class) != null
                  || method.getAnnotation(GET.class) != null
                  || method.getAnnotation(PUT.class) != null
                  || method.getAnnotation(DELETE.class) != null
                  || method.getAnnotation(OPTIONS.class) != null
                  || method.getAnnotation(PATCH.class) != null
                  || method.getAnnotation(HEAD.class) != null);
        });
  }

  private static Stream<ClassPath.ClassInfo> getClassesInStackGres() {
    return classpathScanner.getTopLevelClasses().stream()
        .filter(classInfo -> classInfo.getPackageName().startsWith("io.stackgres"));
  }

}
