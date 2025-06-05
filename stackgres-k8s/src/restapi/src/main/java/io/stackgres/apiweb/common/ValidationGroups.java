/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.common;

import jakarta.validation.groups.Default;

public interface ValidationGroups {
  interface Post extends Default { }

  interface Put extends Default { }

  interface Delete extends Default { }
}
