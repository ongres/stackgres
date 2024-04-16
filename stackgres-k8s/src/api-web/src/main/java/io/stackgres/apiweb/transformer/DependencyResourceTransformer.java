/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.apiweb.dto.ResourceDto;
import org.jetbrains.annotations.Nullable;

public interface DependencyResourceTransformer
    <T extends ResourceDto, R extends HasMetadata> {

  R toCustomResource(@Nonnull T resource, @Nullable R originalResource);

  T toResource(@Nonnull R customResource, @Nonnull List<String> clusters);

}
