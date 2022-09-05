/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import static io.stackgres.operator.conciliation.factory.PostgresExtensionMounts.PG_EXTRA_LIB_PATH_FORMAT;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;

@ApplicationScoped
@ProviderName(VolumeMountProviderName.MAJOR_VERSION_UPGRADE)
public class MajorVersionUpgradeMounts implements VolumeMountsProvider<PostgresContainerContext> {

  private static final String PG_LIB_PATH_FORMAT = "/usr/lib/postgresql/%s/lib";
  private static final String PG_RELOCATED_LIB64_PATH_FORMAT =
      "/var/lib/postgresql/relocated/%s/usr/lib64";

  @ProviderName(VolumeMountProviderName.POSTGRES_EXTENSIONS)
  @Inject
  PostgresExtensionMounts postgresExtensionMounts;

  @Override
  public List<VolumeMount> getVolumeMounts(PostgresContainerContext context) {

    String oldPgVersion = context.getOldPostgresVersion().orElseThrow();
    String oldMajorVersion = context.getOldMajorVersion().orElseThrow();
    String oldBuildMajorVersion = context.getOldImageBuildMajorVersion().orElseThrow();

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresExtensionMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/lib/postgresql/%s/bin", oldPgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/lib/postgresql/%s/bin",
                        oldPgVersion,
                        oldPgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format(PG_EXTRA_LIB_PATH_FORMAT, oldPgVersion))
                .withSubPath(
                    String.format("extensions/%s/%s/usr/lib64",
                        oldMajorVersion,
                        oldBuildMajorVersion)
                )
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format(PG_LIB_PATH_FORMAT, oldPgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/lib/postgresql/%s/lib",
                        oldPgVersion,
                        oldPgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/share/postgresql/%s", oldPgVersion))
                .withSubPath(
                    String.format("relocated/%s/usr/share/postgresql/%s",
                        oldPgVersion,
                        oldPgVersion))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(String.format("/usr/share/postgresql/%s/extension", oldPgVersion))
                .withSubPath(
                    String.format("extensions/%s/%s/usr/share/postgresql/%s/extension",
                        oldMajorVersion,
                        oldBuildMajorVersion,
                        oldMajorVersion))
                .build()
        ).build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(PostgresContainerContext context) {

    final String postgresVersion = context.getPostgresVersion();
    final String oldPostgresVersion = context.getOldPostgresVersion()
        .orElseThrow();
    return ImmutableList.<EnvVar>builder()
        .addAll(postgresExtensionMounts.getDerivedEnvVars(context))
        .add(new EnvVarBuilder()
                .withName("TARGET_PG_LIB_PATH")
                .withValue(String.format(PG_LIB_PATH_FORMAT, postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("TARGET_PG_LIB64_PATH")
                .withValue(String.format(PG_RELOCATED_LIB64_PATH_FORMAT, postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("TARGET_PG_EXTRA_LIB_PATH")
                .withValue(String.format(PG_EXTRA_LIB_PATH_FORMAT, postgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_LIB_PATH")
                .withValue(String.format(PG_LIB_PATH_FORMAT, oldPostgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_LIB64_PATH")
                .withValue(String.format(PG_RELOCATED_LIB64_PATH_FORMAT, oldPostgresVersion))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_EXTRA_LIB_PATH")
                .withValue(String.format(PG_EXTRA_LIB_PATH_FORMAT, oldPostgresVersion))
                .build())
        .build();
  }
}
