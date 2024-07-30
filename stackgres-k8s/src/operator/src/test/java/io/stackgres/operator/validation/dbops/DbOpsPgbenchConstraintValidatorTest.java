/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractConstraintValidator;
import io.stackgres.operator.validation.ConstraintValidationTest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.Test;

class DbOpsPgbenchConstraintValidatorTest extends ConstraintValidationTest<StackGresDbOpsReview> {

  @Override
  protected AbstractConstraintValidator<StackGresDbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected StackGresDbOpsReview getValidReview() {
    return AdmissionReviewFixtures.dbOps().loadPgbenchCreate().get();
  }

  @Override
  protected StackGresDbOpsReview getInvalidReview() {
    final StackGresDbOpsReview review = AdmissionReviewFixtures.dbOps().loadPgbenchCreate().get();

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
  void nullPgbenchMode_shouldPass() throws Exception {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setMode(null);

    validator.validate(review);
  }

  @Test
  void wrongPgbenchMode_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setMode("test");

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.mode",
        "spec.benchmark.pgbench.mode", review, ValidEnum.class);
  }

  @Test
  void nullBenchmarkPgbench_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().setPgbench(null);

    checkErrorCause(StackGresDbOpsBenchmark.class, "spec.benchmark.pgbench",
        "spec.benchmark.isPgbenchSectionProvided", review, AssertTrue.class);
  }

  @Test
  void nullBenchmarkPgbenchDatabaseSize_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setDatabaseSize(null);

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.databaseSize",
        review, NotEmpty.class);
  }

  @Test
  void wrongDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setDuration("10s");

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.duration",
        "spec.benchmark.pgbench.isDurationValid", review, AssertTrue.class);
  }

  @Test
  void negativeDuration_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setDuration("-PT1M");

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.duration",
        "spec.benchmark.pgbench.isDurationValid", review, AssertTrue.class);
  }

  @Test
  void invalidLowConcurrentClients_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setConcurrentClients(0);

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.concurrentClients",
        review, Min.class);
  }

  @Test
  void invalidLowThreads_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setThreads(0);

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.threads",
        review, Min.class);
  }

  @Test
  void samplingSgDbOpsIsRequiredWhenModeIsReplay_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setMode("replay");
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setSamplingSgDbOps(null);

    checkErrorCause(StackGresDbOpsPgbench.class, "spec.benchmark.pgbench.samplingSGDbOps",
        "spec.benchmark.pgbench.isSamplingSgDbOpsValid", review, AssertTrue.class);
  }

}
