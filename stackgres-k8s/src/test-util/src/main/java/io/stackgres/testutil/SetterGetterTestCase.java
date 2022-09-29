/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public abstract class SetterGetterTestCase {

  protected void assertSettersAndGetters(Class<?> sourceClazz) {
    if (isValueType(sourceClazz)) {
      return;
    }

    Field[] fields = getRepresentativeFields(sourceClazz);

    Arrays.asList(fields).forEach(
        field -> assertSettersAndGettersForField(field, sourceClazz)
    );
  }

  private Field[] getRepresentativeFields(Class<?> clazz) {
    if (clazz != null) {
      Field[] declaredFields = clazz.getDeclaredFields();
      Field[] parentFields = getRepresentativeFields(clazz.getSuperclass());
      return Stream
          .concat(
              Arrays.stream(declaredFields),
              Arrays.stream(parentFields)
              .filter(field -> Arrays.stream(declaredFields)
                  .map(Field::getName)
                  .noneMatch(field.getName()::equals)))
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .filter(field -> !Modifier.isFinal(field.getModifiers()))
          .toArray(Field[]::new);
    } else {
      return new Field[0];
    }
  }

  private <S> void assertSettersAndGettersForField(
      Field field, Class<S> targetClazz) {
    if (List.class.isAssignableFrom(field.getType())) {
      var parameterType = getCollectionGenericType(field);
      assertSettersAndGetters(
          parameterType
      );
    } else if (Map.class.isAssignableFrom(field.getType())) {
      var valueType = getMapValueType(field);

      assertSettersAndGetters(
          valueType
      );
    } else {
      String fieldName = field.getName();
      String pascalCaseFieldName =
          fieldName.substring(0, 1).toUpperCase(Locale.US)
          + fieldName.substring(1);

      final String setMethodName = "set" + pascalCaseFieldName;
      Method setMethod = findMethod(
          targetClazz,
          setMethodName,
          field.getType());
      final String getMethodName = "get" + pascalCaseFieldName;
      final String isMethodName = "is" + pascalCaseFieldName;
      Method getMethod = findMethod(
          targetClazz,
          getMethodName);
      Method isMethod = findMethod(
          targetClazz,
          isMethodName);
      assertNotNull(setMethod, "Set method " + setMethodName + " with parameter type "
          + field.getType() + " was not found in class "
          + targetClazz.getName() + " and field " + fieldName);
      if (isBoolean(field.getType())) {
        assertTrue(getMethod != null || isMethod != null, "Get method " + getMethodName + " or "
            + isMethodName + " not found in class "
            + targetClazz.getName() + " and field " + fieldName);
        Class<?> getMethodReturnType =
            getMethod != null ? getMethod.getReturnType() : isMethod.getReturnType();
        assertSame(field.getType(), getMethodReturnType, "Get method " + getMethodName
            + " return type " + getMethodReturnType.getName() + " should be of type "
            + field.getType().getName() + " in class "
            + targetClazz.getName() + " and field " + fieldName);
      } else {
        assertNotNull(getMethod, "Get method " + getMethodName + " not found in class "
            + targetClazz.getName() + " and field " + fieldName);
        assertSame(field.getType(), getMethod.getReturnType(), "Get method " + getMethodName
            + " return type " + getMethod.getReturnType().getName() + " should be of type "
            + field.getType().getName() + " in class "
            + targetClazz.getName() + " and field " + fieldName);
      }

      assertSettersAndGetters(field.getType());
    }
  }

  private boolean isBoolean(Class<?> valueClass) {
    if (valueClass.isPrimitive()
        && valueClass.getName().equals("boolean")) {
      return true;
    } else if (valueClass == Boolean.class) {
      return true;
    }
    return false;
  }

  private boolean isValueType(Class<?> type) {
    return String.class == type
        || Number.class.isAssignableFrom(type)
        || Boolean.class == type
        || type.isPrimitive()
        || Object.class == type;
  }

  private Class<?> getCollectionGenericType(Field collectionField) {
    ParameterizedType listType = (ParameterizedType) collectionField.getGenericType();
    return (Class<?>) listType.getActualTypeArguments()[0];
  }

  private Class<?> getMapValueType(Field mapField) {
    ParameterizedType mapType = (ParameterizedType) mapField.getGenericType();
    return getMapValueType(mapType);
  }

  private Class<?> getMapValueType(ParameterizedType mapType) {
    if (mapType.getActualTypeArguments()[1] instanceof ParameterizedType parameterizedTypeValue) {
      if (Map.class.isAssignableFrom((Class<?>) parameterizedTypeValue.getRawType())) {
        return getMapValueType(parameterizedTypeValue);
      }
    }
    return (Class<?>) mapType.getActualTypeArguments()[1];
  }

  private Method findMethod(Class<?> targetClazz, String name, Class<?>... parameterTypes)
      throws SecurityException {
    try {
      return targetClazz.getMethod(name, parameterTypes);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
