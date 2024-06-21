package io.stackgres.stream.jobs.target.migration.postgres;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.google.common.io.CharStreams;
import org.jooq.lambda.Unchecked;

public enum SnapshotHelperQueries {
  IMPORT_DDL,
  STORE_CONSTRAINTS,
  STORE_PRIMARY_KEYS,
  STORE_INDEXES,
  DROP_CONSTRAINTS,
  DROP_INDEXES,
  RESTORE_CONSTRAINTS,
  RESTORE_INDEXES,
  AUTOVACUUM_DISABLE,
  AUTOVACUUM_RESET;

  public String readSql() {
    String queryType = name().toLowerCase(Locale.ENGLISH);
    return Unchecked.supplier(() -> {
      try (
          InputStream is = getClass().getClassLoader().getResourceAsStream("/postgresql/" + queryType + ".sql");
          InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
          ) {
        return CharStreams.toString(inputStreamReader);
      }
    }).get();
  }

}
