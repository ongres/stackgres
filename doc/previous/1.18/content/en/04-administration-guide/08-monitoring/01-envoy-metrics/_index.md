---
title: Envoy
weight: 1
url: /administration/monitoring/envoy
aliases: [ /monitoring/metrics/envoy ]
description: Contains details about the metrics collected by the envoy proxy with the Postgres filter.
---

The list below contains details about the metrics enabled by the envoy proxy:

|item| metric group           | metric                 | type        | description                                                                                        |
|----|------------------------|------------------------|-------------|----------------------------------------------------------------------------------------------------|
| 1  | envoy_postgres_ingress |                        |             |                                                                                                    |
|    |                        | errors      	       | Counter	 | Number of times the server replied with ERROR message                                              |
|    |                        | errors_error	       | Counter	 | Number of times the server replied with ERROR message with ERROR severity                          |
|    |                        | errors_fatal	       | Counter	 | Number of times the server replied with ERROR message with FATAL severity                          |
|    |                        | errors_panic	       | Counter	 | Number of times the server replied with ERROR message with PANIC severity                          |
|    |                        | errors_unknown	       | Counter	 | Number of times the server replied with ERROR message but the decoder could not parse it           |
|    |                        | messages	           | Counter	 | Total number of messages processed by the filter                                                   |
|    |                        | messages_backend	   | Counter	 | Total number of backend messages detected by the filter                                            |
|    |                        | messages_frontend	   | Counter	 | Number of frontend messages detected by the filter                                                 |
|    |                        | messages_unknown	   | Counter	 | Number of times the filter successfully decoded a message but did not know what to do with it      |
|    |                        | sessions	           | Counter	 | Total number of successful logins                                                                  |
|    |                        | sessions_encrypted	   | Counter	 | Number of times the filter detected encrypted sessions                                             |
|    |                        | sessions_unencrypted   | Counter	 | Number of messages indicating unencrypted successful login                                         |
|    |                        | statements	           | Counter	 | Total number of SQL statements                                                                     |
|    |                        | statements_delete	   | Counter	 | Number of DELETE statements                                                                        |
|    |                        | statements_insert	   | Counter	 | Number of INSERT statements                                                                        |
|    |                        | statements_select	   | Counter	 | Number of SELECT statements                                                                        |
|    |                        | statements_update	   | Counter	 | Number of UPDATE statements                                                                        |
|    |                        | statements_other	   | Counter	 | Number of statements other than DELETE, INSERT, SELECT or UPDATE                                   |
|    |                        | statements_parsed      | Counter	 | Number of SQL queries parsed successfully                                                          |
|    |                        | statements_parse_error | Counter	 | Number of SQL queries not parsed successfully                                                      |
|    |                        | transactions	       | Counter	 | Total number of SQL transactions                                                                   |
|    |                        | transactions_commit	   | Counter	 | Number of COMMIT transactions                                                                      |
|    |                        | transactions_rollback  | Counter	 | Number of ROLLBACK transactions                                                                    |
|    |                        | notices	               | Counter	 | Total number of NOTICE messages                                                                    |
|    |                        | notices_notice	       | Counter	 | Number of NOTICE messages with NOTICE subtype                                                      |
|    |                        | notices_log	           | Counter	 | Number of NOTICE messages with LOG subtype                                                         |
|    |                        | notices_warning	       | Counter	 | Number of NOTICE messages with WARNING severity                                                    |
|    |                        | notices_debug	       | Counter	 | Number of NOTICE messages with DEBUG severity                                                      |
|    |                        | notices_info	       | Counter	 | Number of NOTICE messages with INFO severity                                                       |
|    |                        | notices_unknown	       | Counter	 | Number of NOTICE messages which could not be recognized                                            |

Check the [filter documentation](https://www.envoyproxy.io/docs/envoy/latest/configuration/listeners/network_filters/postgres_proxy_filter) page for more details.
