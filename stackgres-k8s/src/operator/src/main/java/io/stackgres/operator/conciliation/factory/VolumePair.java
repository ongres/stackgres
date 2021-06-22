/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import org.immutables.value.Value;

@Value.Immutable
public interface VolumePair {

  Volume getVolume();

  Optional<HasMetadata> getSource();

}
