/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.validators;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.validation.AdmissionReview;

@ApplicationScoped
public class AlwaysSuccess implements Validator {

  @Override
  public void validate(AdmissionReview review) {
  }

}
