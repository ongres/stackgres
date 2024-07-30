/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSampling;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;

class DbOpsSamplingConstraintValidatorTest extends ConstraintValidationTest<StackGresDbOpsReview> {

  @Override
  protected AbstractConstraintValidator<StackGresDbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected StackGresDbOpsReview getValidReview() {
    return AdmissionReviewFixtures.dbOps().loadSamplingCreate().get();
  }

  @Override
  protected StackGresDbOpsReview getInvalidReview() {
    final StackGresDbOpsReview review = AdmissionReviewFixtures.dbOps().loadSamplingCreate().get();

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullBenchmark_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setBenchmark(null);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.benchmark",
        "spec.isBenchmarkSectionProvided", review, AssertTrue.class);
  }

  @Test
  void nullBenchmarkType_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().setType(null);

    checkErrorCause(StackGresDbOpsBenchmark.class, "spec.benchmark.type",
        "spec.benchmark.type", review, ValidEnum.class);
  }

  @Test
  void wrongBenchmarkType_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().setType("test");

    checkErrorCause(StackGresDbOpsBenchmark.class, "spec.benchmark.type",
        "spec.benchmark.type", review, ValidEnum.class);
  }

  @Test
  void nullSamplingMode_shouldPass() throws Exception {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setMode(null);

    validator.validate(review);
  }

  @Test
  void wrongSamplingMode_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setMode("test");

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.mode",
        "spec.benchmark.sampling.mode", review, ValidEnum.class);
  }

  @Test
  void nullBenchmarkSampling_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().setSampling(null);

    checkErrorCause(StackGresDbOpsBenchmark.class, "spec.benchmark.sampling",
        "spec.benchmark.isSamplingSectionProvided", review, AssertTrue.class);
  }

  @Test
  void wrongTopQueriesCollectDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setTopQueriesCollectDuration("10s");

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.topQueriesCollectDuration",
        "spec.benchmark.sampling.isTopQueriesCollectDurationValid", review, AssertTrue.class);
  }

  @Test
  void negativeTopQueriesCollectDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setTopQueriesCollectDuration("-PT1M");

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.topQueriesCollectDuration",
        "spec.benchmark.sampling.isTopQueriesCollectDurationValid", review, AssertTrue.class);
  }

  @Test
  void wrongSamplingDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setSamplingDuration("10s");

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.samplingDuration",
        "spec.benchmark.sampling.isSamplingDurationValid", review, AssertTrue.class);
  }

  @Test
  void negativeSamplingDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setSamplingDuration("-PT1M");

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.samplingDuration",
        "spec.benchmark.sampling.isSamplingDurationValid", review, AssertTrue.class);
  }

  @Test
  void invalidLowTopQueriesPercentile_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setTopQueriesPercentile(-1);

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.topQueriesPercentile",
        review, Min.class);
  }

  @Test
  void invalidHighTopQueriesPercentile_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setTopQueriesPercentile(100);

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.topQueriesPercentile",
        review, Max.class);
  }

  @Test
  void invalidLowTopQueriesMin_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getSampling().setTopQueriesMin(-1);

    checkErrorCause(StackGresDbOpsSampling.class, "spec.benchmark.sampling.topQueriesMin",
        review, Min.class);
  }

}
