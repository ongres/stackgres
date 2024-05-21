/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.stream;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface StreamMutator extends Mutator<StackGresStream, StackGresStreamReview> {

}
