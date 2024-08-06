/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

public interface ClusterStats {

  String getCpuRequested();

  void setCpuRequested(String cpuRequested);

  String getCpuFound();

  void setCpuFound(String cpuFound);

  String getCpuPsiAvg10();

  void setCpuPsiAvg10(String cpuPsiAvg10);

  String getCpuPsiAvg60();

  void setCpuPsiAvg60(String cpuPsiAvg60);

  String getCpuPsiAvg300();

  void setCpuPsiAvg300(String cpuPsiAvg300);

  String getCpuPsiTotal();

  void setCpuPsiTotal(String cpuPsiTotal);

  String getMemoryRequested();

  void setMemoryRequested(String memoryRequested);

  String getMemoryFound();

  void setMemoryFound(String memoryFound);

  String getMemoryUsed();

  void setMemoryUsed(String memoryUsed);

  String getMemoryPsiAvg10();

  void setMemoryPsiAvg10(String memoryPsiAvg10);

  String getMemoryPsiAvg60();

  void setMemoryPsiAvg60(String memoryPsiAvg60);

  String getMemoryPsiAvg300();

  void setMemoryPsiAvg300(String memoryPsiAvg300);

  String getMemoryPsiTotal();

  void setMemoryPsiTotal(String memoryPsiTotal);

  String getMemoryPsiFullAvg10();

  void setMemoryPsiFullAvg10(String memoryPsiFullAvg10);

  String getMemoryPsiFullAvg60();

  void setMemoryPsiFullAvg60(String memoryPsiFullAvg60);

  String getMemoryPsiFullAvg300();

  void setMemoryPsiFullAvg300(String memoryPsiFullAvg300);

  String getMemoryPsiFullTotal();

  void setMemoryPsiFullTotal(String memoryPsiFullTotal);

  String getDiskRequested();

  void setDiskRequested(String diskRequested);

  String getDiskFound();

  void setDiskFound(String diskFound);

  String getDiskUsed();

  void setDiskUsed(String diskUsed);

  String getDiskPsiAvg10();

  void setDiskPsiAvg10(String diskPsiAvg10);

  String getDiskPsiAvg60();

  void setDiskPsiAvg60(String diskPsiAvg60);

  String getDiskPsiAvg300();

  void setDiskPsiAvg300(String diskPsiAvg300);

  String getDiskPsiTotal();

  void setDiskPsiTotal(String diskPsiTotal);

  String getDiskPsiFullAvg10();

  void setDiskPsiFullAvg10(String diskPsiFullAvg10);

  String getDiskPsiFullAvg60();

  void setDiskPsiFullAvg60(String diskPsiFullAvg60);

  String getDiskPsiFullAvg300();

  void setDiskPsiFullAvg300(String diskPsiFullAvg300);

  String getDiskPsiFullTotal();

  void setDiskPsiFullTotal(String diskPsiFullTotal);

  String getAverageLoad1m();

  void setAverageLoad1m(String averageLoad1m);

  String getAverageLoad5m();

  void setAverageLoad5m(String averageLoad5m);

  String getAverageLoad10m();

  void setAverageLoad10m(String averageLoad10m);

  String getConnections();

  void setConnections(String connections);

  List<KubernetesPod> getPods();

  void setPods(List<KubernetesPod> pods);

  Integer getPodsReady();

  void setPodsReady(Integer podsReady);

}
