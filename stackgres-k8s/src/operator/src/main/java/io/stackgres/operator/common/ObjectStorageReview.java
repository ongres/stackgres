/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@Buildable
public class ObjectStorageReview extends AdmissionReview<StackGresObjectStorage> {
}
