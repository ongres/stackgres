/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;

public interface LabelFactoryForDbOps
    extends LabelFactory<StackGresDbOps> {

  Map<String, String> dbOpsPodLabels(StackGresDbOps resource);

  @Override
  LabelMapperForDbOps labelMapper();

}
