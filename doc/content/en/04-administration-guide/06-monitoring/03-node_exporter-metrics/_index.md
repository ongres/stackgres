---
title: node_exporter
weight: 3
url: monitoring/metrics/node_exporter
description: Contains details about the metrics collected by the node_exporter.
showToc: true
---

The next table contains details about the metrics collected by the node_exporter.

## File system metrics

|item| metric group     | metric       | type  | description                                                              |
|----|------------------|--------------|-------|--------------------------------------------------------------------------|
| 1  | node_filesystem  |              |       |                                                                          |
|    |                  | device       | LABEL | Device of the filesystem                                                 |
|    |                  | mountpoint   | LABEL | Mount point of the filesystem                                            |
|    |                  | fstype       | LABEL | The type of filesystem                                                   |
|    |                  | size_bytes   | GAUGE | Filesystem size in bytes                                                 |
|    |                  | avail_bytes  | GAUGE | Filesystem space available to non-root users in bytes                    |
|    |                  | files        | GAUGE | Filesystem total file nodes                                              |
|    |                  | files_free   | GAUGE | Filesystem total free file nodes                                         |
|    |                  | device_error | GAUGE | Whether an error occurred while getting statistics for the given device  |


