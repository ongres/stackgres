/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Quantity;

public class RandomObjectUtils {

  public static final Random RANDOM = new Random(7);

  private RandomObjectUtils() {
    throw new IllegalStateException("Should not be instantiated");
  }

  @SuppressWarnings("unchecked")
  public static <T> T generateRandomObject(Class<T> clazz) {
    if (isValueType(clazz)) {
      final Object value = generateRandomValue(clazz);
      return (T) value;
    }

    try {
      T targetInstance = clazz.getDeclaredConstructor().newInstance();

      Field[] fields = getRepresentativeFields(clazz);

      for (Field field : fields) {
        setRandomDataForField(field, targetInstance);
      }

      return targetInstance;
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

  }

  private static <T> void setRandomDataForField(Field field, T instance)
      throws IllegalAccessException {
    field.setAccessible(true);
    if (List.class.isAssignableFrom(field.getType())) {
      var parameterType = getCollectionGenericType(field);
      var list = generateRandomList(parameterType);
      field.set(instance, wrapInFieldType(field, list));
    } else if (Map.class.isAssignableFrom(field.getType())) {
      var keyType = getCollectionGenericType(field);
      var valueType = getMapValueType(field);
      var map = generateRandomMap(
          keyType, valueType
      );
      field.set(instance, wrapInFieldType(field, map));
    } else {
      var value = generateRandomObject(field.getType());
      field.set(instance, value);
    }

    field.setAccessible(false);
  }

  private static Object wrapInFieldType(Field field, List<?> list) {
    try {
      if (field.getType().isInterface()) {
        return list;
      }
      return field.getType().getConstructor(Map.class).newInstance(list);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static Object wrapInFieldType(Field field, Map<?, ?> map) {
    try {
      if (field.getType().isInterface()) {
        return map;
      }
      return field.getType().getConstructor(Map.class).newInstance(map);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static <K, V> Map<K, V> generateRandomMap(Class<K> keyType, Class<V> valueType) {
    int desiredMapSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    Map<K, V> map = new HashMap<>(desiredMapSize);

    for (int i = 0; i < desiredMapSize; i++) {
      var key = generateRandomObject(keyType);
      var value = generateRandomObject(valueType);
      map.put(key, value);
    }
    return map;

  }

  public static <T> List<T> generateRandomList(Class<T> clazz) {
    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive
    List<T> list = new ArrayList<>(desiredListSize);
    for (int i = 0; i < desiredListSize; i++) {
      var item = generateRandomObject(clazz);
      list.add(item);
    }
    return list;
  }

  public static Field[] getRepresentativeFields(Class<?> clazz) {
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

  static final String[] QUANTITY_UNITS = new String[] {
      "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "n",
      "u", "m", "k", "M", "G", "T", "P", "E", "" };

  private static Object generateRandomValue(Class<?> clazz) {
    if (clazz.isPrimitive()) {
      return switch (clazz.getName()) {
        case "long" -> RANDOM.nextLong();
        case "int" -> RANDOM.nextInt();
        case "boolean" -> RANDOM.nextBoolean();
        case "char" -> (char) RANDOM.nextInt();
        case "float" -> RANDOM.nextFloat();
        case "double" -> RANDOM.nextDouble();
        default -> throw new RuntimeException("Unsupported primitive type " + clazz.getName());
      };
    } else if (Quantity.class.isAssignableFrom(clazz)) {
      return new Quantity(String.valueOf(RANDOM.nextInt()),
          QUANTITY_UNITS[RANDOM.nextInt(QUANTITY_UNITS.length)]);
    } else if (clazz == String.class) {
      return StringUtils.getRandomString(10);
    } else if (clazz == Object.class) {
      return StringUtils.getRandomString(10);
    } else if (clazz == Boolean.class) {
      return RANDOM.nextBoolean();
    } else if (Number.class.isAssignableFrom(clazz)) {
      int value = RANDOM.nextInt(10) + 1;
      if (Integer.class.isAssignableFrom(clazz)) {
        return value;
      } else if (Long.class.isAssignableFrom(clazz)) {
        return Integer.toUnsignedLong(value);
      } else if (BigDecimal.class.isAssignableFrom(clazz)) {
        return BigDecimal.valueOf(value);
      } else if (BigInteger.class.isAssignableFrom(clazz)) {
        return BigInteger.valueOf(value);
      }
    }
    throw new IllegalArgumentException("Value class " + clazz.getName() + " not supported");
  }

  private static boolean isValueType(Class<?> type) {
    return Quantity.class.isAssignableFrom(type)
        || String.class == type
        || Number.class.isAssignableFrom(type)
        || Boolean.class == type
        || type.isPrimitive()
        || Object.class == type;
  }

  private static Class<?> getCollectionGenericType(Field collectionField) {
    if (collectionField.getGenericType() instanceof ParameterizedType) {
      ParameterizedType listType = (ParameterizedType) collectionField.getGenericType();
      return (Class<?>) listType.getActualTypeArguments()[0];
    }
    Class<?> type = collectionField.getType();
    while (type != Object.class) {
      if (type.getGenericSuperclass() instanceof ParameterizedType) {
        ParameterizedType listType = (ParameterizedType) type.getGenericSuperclass();
        return (Class<?>) listType.getActualTypeArguments()[0];
      }
      type = type.getSuperclass();
    }
    throw new IllegalArgumentException(
        "Field " + collectionField + " is has not a parameterized"
            + " type or it type has not any parameterized superclass");
  }

  private static Class<?> getMapValueType(Field mapField) {
    if (mapField.getGenericType() instanceof ParameterizedType) {
      ParameterizedType listType = (ParameterizedType) mapField.getGenericType();
      return (Class<?>) listType.getActualTypeArguments()[1];
    }
    Class<?> type = mapField.getType();
    while (type != Object.class) {
      if (type.getGenericSuperclass() instanceof ParameterizedType) {
        ParameterizedType listType = (ParameterizedType) type.getGenericSuperclass();
        return (Class<?>) listType.getActualTypeArguments()[1];
      }
      type = type.getSuperclass();
    }
    throw new IllegalArgumentException(
        "Field " + mapField + " is has not a parameterized"
            + " type or it type has not any parameterized superclass");
  }
}
