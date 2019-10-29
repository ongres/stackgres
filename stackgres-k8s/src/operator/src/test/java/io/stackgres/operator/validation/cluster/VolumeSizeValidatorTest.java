/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operatorframework.ValidationFailed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class VolumeSizeValidatorTest {

  private VolumeSizeValidator validator = new VolumeSizeValidator();

  private StackgresClusterReview defaultReview;

  @BeforeEach
  public void setUp() {

    defaultReview = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

  }

  private void setVolumeSize(String volumeSize) {
    defaultReview.getRequest().getObject().getSpec().setVolumeSize(volumeSize);
  }

  private void testWithVolumeSize(String volumeSize) throws ValidationFailed {
    setVolumeSize(volumeSize);
    validator.validate(defaultReview);
  }

  private void testForFailure(String volumeSize) {
    setVolumeSize(volumeSize);
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(defaultReview);
    });

    assertEquals("Invalid volume size " + volumeSize, ex.getResult().getMessage());
  }

  @Test
  public void givenVolumeSizeInBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("128974848");

  }

  @Test
  public void givenVolumeSizeInKiloBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129K");

  }

  @Test
  public void givenVolumeSizeInKibiBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129Ki");

  }

  @Test
  public void givenVolumeSizeInMegaBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129M");

  }

  @Test
  public void givenVolumeSizeInMebiBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129Mi");

  }

  @Test
  public void givenVolumeSizeInGigaBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129G");

  }

  @Test
  public void givenVolumeSizeInGibiBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129Gi");

  }

  @Test
  public void givenVolumeSizeInTeraBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129T");

  }

  @Test
  public void givenVolumeSizeInTebiBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129Ti");

  }

  @Test
  public void givenVolumeSizeInPetaBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129P");

  }

  @Test
  public void givenVolumeSizeInPebiBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129P");

  }

  @Test
  public void givenVolumeSizeInExponentialBytes_shouldNotFail() throws ValidationFailed {

    testWithVolumeSize("129e6");
    testWithVolumeSize("129E6");

  }

  @Test
  public void givenVolumeSizeInBadFormat_shouldNotFail() {

    testForFailure("126.0M");
    testForFailure("126,0M");
    testForFailure("-126M");
    testForFailure("m126");
    testForFailure("123,3");

  }


}
