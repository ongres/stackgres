package io.stackgres.common.fixture;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatus;
import io.stackgres.testutil.JsonUtil;

public class StackGresDbOpsPgbenchStatusFixture {

  public StackGresDbOpsPgbenchStatus build() {
    return JsonUtil.readFromJson("dbops/pgbench_status.json", StackGresDbOpsPgbenchStatus.class);
  }

}
