/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

public abstract class AbstractAnnotationMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements DefaultAnnotationMutator<R, T> {

}
