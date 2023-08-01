/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ConfigReview extends AdmissionReview<StackGresConfig> {
}
