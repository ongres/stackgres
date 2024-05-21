/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.cloudevent;

import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.stream.jobs.StreamTargetOperation;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(visibility = ImplementationVisibility.PACKAGE)
public interface StreamEventState {

  String getStreamName();

  StreamTargetOperation getStreamOperation();

  String getNamespace();

  StreamSourceType getSourceType();

  StreamTargetType getTargetType();

  class Builder extends ImmutableStreamEventState.Builder {
  }

  static Builder builder() {
    return new Builder();
  }

}
