/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream.context;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.ContextPipeline;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

@ApplicationScoped
public class StreamContextPipeline
    extends ContextPipeline<StackGresStream, Builder> {

  public StreamContextPipeline(Instance<ContextAppender<StackGresStream, Builder>> contextAppenders) {
    super(contextAppenders);
  }

}
