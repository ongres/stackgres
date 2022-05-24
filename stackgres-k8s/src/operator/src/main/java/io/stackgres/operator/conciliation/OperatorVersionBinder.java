/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import io.stackgres.common.StackGresVersion;

@Qualifier
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperatorVersionBinder {

  @Nonbinding StackGresVersion startAt() default StackGresVersion.V_1_0;

  @Nonbinding StackGresVersion stopAt() default StackGresVersion.V_1_3;

}
