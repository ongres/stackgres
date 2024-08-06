/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.application.bbfcompass;

import java.io.InputStream;

public record FileUpload(String filename, InputStream content) {
}
