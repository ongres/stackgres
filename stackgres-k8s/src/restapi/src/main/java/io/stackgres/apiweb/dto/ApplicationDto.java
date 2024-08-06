/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@JsonDeserialize(builder = ApplicationDto.Builder.class)
@Value.Immutable
@Value.Style(visibility = ImplementationVisibility.PACKAGE)
public interface ApplicationDto {

  String name();

  String publisher();

  class Builder extends ImmutableApplicationDto.Builder {
  }

  static Builder builder() {
    return new Builder();
  }

}
