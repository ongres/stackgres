/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgstream.StackGresStream;

public interface LabelMapperForStream
    extends LabelMapper<StackGresStream> {

  default String streamKey(StackGresStream resource) {
    return getKeyPrefix(resource) + StackGresContext.STREAM_KEY;
  }

}
