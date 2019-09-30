/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.AdmissionReview;

@ApplicationScoped
public class AlwaysSuccess implements ClusterValidator {

  @Override
  public void validate(AdmissionReview review) {
  }

}
