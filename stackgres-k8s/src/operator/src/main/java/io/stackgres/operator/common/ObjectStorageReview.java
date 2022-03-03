/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.io.Serial;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

@RegisterForReflection
public class ObjectStorageReview extends AdmissionReview<StackGresObjectStorage> {

  @Serial
  private static final long serialVersionUID = 1L;
}
