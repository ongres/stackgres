/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Quantity;

public class ModelTestUtil {

  private static final Random RANDOM = new Random(7);

  public static <T> void assertEqualsAndHashCode(Class<T> targetClazz) {
    visit(new CheckEqualsAndHashCodeVisitor(), targetClazz);
  }

  public static class CheckEqualsAndHashCodeVisitor implements ResourceVisitor<Void> {
    @Override
    public Void onObject(Class<?> clazz, List<Field> fields) {
      for (Field field : fields) {
        visit(this, field.getType(), field.getGenericType());
      }

      if (clazz.getPackage().getName().startsWith("io.stackgres.")) {
        var target = createWithRandomData(clazz);
        JsonNode targetJsonCopy = JsonUtil.toJson(target);
        var targetCopy1 = JsonUtil.fromJson(targetJsonCopy, target.getClass());
        var targetCopy2 = JsonUtil.fromJson(targetJsonCopy, target.getClass());
  
        assertEquals(targetCopy1, targetCopy2, "Type "
            + target.getClass() + " has not correctly implemented equals method.");
  
        assertEquals(targetCopy1.hashCode(), targetCopy2.hashCode(), "Type "
            + target.getClass() + " has not correctly implemented hash method.");
  
        var resource = ModelTestUtil.createWithRandomData(clazz);
        var anotherResource = ModelTestUtil.createWithRandomData(clazz);
        JsonNode resourceJsonCopy = JsonUtil.toJson(resource);
        JsonNode anotherResourceJsonCopy = JsonUtil.toJson(anotherResource);

        if (!Objects.equals(resourceJsonCopy, anotherResourceJsonCopy)) {
          assertNotEquals(anotherResource, resource, "Type "
              + target.getClass() + " has not correctly implemented equals method"
              + " since different objects are see as equals:\n" + resourceJsonCopy
              + "\n is seen equals as \n" + anotherResourceJsonCopy);
        }
      }

      return null;
    }

    @Override
    public Void onList(Class<?> clazz, Class<?> elementClazz) {
      return visit(this, elementClazz);
    }

    @Override
    public Void onMap(Class<?> clazz, Class<?> keyClazz, Class<?> valueClazz, Type genericType) {
      return visit(this, valueClazz, genericType);
    }

    @Override
    public Void onValue(Class<?> clazz) {
      return null;
    }
  }

  public static <T> void assertSettersAndGetters(Class<T> targetClazz) {
    visit(new CheckSettersAndGettersVisitor(), targetClazz);
  }

  public static class CheckSettersAndGettersVisitor implements ResourceVisitor<Void> {
    @Override
    public Void onObject(Class<?> clazz, List<Field> fields) {
      if (clazz.getPackage().getName().startsWith("io.stackgres.")) {
        for (Field field : fields) {
          String fieldName = field.getName();
          String pascalCaseFieldName =
              fieldName.substring(0, 1).toUpperCase(Locale.US)
              + fieldName.substring(1);
  
          final String setMethodName = "set" + pascalCaseFieldName;
          Method setMethod = findMethod(
              clazz,
              setMethodName,
              field.getType());
          final String getMethodName = "get" + pascalCaseFieldName;
          final String isMethodName = "is" + pascalCaseFieldName;
          Method getMethod = findMethod(
              clazz,
              getMethodName);
          Method isMethod = findMethod(
              clazz,
              isMethodName);
          assertNotNull(setMethod, "Set method " + setMethodName + " with parameter type "
              + field.getType() + " was not found in class "
              + clazz.getName() + " and field " + fieldName);
          if (isBoolean(field.getType())) {
            assertTrue(getMethod != null || isMethod != null, "Get method " + getMethodName + " or "
                + isMethodName + " not found in class "
                + clazz.getName() + " and field " + fieldName);
            @SuppressWarnings("null")
            Class<?> getMethodReturnType =
                getMethod != null ? getMethod.getReturnType() : isMethod.getReturnType();
            assertSame(field.getType(), getMethodReturnType, "Get method " + getMethodName
                + " return type " + getMethodReturnType.getName() + " should be of type "
                + field.getType().getName() + " in class "
                + clazz.getName() + " and field " + fieldName);
          } else {
            assertNotNull(getMethod, "Get method " + getMethodName + " not found in class "
                + clazz.getName() + " and field " + fieldName);
            assertSame(field.getType(), getMethod.getReturnType(), "Get method " + getMethodName
                + " return type " + getMethod.getReturnType().getName() + " should be of type "
                + field.getType().getName() + " in class "
                + clazz.getName() + " and field " + fieldName);
          }
        }
      }

      for (Field field : fields) {
        visit(this, field.getType(), field.getGenericType());
      }

      return null;
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

    private Method findMethod(Class<?> targetClazz, String name, Class<?>... parameterTypes)
        throws SecurityException {
      try {
        return targetClazz.getMethod(name, parameterTypes);
      } catch (NoSuchMethodException e) {
        return null;
      }
    }

    @Override
    public Void onList(Class<?> clazz, Class<?> elementClazz) {
      return visit(this, elementClazz);
    }

    @Override
    public Void onMap(Class<?> clazz, Class<?> keyClazz, Class<?> valueClazz, Type genericType) {
      return visit(this, valueClazz, genericType);
    }

    @Override
    public Void onValue(Class<?> clazz) {
      return null;
    }
  }

  public static void assertJsonInoreUnknownProperties(Class<?> targetClazz) {
    visit(new CheckJsonInoreUnknownPropertiesVisitor(), targetClazz);
  }

  public static class CheckJsonInoreUnknownPropertiesVisitor implements ResourceVisitor<Void> {
    @Override
    public Void onObject(Class<?> clazz, List<Field> fields) {
      if (clazz.getPackage().getName().startsWith("io.stackgres.")) {
        JsonIgnoreProperties jsonIgnoreProperties = clazz.getAnnotation(JsonIgnoreProperties.class);
        for (var currentClazz = clazz; jsonIgnoreProperties == null;) {
          if (currentClazz == Object.class) {
            break;
          }
          currentClazz = currentClazz.getSuperclass();
          jsonIgnoreProperties = currentClazz.getAnnotation(JsonIgnoreProperties.class);
        }
        assertNotNull(jsonIgnoreProperties,
            "Annotation " + JsonIgnoreProperties.class.getSimpleName()
            + " is not present for class " + clazz.getName());
        assertTrue(jsonIgnoreProperties.ignoreUnknown(),
            "Annotation " + JsonIgnoreProperties.class.getSimpleName()
            + " has property ignoreUnknown set to false for class " + clazz.getName());
      }
      for (Field field : fields) {
        visit(this, field.getType(), field.getGenericType());
      }
      return null;
    }

    @Override
    public Void onList(Class<?> clazz, Class<?> elementClazz) {
      return visit(this, elementClazz);
    }

    @Override
    public Void onMap(Class<?> clazz, Class<?> keyClazz, Class<?> valueClazz, Type genericType) {
      return visit(this, valueClazz, genericType);
    }

    @Override
    public Void onValue(Class<?> clazz) {
      return null;
    }
  }

  public static <T> T createWithRandomData(Class<T> targetClazz) {
    return visit(new RandomDataVisitor<>(), targetClazz);
  }

  public static class RandomDataVisitor<T> implements ResourceVisitor<T> {
    @Override
    @SuppressWarnings("unchecked")
    public T onObject(Class<?> clazz, List<Field> fields) {
      T targetInstance;
      try {
        targetInstance = (T) clazz.getDeclaredConstructor().newInstance();
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(ex);
      }
      for (Field field : fields) {
        setFieldWithRandomData(field, targetInstance);
      }
      return targetInstance;
    }

    private void setFieldWithRandomData(
        Field field, Object target) {
      try {
        field.setAccessible(true);
        field.set(target,
            visit(new RandomDataVisitor<>(), field.getType(), field.getGenericType()));
        field.setAccessible(false);
      } catch (ReflectiveOperationException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T onList(Class<?> clazz, Class<?> elementClazz) {
      int desiredListSize = RANDOM.nextInt(3) + 1; //More than this could be counter-productive

      List<Object> targetList = new ArrayList<>(desiredListSize);
      if (clazz != List.class) {
        try {
          var constructor = Arrays.stream(clazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == List.class)
              .findAny()
              .orElseThrow();
          targetList = (List<Object>) constructor.newInstance(targetList);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      for (int i = 0; i < desiredListSize; i++) {
        Object item = visit(new RandomDataVisitor<>(), elementClazz);
        targetList.add(item);
      }

      return (T) targetList;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T onMap(Class<?> clazz, Class<?> keyClazz, Class<?> valueClazz, Type valueType) {
      int desiredMapSize = RANDOM.nextInt(3) + 1; //More than this could be counter-productive

      Map<Object, Object> targetMap = new HashMap<>(desiredMapSize);
      if (clazz != Map.class) {
        try {
          var constructor = Arrays.stream(clazz.getConstructors())
              .filter(c -> c.getParameterTypes().length == 1
                  && c.getParameterTypes()[0] == Map.class)
              .findAny()
              .orElseThrow();
          targetMap = (Map<Object, Object>) constructor.newInstance(targetMap);
        } catch (SecurityException | InvocationTargetException | IllegalAccessException
            | InstantiationException ex) {
          throw new RuntimeException(ex);
        }
      }

      for (int i = 0; i < desiredMapSize; i++) {
        Object key = visit(new RandomDataVisitor<>(), keyClazz);
        Object value = visit(new RandomDataVisitor<>(), valueClazz, valueType);
        targetMap.put(key, value);
      }

      return (T) targetMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T onValue(Class<?> clazz) {
      return (T) generateRandomValue(clazz);
    }
  }

  public interface ResourceVisitor<T> {
    T onObject(Class<?> clazz, List<Field> fields);

    T onList(Class<?> clazz, Class<?> elementClazz);

    T onMap(Class<?> clazz, Class<?> keyClazz, Class<?> valueClazz, Type genericType);

    T onValue(Class<?> clazz);
  }

  public static <T> T visit(ResourceVisitor<T> visitor, Class<?> clazz) {
    return visit(visitor, clazz, null);
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  public static <T> T visit(ResourceVisitor<T> visitor, Class<?> clazz,
      Type genericType) {
    if (isValueType(clazz)) {
      return visitor.onValue(clazz);
    } else if (List.class.isAssignableFrom(clazz)) {
      var elementClazz = getParameterFromGenericType(clazz, genericType, 0);
      return visitor.onList(clazz, elementClazz);
    } else if (Map.class.isAssignableFrom(clazz)) {
      var keyClazz = getParameterFromGenericType(clazz, genericType, 0);
      var valueClazz = getParameterFromGenericType(clazz, genericType, 1);
      if (genericType instanceof ParameterizedType parameterizedValueType) {
        return visitor.onMap(clazz, keyClazz, valueClazz,
            parameterizedValueType.getActualTypeArguments()[1]);
      } else {
        return visitor.onMap(clazz, keyClazz, valueClazz,
            genericType);
      }
    }

    List<Field> targetFields = getRepresentativeFields(clazz);

    return visitor.onObject(clazz, targetFields);
  }

  public static Class<?> getParameterFromGenericType(Class<?> clazz, Type genericType, int index) {
    ParameterizedType listType = getParameterizedType(clazz, genericType);
    return TypeFactory.rawClass(listType.getActualTypeArguments()[index]);
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private static ParameterizedType getParameterizedType(Class<?> clazz, Type genericType) {
    if (genericType instanceof ParameterizedType currentGenericType) {
      return currentGenericType;
    } else {
      Class<?> currentClazz = clazz;
      while (true) {
        if (currentClazz.getGenericSuperclass() instanceof ParameterizedType) {
          return (ParameterizedType) currentClazz.getGenericSuperclass();
        }
        var currentInterfaces = Arrays.asList(currentClazz.getGenericInterfaces());
        if (currentInterfaces.stream()
            .anyMatch(ParameterizedType.class::isInstance)) {
          return currentInterfaces.stream()
              .filter(ParameterizedType.class::isInstance)
              .map(ParameterizedType.class::cast)
              .findFirst()
              .orElseThrow();
        }
        currentClazz = currentClazz.getSuperclass();
        if (currentClazz == Object.class) {
          break;
        }
      }
      throw new RuntimeException(
          "Class " + clazz.getName() + " do not have any generics!");
    }
  }

  private static boolean isValueType(Class<?> type) {
    return Quantity.class.isAssignableFrom(type)
        || String.class == type
        || Number.class.isAssignableFrom(type)
        || Boolean.class == type
        || type.isPrimitive()
        || Object.class == type;
  }

  private static List<Field> getRepresentativeFields(Class<?> clazz) {
    if (clazz != null) {
      Field[] declaredFields = clazz.getDeclaredFields();
      List<Field> parentFields = getRepresentativeFields(clazz.getSuperclass());
      List<String> ignoredFields = Optional
          .ofNullable(clazz.getAnnotation(JsonIgnoreProperties.class))
          .map(JsonIgnoreProperties::value)
          .map(Arrays::asList)
          .orElse(List.of());
      return Stream
          .concat(
              Arrays.stream(declaredFields),
              parentFields.stream()
              .filter(parentField -> Optional.ofNullable(parentField.getAnnotation(JsonProperty.class))
                  .map(JsonProperty::value)
                  .filter(Predicate.not(JsonProperty.USE_DEFAULT_NAME::equals))
                  .or(() -> Optional.of(parentField.getName()))
                  .stream()
                  .allMatch(parentFieldName -> Arrays.stream(declaredFields)
                      .map(field -> Optional.ofNullable(field.getAnnotation(JsonProperty.class))
                          .map(JsonProperty::value)
                          .filter(Predicate.not(JsonProperty.USE_DEFAULT_NAME::equals))
                          .orElse(parentField.getName()))
                      .noneMatch(parentFieldName::equals))))
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .filter(field -> !Modifier.isFinal(field.getModifiers()))
          .filter(field -> !field.isAnnotationPresent(JsonIgnore.class))
          .filter(field -> !ignoredFields.contains(
              Optional.ofNullable(field.getAnnotation(JsonProperty.class))
              .map(JsonProperty::value)
              .filter(Predicate.not(JsonProperty.USE_DEFAULT_NAME::equals))
              .orElse(field.getName())))
          .toList();
    } else {
      return List.of();
    }
  }

  static final String[] QUANTITY_UNITS = new String[] {
      "Ki", "Mi", "Gi", "Ti", "Pi", "Ei", "n",
      "u", "m", "k", "M", "G", "T", "P", "E", "" };

  @SuppressWarnings("unchecked")
  public static <T> T generateRandom(Class<?> valueClass) {
    return (T) generateRandomValue(valueClass);
  }

  public static Object generateRandomValue(Class<?> valueClass) {
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
    } else if (Quantity.class.isAssignableFrom(valueClass)) {
      return new Quantity(String.valueOf(RANDOM.nextInt()),
          QUANTITY_UNITS[RANDOM.nextInt(QUANTITY_UNITS.length)]);
    } else if (valueClass == String.class || valueClass == Object.class) {
      return "rnd-" + StringUtils.getRandomString(10).toLowerCase(Locale.US);
    } else if (valueClass == Boolean.class) {
      return RANDOM.nextBoolean();
    } else if (Number.class.isAssignableFrom(valueClass)) {
      int value = RANDOM.nextInt(10) + 1;
      if (Integer.class.isAssignableFrom(valueClass)) {
        return value;
      } else if (Long.class.isAssignableFrom(valueClass)) {
        return Integer.toUnsignedLong(value);
      } else if (BigDecimal.class.isAssignableFrom(valueClass)) {
        return BigDecimal.valueOf(value);
      } else if (BigInteger.class.isAssignableFrom(valueClass)) {
        return BigInteger.valueOf(value);
      }
    }
    throw new IllegalArgumentException("Value class " + valueClass.getName() + " not supported");
  }

}
