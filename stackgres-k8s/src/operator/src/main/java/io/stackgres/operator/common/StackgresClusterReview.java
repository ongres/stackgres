/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.AdmissionReview;

@RegisterForReflection
public class StackgresClusterReview extends AdmissionReview<StackGresCluster> {

  private static final long serialVersionUID = 1L;

}
