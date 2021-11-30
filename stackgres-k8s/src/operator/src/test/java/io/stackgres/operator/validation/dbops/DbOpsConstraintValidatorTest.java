/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepack;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsVacuum;
import io.stackgres.common.validation.ValidEnum;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operator.validation.ConstraintValidationTest;
import io.stackgres.operator.validation.ConstraintValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DbOpsConstraintValidatorTest extends ConstraintValidationTest<StackGresDbOpsReview> {

  @Override
  protected ConstraintValidator<StackGresDbOpsReview> buildValidator() {
    return new DbOpsConstraintValidator();
  }

  @Override
  protected StackGresDbOpsReview getValidReview() {
    return JsonUtil.readFromJson("dbops_allow_requests/valid_pgbench_creation.json",
        StackGresDbOpsReview.class);
  }

  @Override
  protected StackGresDbOpsReview getInvalidReview() {
    final StackGresDbOpsReview review = JsonUtil
        .readFromJson("dbops_allow_requests/valid_pgbench_creation.json",
            StackGresDbOpsReview.class);

    review.getRequest().getObject().setSpec(null);
    return review;
  }

  @Test
  void nullSpec_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().setSpec(null);

    checkNotNullErrorCause(StackGresDbOps.class, "spec", review);
  }

  @Test
  void nullSgCluster_shouldFail() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setSgCluster(null);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.sgCluster",
        review, NotEmpty.class);
  }

  @Test
  void nullOp_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setOp(null);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.op",
        review, ValidEnum.class);
  }

  @Test
  void wrongOp_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setOp("test");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.op",
        "spec.op", review, ValidEnum.class);
  }

  @Test
  void wrongRunAt_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setRunAt("2018-01-01 01:02:03");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.runAt",
        "spec.isRunAtValid", review, AssertTrue.class);
  }

  @Test
  void wrongTimeout_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setTimeout("10s");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.timeout",
        "spec.isTimeoutValid", review, AssertTrue.class);
  }

  @Test
  void negativeTimeout_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setTimeout("-PT1M");

    checkErrorCause(StackGresDbOpsSpec.class, "spec.timeout",
        "spec.isTimeoutValid", review, AssertTrue.class);
  }

  @Test
  void invalidLowMaxRetries_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMaxRetries(-1);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.maxRetries",
        review, Min.class);

  }

  @Test
  void invalidHighMaxRetries_shouldFail() {

    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().setMaxRetries(11);

    checkErrorCause(StackGresDbOpsSpec.class, "spec.maxRetries",
        review, Max.class);

  }

  @Test
  void invalidDuration_shouldFailWithMessage() {
    StackGresDbOpsReview review = getValidReview();
    review.getRequest().getObject().getSpec().getBenchmark().getPgbench().setDuration("P5M");

    ValidationFailed assertThrows = assertThrows(ValidationFailed.class,
        () -> validator.validate(review));

    assertEquals("SGDbOps has invalid properties. "
        + "duration must be positive and in ISO 8601 duration format: `PnDTnHnMn.nS`.",
        assertThrows.getMessage());
  }

  @ParameterizedTest(name = "op: {0} section: {1}")
  @MethodSource("dbOpsOperationsMatrix")
  void opThatDontMatchSection_shouldFailWithMessage(DbOpsOperation op, DbOpsOperation section) {
    StackGresDbOpsReview review = getValidReview();
    StackGresDbOpsSpec spec = review.getRequest().getObject().getSpec();
    spec.setOp(op.toString());

    spec.setBenchmark(null);
    spec.setVacuum(null);
    spec.setRepack(null);
    spec.setRestart(null);
    spec.setMajorVersionUpgrade(null);
    spec.setMinorVersionUpgrade(null);
    spec.setSecurityUpgrade(null);

    switch (section) {
      case BENCHMARK:
        var bench = new StackGresDbOpsBenchmark();
        bench.setType("pgbench");
        var pgbench = new StackGresDbOpsPgbench();
        pgbench.setDuration("P0DT0H10M0S");
        pgbench.setDatabaseSize("10GB");
        bench.setPgbench(pgbench);
        spec.setBenchmark(bench);
        break;
      case VACUUM:
        spec.setVacuum(new StackGresDbOpsVacuum());
        break;
      case REPACK:
        spec.setRepack(new StackGresDbOpsRepack());
        break;
      case RESTART:
        spec.setRestart(new StackGresDbOpsRestart());
        break;
      case MAJOR_VERSION_UPGRADE:
        var major = new StackGresDbOpsMajorVersionUpgrade();
        major.setPostgresVersion("14");
        major.setSgPostgresConfig("conf14");
        spec.setMajorVersionUpgrade(major);
        break;
      case MINOR_VERSION_UPGRADE:
        var minor = new StackGresDbOpsMinorVersionUpgrade();
        minor.setPostgresVersion("14.1");
        spec.setMinorVersionUpgrade(minor);
        break;
      case SECURITY_UPGRADE:
        spec.setSecurityUpgrade(new StackGresDbOpsSecurityUpgrade());
        break;
      default:
        break;
    }

    switch (op) {
      case BENCHMARK:
        ValidationUtils.assertValidationFailed(() -> validator.validate(review),
            "SGDbOps has invalid properties. benchmark section must be provided.", 422);
        break;
      case MAJOR_VERSION_UPGRADE:
        ValidationUtils.assertValidationFailed(() -> validator.validate(review),
            "SGDbOps has invalid properties. majorVersionUpgrade section must be provided.", 422);
        break;
      case MINOR_VERSION_UPGRADE:
        ValidationUtils.assertValidationFailed(() -> validator.validate(review),
            "SGDbOps has invalid properties. minorVersionUpgrade section must be provided.", 422);
        break;
      default:
        ValidationUtils.assertValidationFailed(() -> validator.validate(review),
            "SGDbOps has invalid properties. op must match corresponding section.", 422);
        break;
    }

  }

  private static Stream<Arguments> dbOpsOperationsMatrix() {
    return Arrays.stream(DbOpsOperation.values())
        .sorted()
        .collect(Collectors.toMap(Function.identity(),
            e -> Arrays.stream(DbOpsOperation.values())
                .filter(p -> p != e)
                .collect(Collectors.toUnmodifiableList())))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .flatMap(map -> map.getValue().stream()
            .sorted()
            .map(val -> Arguments.of(map.getKey(), val)));
  }

  @ParameterizedTest(name = "op: {0} section: {1}")
  @MethodSource("dbOpsOperationsSameMatrix")
  void opThatMatchSection_shouldNotFail(DbOpsOperation op, DbOpsOperation section) {
    StackGresDbOpsReview review = getValidReview();
    StackGresDbOpsSpec spec = review.getRequest().getObject().getSpec();
    spec.setOp(op.toString());

    spec.setBenchmark(null);
    spec.setVacuum(null);
    spec.setRepack(null);
    spec.setRestart(null);
    spec.setMajorVersionUpgrade(null);
    spec.setMinorVersionUpgrade(null);
    spec.setSecurityUpgrade(null);

    switch (section) {
      case BENCHMARK:
        var bench = new StackGresDbOpsBenchmark();
        bench.setType("pgbench");
        var pgbench = new StackGresDbOpsPgbench();
        pgbench.setDuration("P0DT0H10M0S");
        pgbench.setDatabaseSize("10GB");
        bench.setPgbench(pgbench);
        spec.setBenchmark(bench);
        break;
      case VACUUM:
        spec.setVacuum(new StackGresDbOpsVacuum());
        break;
      case REPACK:
        spec.setRepack(new StackGresDbOpsRepack());
        break;
      case RESTART:
        spec.setRestart(new StackGresDbOpsRestart());
        break;
      case MAJOR_VERSION_UPGRADE:
        var major = new StackGresDbOpsMajorVersionUpgrade();
        major.setPostgresVersion("14");
        major.setSgPostgresConfig("conf14");
        spec.setMajorVersionUpgrade(major);
        break;
      case MINOR_VERSION_UPGRADE:
        var minor = new StackGresDbOpsMinorVersionUpgrade();
        minor.setPostgresVersion("14.1");
        spec.setMinorVersionUpgrade(minor);
        break;
      case SECURITY_UPGRADE:
        spec.setSecurityUpgrade(new StackGresDbOpsSecurityUpgrade());
        break;
      default:
        break;
    }
    assertDoesNotThrow(() -> validator.validate(review));
  }

  private static Stream<Arguments> dbOpsOperationsSameMatrix() {
    return Arrays.stream(DbOpsOperation.values())
        .sorted()
        .map(val -> Arguments.of(val, val));
  }

}
