/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;

@RegisterForReflection
public class StackGresClusterReview extends AdmissionReview<StackGresCluster> {

  private static final long serialVersionUID = 1L;

}
