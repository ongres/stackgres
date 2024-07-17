/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.common;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.immutables.value.Value.Immutable;

@Immutable
public abstract class StackGresStreamContext
    implements ResourceHandlerContext {

  public abstract StackGresStream getStream();

}
