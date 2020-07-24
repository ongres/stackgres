/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.nativeimage;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.jooq.impl.DefaultRecordMapper", innerClass = "ProxyMapper")
public final class DefaultRecordMapperSubstitutions<E> {
  @Substitute
  private E proxy() { //NOPMD
    return null;
  }
}
