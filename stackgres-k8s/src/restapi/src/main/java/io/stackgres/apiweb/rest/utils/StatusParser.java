/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import io.fabric8.kubernetes.api.model.Status;

public interface StatusParser {

  String parseDetails(Status status);

  String[] parseFields(Status status);
}
