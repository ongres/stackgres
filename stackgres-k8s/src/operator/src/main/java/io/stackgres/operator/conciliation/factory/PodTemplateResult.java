/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.immutables.value.Value;

@Value.Immutable
public interface PodTemplateResult {

  PodTemplateSpec getSpec();

  List<String> claimedVolumes();
}
