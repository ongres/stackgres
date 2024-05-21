/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgstream.StackGresStream;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StreamLabelMapper implements LabelMapperForStream {

  @Override
  public String appName() {
    return StackGresContext.STREAM_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresStream resource) {
    return getKeyPrefix(resource) + StackGresContext.STREAM_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresStream resource) {
    return getKeyPrefix(resource) + StackGresContext.STREAM_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresStream resource) {
    return getKeyPrefix(resource) + StackGresContext.STREAM_UID_KEY;
  }

}
