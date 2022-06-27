/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.util.List;

import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import org.jetbrains.annotations.NotNull;

public interface JsonPatchMutator<T extends AdmissionReview<?>> extends JsonPatchMutatorUtil {

  @NotNull
  List<@NotNull JsonPatchOperation> mutate(@NotNull T review);

}
