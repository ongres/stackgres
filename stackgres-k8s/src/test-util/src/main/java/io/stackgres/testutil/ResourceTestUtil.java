/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

public class ResourceTestUtil {

  private static final Random RANDOM = new Random(7);

  public static <T> void assertEqualsAndHashCode(T target) {
    var targetCopy = JsonUtil.fromJson(JsonUtil.toJson(target), target.getClass());

    assertEquals(target, targetCopy, "Type "
        + target.getClass() + " has not correctly implemented equals method.");

    assertEquals(target.hashCode(), targetCopy.hashCode(), "Type "
        + target.getClass() + " has not correctly implemented hash method.");
  }

  public static <T> T createWithRandomData(Class<T> targetClazz) {
    return fillWithRandomData(targetClazz);
  }

  @SuppressWarnings("unchecked")
  private static <T> T fillWithRandomData(Class<T> targetClazz) {
    try {
      if (isValueType(targetClazz)) {
        Object value = generateRandomValue(targetClazz);
        return (T) value;
      }

      Field[] targetFields = getRepresentativeFields(targetClazz);

      T targetInstance = targetClazz.getDeclaredConstructor().newInstance();

      Arrays.asList(targetFields).forEach(
          field -> setRandomDataForFields(field, targetInstance)
      );

      return targetInstance;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static Field[] getRepresentativeFields(Class<?> clazz) {
    if (clazz != null) {
      Field[] declaredFields = clazz.getDeclaredFields();
      Field[] parentFields = getRepresentativeFields(clazz.getSuperclass());
      return Stream.concat(Arrays.stream(declaredFields), Arrays.stream(parentFields))
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .filter(field -> !Modifier.isFinal(field.getModifiers()))
          .toArray(Field[]::new);
    } else {
      return new Field[0];
    }
  }

  private static Object generateRandomValue(Class<?> valueClass) {
    if (valueClass.isPrimitive()) {
      switch (valueClass.getName()) {
        case "long":
          return RANDOM.nextLong();
        case "int":
          return RANDOM.nextInt();
        case "boolean":
          return RANDOM.nextBoolean();
        case "char":
          return (char) RANDOM.nextInt();
        case "float":
          return RANDOM.nextFloat();
        case "double":
          return RANDOM.nextDouble();
        default:
          throw new RuntimeException("Unsupported primitive type " + valueClass.getName());
      }
    } else if (valueClass == String.class || valueClass == Object.class) {
      return StringUtils.getRandomString(10);
    } else if (valueClass == Boolean.class) {
      return RANDOM.nextBoolean();
    } else if (Number.class.isAssignableFrom(valueClass)) {
      int value = RANDOM.nextInt(10) + 1;
      if (Integer.class.isAssignableFrom(valueClass)) {
        return value;
      } else if (Long.class.isAssignableFrom(valueClass)) {
        return Integer.toUnsignedLong(value);
      }
    }
    throw new IllegalArgumentException("Value class " + valueClass.getName() + " not supported");
  }

  private static <T, S> void setRandomDataForFields(
      Field field, T target) {

    try {

      field.setAccessible(true);

      if (List.class.isAssignableFrom(field.getType())) {
        var targetParameterType = getCollectionGenericType(field);
        var listTuple = generateRandomList(
            targetParameterType
        );
        field.set(target, listTuple);
      } else if (Map.class.isAssignableFrom(field.getType())) {
        var targetKeyType = getCollectionGenericType(field);
        var targetValueType = getMapValueType(field);

        var mapTuple = generateRandomMap(
            targetKeyType, targetValueType
        );
        field.set(target, mapTuple);
      }

      field.setAccessible(false);

    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> List<T> generateRandomList(
      Class<T> targetParameterizedType) {

    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    List<T> targetList = new ArrayList<>(desiredListSize);

    for (int i = 0; i < desiredListSize; i++) {
      T item = fillWithRandomData(targetParameterizedType);
      targetList.add(item);
    }

    return targetList;
  }

  private static <K1, V1> Map<K1, V1> generateRandomMap(
      Class<K1> targetKeyType,
      Class<V1> targetValueType) {

    int desiredMapSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    Map<K1, V1> targetMap = new HashMap<>(desiredMapSize);

    for (int i = 0; i < desiredMapSize; i++) {
      K1 key = fillWithRandomData(targetKeyType);
      V1 value = fillWithRandomData(targetValueType);
      targetMap.put(key, value);
    }

    return targetMap;
  }

  private static boolean isValueType(Class<?> type) {
    return String.class == type
        || Number.class.isAssignableFrom(type)
        || Boolean.class == type
        || type.isPrimitive()
        || Object.class == type;
  }

  private static Class<?> getCollectionGenericType(Field collectionField) {
    ParameterizedType listType = (ParameterizedType) collectionField.getGenericType();
    return (Class<?>) listType.getActualTypeArguments()[0];
  }

  private static Class<?> getMapValueType(Field mapField) {
    ParameterizedType listType = (ParameterizedType) mapField.getGenericType();
    return (Class<?>) listType.getActualTypeArguments()[1];
  }
}
