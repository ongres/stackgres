/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

public interface Mutator<R extends HasMetadata, T extends AdmissionReview<R>> {

  R mutate(T review, R resource);

}
