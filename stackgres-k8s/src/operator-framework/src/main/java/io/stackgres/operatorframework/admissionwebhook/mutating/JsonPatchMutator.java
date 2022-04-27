/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.List;

import com.github.fge.jsonpatch.JsonPatchOperation;

public interface JsonPatchMutator<T> extends JsonPatchMutatorUtil {

  List<JsonPatchOperation> mutate(T review);

}
