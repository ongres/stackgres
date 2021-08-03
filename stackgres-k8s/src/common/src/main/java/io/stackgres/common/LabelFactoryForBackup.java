/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;

public interface LabelFactoryForBackup<T extends CustomResource<?, ?>>
    extends LabelFactory<T> {

  Map<String, String> backupPodLabels(T resource);

  LabelMapperForBackup<T> labelMapper();

}
