/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgstream.StackGresStream;

public interface LabelFactoryForStream
    extends LabelFactory<StackGresStream> {

  Map<String, String> streamPodLabels(StackGresStream resource);

  @Override
  LabelMapperForStream labelMapper();

}
