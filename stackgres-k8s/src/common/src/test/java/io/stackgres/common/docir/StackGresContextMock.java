/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import io.stackgres.common.component.StackGresContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Singleton
@Alternative
@Priority(1)
public class StackGresContextMock extends StackGresContext {

  public static final StackGresContext CONTEXT =
      new StackGresContextMock();

  public StackGresContextMock() {
    super(DocirMetadataManagerMock.INSTANCE);
  }

}
