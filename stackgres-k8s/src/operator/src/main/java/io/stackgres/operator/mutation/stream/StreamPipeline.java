/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.stream;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamPipeline extends AbstractMutationPipeline<StackGresStream, StackGresStreamReview> {

  @Inject
  public StreamPipeline(
      @Any Instance<StreamMutator> mutators) {
    super(mutators);
  }

}
