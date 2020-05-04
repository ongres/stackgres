/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.substratevm;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.jooq.impl.DefaultRecordMapper", innerClass = "ProxyMapper")
final class ProxyMapperSubstitute {
  @Substitute
  private Object proxy() { // NOPMD
    throw new UnsupportedOperationException("Can't work with GraalVM native");
  }
}
