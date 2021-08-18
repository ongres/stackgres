/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.converter;

public interface DtoConverter<T extends Object, R extends Object> {

  R from(T source);
}
