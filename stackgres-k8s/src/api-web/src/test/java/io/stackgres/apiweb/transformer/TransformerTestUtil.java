/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.Metadata;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.ModelTestUtil;
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
            + "transforming CRDs to DTOs.");

    var actualSource = transformer.toCustomResource(target, source);

    assertNotSame(source, actualSource,
        "The original CRD is being returned which can lead to side effects errors");

    JsonUtil.assertJsonEquals(JsonUtil.toJson(target), JsonUtil.toJson(actualTarget),
        "Transformation from DTO to CRD doesn't return the "
            + "expected output. Which means that the "
            + "transformer is not accurately transforming DTOs to CRDs.");
  }

  public static TransformerTuple<Metadata, ObjectMeta> createMetadataTuple() {
    final String clusterName = StringUtils.getRandomResourceName();
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
        if (ModelTestUtil.isValueType(targetClazz) && ModelTestUtil.isValueType(sourceClazz)) {
          Object value = ModelTestUtil.generateRandomValue(targetClazz);
          return new TransformerTuple<>((T) value, (S) value);
        }
      }

      List<Field> targetFields = ModelTestUtil.getRepresentativeFields(targetClazz);
      List<Field> sourceFields = ModelTestUtil.getRepresentativeFields(sourceClazz);

      T targetInstance = targetClazz.getDeclaredConstructor().newInstance();
      S sourceInstance = sourceClazz.getDeclaredConstructor().newInstance();

      List<TransformerTuple<Field, Field>> commonFields = getCommonFields(
          targetFields, sourceFields);

      commonFields.forEach(
          fieldTuple -> setRandomDataForFields(fieldTuple, targetInstance, sourceInstance)
      );

      return new TransformerTuple<>(targetInstance, sourceInstance);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }

  }

  private static List<TransformerTuple<Field, Field>> getCommonFields(
      List<Field> targetFields,
      List<Field> sourceFields) {

    List<TransformerTuple<Field, Field>> commonFields = new ArrayList<>();

    // If the performance becomes a problem this could be improved by using quick select, but is
    // unlikely
    for (Field targetField : targetFields) {
      String targetFieldName = ModelTestUtil.getJsonFieldName(targetField);
      for (Field sourceField : sourceFields) {
        String sourceFieldName = ModelTestUtil.getJsonFieldName(sourceField);
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

      if (List.class.isAssignableFrom(sourceField.getType())
          && List.class.isAssignableFrom(targetField.getType())) {
        var sourceParameterType = ModelTestUtil.getParameterFromGenericType(
            sourceField.getType(), sourceField.getGenericType(), 0);
        var targetParameterType = ModelTestUtil.getParameterFromGenericType(
            targetField.getType(), targetField.getGenericType(), 0);
        var listTuple = generateRandomListTuple(
            targetParameterType,
            sourceParameterType
        );
        sourceField.set(source,
            sourceField.getType() != List.class
            ? sourceField.getType().getDeclaredConstructor(List.class)
                .newInstance(listTuple.source()) : listTuple.source());
        targetField.set(target,
            targetField.getType() != List.class
            ? targetField.getType().getDeclaredConstructor(List.class)
                .newInstance(listTuple.target()) : listTuple.target());
      } else if (Map.class.isAssignableFrom(sourceField.getType())
          && Map.class.isAssignableFrom(targetField.getType())) {
        var targetKeyType = ModelTestUtil.getParameterFromGenericType(
            targetField.getType(), targetField.getGenericType(), 0);
        var targetValueType = ModelTestUtil.getParameterFromGenericType(
            targetField.getType(), targetField.getGenericType(), 1);
        var sourceKeyType = ModelTestUtil.getParameterFromGenericType(
            sourceField.getType(), sourceField.getGenericType(), 0);
        var sourceValueType = ModelTestUtil.getParameterFromGenericType(
            sourceField.getType(), sourceField.getGenericType(), 1);

        var mapTuple = generateRandomMapTuple(
            targetKeyType, targetValueType,
            sourceKeyType, sourceValueType
        );
        sourceField.set(source,
            sourceField.getType() != Map.class
            ? sourceField.getType().getDeclaredConstructor(Map.class)
                .newInstance(mapTuple.source()) : mapTuple.source());
        targetField.set(target,
            targetField.getType() != Map.class
            ? targetField.getType().getDeclaredConstructor(Map.class)
                .newInstance(mapTuple.target()) : mapTuple.target());
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

}
