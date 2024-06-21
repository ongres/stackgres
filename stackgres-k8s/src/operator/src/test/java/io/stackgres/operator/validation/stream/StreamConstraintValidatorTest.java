/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSource;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StackGresStreamTarget;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetCloudEvent;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetCloudEventHttp;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetSgCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StreamConstraintValidatorTest extends ConstraintValidationTest<StackGresStreamReview> {

  @Override
  protected AbstractConstraintValidator<StackGresStreamReview> buildValidator() {
    return new StreamConstraintValidator();
  }

  @Override
  protected StackGresStreamReview getValidReview() {
    return AdmissionReviewFixtures.stream().loadCreate().get();
  }

  @Override
  protected StackGresStreamReview getInvalidReview() {
    final StackGresStreamReview review = AdmissionReviewFixtures.stream().loadCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresStream.class, "spec", review);
  }

  @Test
  void nullSource_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().setSource(null);

    checkErrorCause(StackGresStreamSpec.class, "spec.source",
        review, NotNull.class, "must not be null");
  }

  @Test
  void nullSourceType_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getSource().setType(null);

    checkErrorCause(StackGresStreamSource.class, "spec.source.type",
        review, ValidEnum.class);
  }

  @Test
  void wrongSourceType_shouldFail() {

    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getSource().setType("test");

    checkErrorCause(StackGresStreamSource.class, "spec.source.type",
        "spec.source.type", review, ValidEnum.class);
  }

  @Test
  void nullSourceSgCluster_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getSource().setSgCluster(null);

    checkErrorCause(StackGresStreamSource.class,
        "spec.source.sgCluster",
        "isSgClusterPresent", review, AssertTrue.class);
  }

  @Test
  void nullSourceSgClusterName_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getSource().getSgCluster().setName(null);

    checkErrorCause(StackGresStreamSourceSgCluster.class, "spec.source.sgCluster.name",
        review, NotNull.class, "must not be null");
  }

  @Test
  void nullTarget_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().setTarget(null);

    checkErrorCause(StackGresStreamSpec.class, "spec.target",
        review, NotNull.class, "must not be null");
  }

  @Test
  void nullTargetType_shouldFail() {
    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getTarget().setType(null);

    checkErrorCause(StackGresStreamTarget.class, "spec.target.type",
        review, ValidEnum.class);
  }

  @Test
  void wrongTargetType_shouldFail() {

    StackGresStreamReview review = getValidReview();
    review.getRequest().getObject().getSpec().getTarget().setType("test");

    checkErrorCause(StackGresStreamTarget.class, "spec.target.type",
        "spec.target.type", review, ValidEnum.class);
  }

  @ParameterizedTest(name = "type: {0} section: {1}")
  @MethodSource("streamSourceTypesMatrix")
  void sourceTypeThatDontMatchSection_shouldFailWithMessage(StreamSourceType type, StreamSourceType section) {
    StackGresStreamReview review = getValidReview();
    StackGresStreamSpec spec = review.getRequest().getObject().getSpec();
    spec.getSource().setType(type.toString());

    spec.getSource().setSgCluster(null);

    switch (type) {
      case SGCLUSTER:
        var sgCluster = new StackGresStreamSourceSgCluster();
        sgCluster.setName("test");
        spec.getSource().setSgCluster(sgCluster);
        break;
      case POSTGRES:
        var postgres = new StackGresStreamSourcePostgres();
        postgres.setHost("test");
        spec.getSource().setPostgres(postgres);
        break;
      default:
        break;
    }

    switch (section) {
      case SGCLUSTER:
        var sgCluster = new StackGresStreamSourceSgCluster();
        sgCluster.setName("test");
        spec.getSource().setSgCluster(sgCluster);
        break;
      case POSTGRES:
        var postgres = new StackGresStreamSourcePostgres();
        postgres.setHost("test");
        spec.getSource().setPostgres(postgres);
        break;
      default:
        break;
    }

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "SGStream has invalid properties. type must match corresponding section.", 422);
  }

  private static Stream<Arguments> streamSourceTypesMatrix() {
    return Arrays.stream(StreamSourceType.values())
        .sorted()
        .collect(Collectors.toMap(Function.identity(),
            e -> Arrays.stream(StreamSourceType.values())
                .filter(p -> p != e)
                .collect(Collectors.toUnmodifiableList())))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .flatMap(map -> map.getValue().stream()
            .sorted()
            .map(val -> Arguments.of(map.getKey(), val)));
  }

  @ParameterizedTest(name = "type: {0} section: {1}")
  @MethodSource("streamSourceTypesSameMatrix")
  void sourceTypeThatMatchSection_shouldNotFail(StreamSourceType type, StreamSourceType section) {
    StackGresStreamReview review = getValidReview();
    StackGresStreamSpec spec = review.getRequest().getObject().getSpec();
    spec.getSource().setType(type.toString());

    spec.getSource().setSgCluster(null);

    switch (section) {
      case SGCLUSTER:
        var sgCluster = new StackGresStreamSourceSgCluster();
        sgCluster.setName("test");
        spec.getSource().setSgCluster(sgCluster);
        break;
      case POSTGRES:
        var postgres = new StackGresStreamSourcePostgres();
        postgres.setHost("test");
        spec.getSource().setPostgres(postgres);
        break;
      default:
        break;
    }
    assertDoesNotThrow(() -> validator.validate(review));
  }

  private static Stream<Arguments> streamSourceTypesSameMatrix() {
    return Arrays.stream(StreamSourceType.values())
        .sorted()
        .map(val -> Arguments.of(val, val));
  }

  @ParameterizedTest(name = "type: {0} section: {1}")
  @MethodSource("streamTargetTypesMatrix")
  void targetTypeThatDontMatchSection_shouldFailWithMessage(StreamTargetType type, StreamTargetType section) {
    StackGresStreamReview review = getValidReview();
    StackGresStreamSpec spec = review.getRequest().getObject().getSpec();
    spec.getTarget().setType(type.toString());

    spec.getTarget().setCloudEvent(null);

    switch (type) {
      case CLOUD_EVENT:
        var cloudEvent = new StackGresStreamTargetCloudEvent();
        cloudEvent.setBinding("http");
        cloudEvent.setFormat("json");
        cloudEvent.setHttp(new StackGresStreamTargetCloudEventHttp());
        cloudEvent.getHttp().setUrl("test");
        spec.getTarget().setCloudEvent(cloudEvent);
        break;
      case SGCLUSTER:
        var sgCluster = new StackGresStreamTargetSgCluster();
        sgCluster.setName("test");
        spec.getTarget().setSgCluster(sgCluster);
        break;
      default:
        break;
    }

    switch (section) {
      case CLOUD_EVENT:
        var cloudEvent = new StackGresStreamTargetCloudEvent();
        cloudEvent.setBinding("http");
        cloudEvent.setFormat("json");
        cloudEvent.setHttp(new StackGresStreamTargetCloudEventHttp());
        spec.getTarget().setCloudEvent(cloudEvent);
        break;
      case SGCLUSTER:
        var sgCluster = new StackGresStreamTargetSgCluster();
        sgCluster.setName("test");
        spec.getTarget().setSgCluster(sgCluster);
        break;
      default:
        break;
    }

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "SGStream has invalid properties. type must match corresponding section.", 422);
  }

  private static Stream<Arguments> streamTargetTypesMatrix() {
    return Arrays.stream(StreamTargetType.values())
        .sorted()
        .collect(Collectors.toMap(Function.identity(),
            e -> Arrays.stream(StreamTargetType.values())
                .filter(p -> p != e)
                .collect(Collectors.toUnmodifiableList())))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .flatMap(map -> map.getValue().stream()
            .sorted()
            .map(val -> Arguments.of(map.getKey(), val)));
  }

  @ParameterizedTest(name = "type: {0} section: {1}")
  @MethodSource("streamTargetTypesSameMatrix")
  void targetTypeThatMatchSection_shouldNotFail(StreamTargetType type, StreamTargetType section) {
    StackGresStreamReview review = getValidReview();
    StackGresStreamSpec spec = review.getRequest().getObject().getSpec();
    spec.getTarget().setType(type.toString());

    spec.getTarget().setCloudEvent(null);

    switch (section) {
      case CLOUD_EVENT:
        var cloudEvent = new StackGresStreamTargetCloudEvent();
        cloudEvent.setBinding("http");
        cloudEvent.setFormat("json");
        cloudEvent.setHttp(new StackGresStreamTargetCloudEventHttp());
        cloudEvent.getHttp().setUrl("test");
        spec.getTarget().setCloudEvent(cloudEvent);
        break;
      case SGCLUSTER:
        var sgCluster = new StackGresStreamTargetSgCluster();
        sgCluster.setName("test");
        spec.getTarget().setSgCluster(sgCluster);
        break;
      default:
        break;
    }
    assertDoesNotThrow(() -> validator.validate(review));
  }

  private static Stream<Arguments> streamTargetTypesSameMatrix() {
    return Arrays.stream(StreamTargetType.values())
        .sorted()
        .map(val -> Arguments.of(val, val));
  }

}
