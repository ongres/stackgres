/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;

@ApplicationScoped
public class VolumeSizeValidator implements ClusterValidator {

  private static final String VOLUME_SIZE_EXPR =
      "^\\d+[GPMTK]i$|^\\d+[GPMTK]$|^\\d+[Ee]\\d+$|^\\d+$";

  private static final Pattern VOLUME_SIZE_PATTERN = Pattern.compile(VOLUME_SIZE_EXPR);

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    String volumeSize = review.getRequest().getObject().getSpec().getVolumeSize();

    Matcher m = VOLUME_SIZE_PATTERN.matcher(volumeSize);

    if (!m.matches()) {
      throw new ValidationFailed("Invalid volume size " + volumeSize);
    }

  }
}
