/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;

public class TransformerTestUtil {

  public static final Random RANDOM = new Random(7);

  public static <T, S> void assertTransformation(
      Transformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toTarget(source);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toSource(target);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(source), JsonUtil.toJson(actualSource),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static <T extends ResourceDto, S extends CustomResource<?, ?>> void assertTransformation(
      AbstractResourceTransformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toDto(source);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toCustomResource(target, source);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(source), JsonUtil.toJson(actualSource),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static <T extends ResourceDto, S extends CustomResource<?, ?>> void assertTransformation(
      DependencyResourceTransformer<T, S> transformer,
      TransformerTuple<T, S> transformerTuple,
      List<String> clusters) {

    var target = transformerTuple.target();
    var source = transformerTuple.source();

    var actualTarget = transformer.toResource(source, clusters);

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from CRD to DTO doesn't "
            + "return the expected output. Which means that the transformer is not accurately "
            + "transforming CRDs to DTOs. "
            + "It could also mean that the transformed classes doesn't have implemented the "
            + "equals method");

    var actualSource = transformer.toCustomResource(target, source);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs. "
            + "It could also mean that the transformed classes doesn't have implemented the "
            + "equals method");
  }

  public static TransformerTuple<Metadata, ObjectMeta> createMetadataTuple() {
    final String clusterName = StringUtils.getRandomClusterName();
    final String namespace = StringUtils.getRandomNamespace();
    var crdMetadata = new ObjectMetaBuilder()
        .withName(clusterName)
        .withNamespace(namespace)
        .build();
    var dtoMetadata = new Metadata();
    dtoMetadata.setName(clusterName);
    dtoMetadata.setNamespace(namespace);
    return new TransformerTuple<>(dtoMetadata, crdMetadata);
  }

  /**
   * Creates a tuple of objects whose classes with the same random data in the common fields.
   *
   * @param targetClazz the target class of the object to be put into tuple
   * @param sourceClazz the source of the object to be put into the tuple
   * @param <T>         the target generic type
   * @param <S>         the source generic type
   * @return the created tuple
   */
  @SuppressWarnings("unchecked")
  public static <T, S> TransformerTuple<T, S> fillTupleWithRandomData(
      Class<T> targetClazz, Class<S> sourceClazz) {

    try {

      if (targetClazz == sourceClazz) {
        if (isValueType(targetClazz) && isValueType(sourceClazz)) {
          Object value = generateRandomValue(targetClazz);
          return new TransformerTuple<>((T) value, (S) value);
        }
      }

      Field[] targetFields = getRepresentativeFields(targetClazz);
      Field[] sourceField = getRepresentativeFields(sourceClazz);

      T targetInstance = targetClazz.getDeclaredConstructor().newInstance();
      S sourceInstance = sourceClazz.getDeclaredConstructor().newInstance();

      List<TransformerTuple<Field, Field>> commonFields = getCommonFields(
          targetFields, sourceField);

      commonFields.forEach(
          fieldTuple -> setRandomDataForFields(fieldTuple, targetInstance, sourceInstance)
      );

      return new TransformerTuple<>(targetInstance, sourceInstance);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

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
    } else if (Quantity.class.isAssignableFrom(valueClass)) {
      return new Quantity(String.valueOf(RANDOM.nextInt()),
          QUANTITY_UNITS[RANDOM.nextInt(QUANTITY_UNITS.length)]);
    } else if (IntOrString.class.isAssignableFrom(valueClass)) {
      return new IntOrString(RANDOM.nextInt());
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
      } else if (BigDecimal.class.isAssignableFrom(valueClass)) {
        return BigDecimal.valueOf(Integer.toUnsignedLong(value));
      } else if (BigInteger.class.isAssignableFrom(valueClass)) {
        return BigInteger.valueOf(Integer.toUnsignedLong(value));
      }
    }
    throw new IllegalArgumentException("Value class " + valueClass.getName() + " not supported");
  }

  private static List<TransformerTuple<Field, Field>> getCommonFields(
      Field[] targetFields,
      Field[] sourceFields) {

    List<TransformerTuple<Field, Field>> commonFields = new ArrayList<>();

    // If the performance becomes a problem this could be improved by using quick select, but is
    // unlikely
    for (Field targetField : targetFields) {
      String targetFieldName = getFieldName(targetField);
      for (Field sourceField : sourceFields) {
        String sourceFieldName = getFieldName(sourceField);
        if (Objects.equals(targetFieldName, sourceFieldName)) {
          commonFields.add(
              new TransformerTuple<>(targetField, sourceField)
          );
        }
      }
    }

    return commonFields;
  }

  private static <T, S> void setRandomDataForFields(
      TransformerTuple<Field, Field> fieldTuple, T target, S source) {

    try {

      Field sourceField = fieldTuple.source();
      Field targetField = fieldTuple.target();
      sourceField.setAccessible(true);
      targetField.setAccessible(true);

      if (sourceField.getType() == targetField.getType()) {
        if (List.class.isAssignableFrom(sourceField.getType())) {
          var sourceParameterType = getCollectionGenericType(sourceField);
          var targetParameterType = getCollectionGenericType(targetField);
          var listTuple = generateRandomListTuple(
              targetParameterType,
              sourceParameterType
          );
          sourceField.set(source, listTuple.source());
          targetField.set(target, listTuple.target());
        } else if (Map.class.isAssignableFrom(sourceField.getType())) {
          var targetKeyType = getCollectionGenericType(targetField);
          var targetValueType = getMapValueType(targetField);
          var sourceKeyType = getCollectionGenericType(sourceField);
          var sourceValueType = getMapValueType(sourceField);

          var mapTuple = generateRandomMapTuple(
              targetKeyType, targetValueType,
              sourceKeyType, sourceValueType
          );
          sourceField.set(source, mapTuple.source());
          targetField.set(target, mapTuple.target());
        } else {
          var valueTuple = fillTupleWithRandomData(
              targetField.getType(),
              sourceField.getType()
          );
          sourceField.set(source, valueTuple.source());
          targetField.set(target, valueTuple.target());
        }
      } else {
        var valueTuple = fillTupleWithRandomData(
            targetField.getType(),
            sourceField.getType()
        );
        sourceField.set(source, valueTuple.source());
        targetField.set(target, valueTuple.target());
      }

      sourceField.setAccessible(false);
      targetField.setAccessible(false);

    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public static TransformerTuple<List<String>, List<String>> generateRandomListTuple() {
    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    List<String> targetList = new ArrayList<>(desiredListSize);
    List<String> sourceList = new ArrayList<>(desiredListSize);

    for (int i = 0; i < desiredListSize; i++) {
      String randomValue = StringUtils.getRandomString(10);
      targetList.add(randomValue);
      sourceList.add(randomValue);
    }

    return new TransformerTuple<>(targetList, sourceList);
  }

  public static <T, S> TransformerTuple<List<T>, List<S>> generateRandomListTuple(
      Class<T> targetParameterizedType, Class<S> sourceParameterizedType) {

    int desiredListSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    List<T> targetList = new ArrayList<>(desiredListSize);
    List<S> sourceList = new ArrayList<>(desiredListSize);

    for (int i = 0; i < desiredListSize; i++) {
      TransformerTuple<T, S> item = fillTupleWithRandomData(
          targetParameterizedType, sourceParameterizedType);
      targetList.add(item.target());
      sourceList.add(item.source());
    }

    return new TransformerTuple<>(targetList, sourceList);
  }

  public static <K1, V1, K2, V2> TransformerTuple<Map<K1, V1>, Map<K2, V2>> generateRandomMapTuple(
      Class<K1> targetKeyType,
      Class<V1> targetValueType,
      Class<K2> sourceKeyType,
      Class<V2> sourceValueType) {

    int desiredMapSize = RANDOM.nextInt(10) + 1; //More than this could be counter-productive

    Map<K1, V1> targetMap = new HashMap<>(desiredMapSize);
    Map<K2, V2> sourceMap = new HashMap<>(desiredMapSize);

    for (int i = 0; i < desiredMapSize; i++) {
      TransformerTuple<K1, K2> key = fillTupleWithRandomData(
          targetKeyType, sourceKeyType);
      TransformerTuple<V1, V2> value = fillTupleWithRandomData(
          targetValueType, sourceValueType);
      targetMap.put(key.target(), value.target());
      sourceMap.put(key.source(), value.source());
    }

    return new TransformerTuple<>(targetMap, sourceMap);
  }

  private static String getFieldName(Field field) {
    if (field.isAnnotationPresent(JsonProperty.class)) {
      JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
      final String configuredFieldName = jsonProperty.value();
      if (configuredFieldName.isEmpty()) {
        return field.getName();
      } else {
        return configuredFieldName;
      }
    } else {
      return field.getName();
    }
  }

  private static boolean isValueType(Class<?> type) {
    return Quantity.class.isAssignableFrom(type)
        || IntOrString.class.isAssignableFrom(type)
        || String.class == type
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
