/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgstat;

import java.util.List;

public record PostgresStatDto(int resultSetIndex, List<String> fields) { }
