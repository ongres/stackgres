var ClusterInfo = Vue.component("ClusterInfo", {
	template: `
		<div id="cluster-info">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">SGClusters</router-link>
					</li>
					<li>
						<router-link :to="'/admin/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status">{{ $route.params.name }}</router-link>
					</li>
					<li>
						Configuration
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', currentNamespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('create','sgclusters',$route.params.namespace)" :to="'/admin/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" v-on:click="deleteCRD('sgcluster', currentNamespace, $route.params.name, '/admin/overview/'+currentNamespace)" :class="'/overview/'+currentNamespace">Delete Cluster</a>
					</div>		
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/admin/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/admin/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="iCan('list','sgbackups',$route.params.namespace)">
						<router-link :to="'/admin/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && cluster.data.spec.hasOwnProperty('distributedLogs')">
						<router-link :to="'/admin/cluster/logs/'+$route.params.namespace+'/'+$route.params.name" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="cluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/admin/cluster/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>

			<div class="content">
				<h2>Cluster Details</h2>
				<table class="clusterConfig">
					<thead>
						<th></th>
						<th></th>
						<th></th>
						<th></th>
					</thead>
					<tbody>
						<tr>
							<td class="label">
								Postgres Version
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresVersion')"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.postgresVersion }}</td>
						</tr>
						<tr>
							<td class="label">
								Instances
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.instances')"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.instances }}</td>
						</tr>
						<tr>
							<td class="label">
								Instance Profile
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.sgInstanceProfile')"></span>
							</td>
							<td colspan="3" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
								<router-link :to="'/admin/profiles/'+currentNamespace+'/'+cluster.data.spec.sgInstanceProfile">
									{{ cluster.data.spec.sgInstanceProfile }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
							</td>
						</tr>
						<tr>
							<td class="label" rowspan="3">
								Pods
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods')"></span>
							</td>
							<td class="label" :rowspan="Object.keys(cluster.data.spec.pods.persistentVolume).length">
								Persistent Volume
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.persistentVolume')"></span>
							</td>
							<td class="label">
								Volume Size
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.persistentVolume.size')"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.pods.persistentVolume.storageClass !== 'undefined')">
							<td class="label">
								Storage Class
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.persistentVolume.storageClass')"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr>
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.configurations.sgPoolingConfig')"></span>
							</td>
							<td colspan="2">
								<template v-if="(typeof cluster.data.spec.configurations.sgPoolingConfig !== 'undefined')">
									ON
								</template>
								<template v-else>
									OFF
								</template>
							</td>
						</tr>
						<tr>
							<td class="label">
								Metrics Exporter
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.disableMetricsExporter')"></span>
							</td>
							<td colspan="2">
								<template v-if="!cluster.data.spec.pods.disableMetricsExporter">
									ON
								</template>
								<template v-else>
									OFF
								</template>
							</td>
						</tr>
						<tr>
							<td class="label" :rowspan="Object.keys(cluster.data.spec.configurations).length">
								Configurations
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.configurations')"></span>
							</td>
							<td class="label">
								Postgres
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.configurations.sgPostgresConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/admin/configurations/postgres/'+currentNamespace+'/'+cluster.data.spec.configurations.sgPostgresConfig">
									{{ cluster.data.spec.configurations.sgPostgresConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>								
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgPoolingConfig !== 'undefined')">
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.configurations.sgPoolingConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/admin/configurations/connectionpooling/'+currentNamespace+'/'+cluster.data.spec.configurations.sgPoolingConfig">
									{{ cluster.data.spec.configurations.sgPoolingConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>	
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgBackupConfig !== 'undefined')">
							<td class="label">
								Managed Backups
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.configurations.sgBackupConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/admin/configurations/backup/'+currentNamespace+'/'+cluster.data.spec.configurations.sgBackupConfig">
									{{ cluster.data.spec.configurations.sgBackupConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
							</td>
						</tr>
						<tr>
							<td class="label">
								Prometheus Autobind
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.prometheusAutobind')"></span>
							</td>
							<td colspan="3">
								<template v-if="(typeof cluster.data.spec.prometheusAutobind !== 'undefined')">
									ON
								</template>
								<template v-else>
									OFF
								</template>
							</td>
						</tr>
						<tr v-if="typeof cluster.data.spec.nonProductionOptions !== 'undefined'">
							<td class="label">
								Non-Production Settings
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.nonProductionOptions')"></span>
							</td>
							<td class="label">
								Cluster Pod Anti Affinity
								<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.nonProductionOptions.disableClusterPodAntiAffinity')"></span>
							</td>
							<td colspan="2">
								<template v-if="typeof cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity !== 'undefined'">
									OFF
								</template>
								<template v-else>
									ON
								</template>
							</td>
						</tr>
						<template v-if="hasProp(cluster, 'data.spec.initialData.restore')">
							<template v-for="(backup, index) in backups">
								<template v-if="backup.data.metadata.uid == cluster.data.spec.initialData.restore.fromBackup">
									<tr>
										<td class="label" rowspan="3">
											Initial Data
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData')"></span>
										</td>
										<td class="label">
											Download Disk Concurrency
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData.restore.downloadDiskConcurrency')"></span>
										</td>
										<td colspan="2">
											{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}
										</td>
									</tr>
									<tr>
										<td class="label" rowspan="2">
											Restored from Backup
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData.restore.fromBackup')"></span>
										</td>
										<td class="label">
											Backup UID
										</td>
										<td>
											<router-link :to="'/admin/cluster/backups/'+$route.params.namespace+'/'+backup.data.spec.sgCluster+'/'+backup.data.metadata.uid"> 
												{{ cluster.data.spec.initialData.restore.fromBackup }}
												<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
											</router-link>
										</td>
									</tr>
									<tr>
										<td class="label">
											Date
										</td>
										<td class="timestamp">
											<template v-if="backup.data.status.process.status == 'Completed'">
												<span class='date'>
													{{ backup.data.status.process.timing.stored | formatTimestamp('date') }}
												</span>
												<span class='time'>
													{{ backup.data.status.process.timing.stored | formatTimestamp('time') }}
												</span>
												<span class='ms'>
													{{ backup.data.status.process.timing.stored | formatTimestamp('ms') }}
												</span>
												Z
											</template>
										</td>
									</tr>
								</template>
								<template v-else-if="index+1 == backups.length">
									<tr>
										<td class="label" rowspan="3">
											Initial Data
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData')"></span>
										</td>
										<td class="label">
											Download Disk Concurrency
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData.restore.downloadDiskConcurrency')"></span>
										</td>
										<td colspan="2">
											{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}
										</td>
									</tr>
									<tr>
										<td class="label" rowspan="2">
											Restored from Backup
											<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData.restore.fromBackup')"></span>
										</td>
										<td class="label">
											Backup UID
										</td>
										<td>
											{{ cluster.data.spec.initialData.restore.fromBackup }}
										</td>
									</tr>
									<tr>
										<td colspan="2">
											No further information available.
										</td>
									</tr>
								</template>
							</template>
						</template>	
					</tbody>
				</table>

				<div class="podsMetadata" v-if="hasProp(cluster, 'data.spec.pods.metadata') || hasProp(cluster, 'data.spec.pods.scheduling')">
					<h2>Pods Details</h2>
					<table v-if="hasProp(cluster, 'data.spec.pods.metadata.labels')" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.pods.metadata.labels)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.metadata.labels).length">
									Pods Metadata
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.metadata')"></span>
								</td>
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.metadata.labels).length">
									Labels
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.metadata.labels')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td>
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>

					<table v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeSelector')" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.pods.scheduling.nodeSelector)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.scheduling.nodeSelector).length">
									Pods Scheduling
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.scheduling')"></span>
								</td>
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.scheduling.nodeSelector).length">
									Node Selectors
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.pods.scheduling.nodeSelector')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td>
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>
				</div>

				<div class="scripts" v-if="hasProp(cluster, 'data.spec.initialData.scripts')">
					<h2>Scripts <span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.initialData.scripts')"></span></h2>
					<table class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<template v-for="(item, index) in cluster.data.spec.initialData.scripts">
								<template v-if="hasProp(item, 'database')">
									<tr>
										<td class="label" rowspan="2">
											Script #{{ index+1 }} <template v-if="hasProp(item, 'name')">– {{ item.name }} </template> 
										</td>
										<td class="label">
											Database
										</td>
										<td colspan="2">
											{{ item.database }}
										</td>
									</tr>
									<tr>
										<td class="label">
											Script Details 
										</td>
										<td colspan="2">
											<a @click="setContentTooltip('#script-'+index)"> 
												View Script
												<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
											</a>
											<div :id="'script-'+index" class="hidden"><pre>{{ item.script }}</pre></div>
										</td>
									</tr>
								</template>		
								<template v-else>
									<tr>
										<td class="label">
											Script #{{ index+1 }} <template v-if="hasProp(item, 'name')">– {{ item.name }} </template> 
										</td>
										<td class="label">
											Script Details
										</td>
										<td colspan="2">
											<a @click="setContentTooltip('#script-'+index)"> 
												View Script
												<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
											</a>
											<div :id="'script-'+index" class="hidden"><pre>{{ item.script }}</pre></div>
										</td>
									</tr>
								</template>
							</template>
						</tbody>
					</table>
				</div>

				<div class="resourcesMetadata" v-if="hasProp(cluster, 'data.spec.metadata.annotations')">
					<h2>Resources Metadata <span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.metadata')"></span></h2>
					<table v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.allResources)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.allResources).length">
									All Resources
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.metadata.annotations.allResources')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td colspan="2">
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>

					<table v-if="hasProp(cluster, 'data.spec.metadata.annotations.pods')" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.pods)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.pods).length">
									Pods
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.metadata.annotations.pods')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td colspan="2">
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>

					<table v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.services)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.services).length">
									Services
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td colspan="2">
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>
				</div>	

				<div class="postgresServices" v-if="hasProp(cluster, 'data.spec.postgresServices') && ((hasProp(cluster, 'data.spec.postgresServices.primary') && cluster.data.spec.postgresServices.primary.enabled) || (hasProp(cluster, 'data.spec.postgresServices.replicas') && cluster.data.spec.postgresServices.replicas.enabled))">
					<h2>Postgres Services <span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices')"></span></h2>
					<table v-if="hasProp(cluster, 'data.spec.postgresServices.primary') && cluster.data.spec.postgresServices.primary.enabled" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr>
								<td class="label" v-if="!hasProp(cluster, 'data.spec.postgresServices.primary.annotations')">
									Primary
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.primary')"></span>
								</td>
								<td class="label" v-else :rowspan="Object.keys(cluster.data.spec.postgresServices.primary.annotations).length+1">
									Primary
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.primary')"></span>
								</td>
								<td class="label">
									Type
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.primary.type')"></span>
								</td>
								<td colspan="2">
									{{ cluster.data.spec.postgresServices.primary.type }}
								</td>
							</tr>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.postgresServices.primary.annotations)">
								<td v-if="index == 0" class="label" :rowspan="Object.keys(cluster.data.spec.postgresServices.primary.annotations).length">
									Annotations
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.primary.annotations')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td>
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>

					<table v-if="hasProp(cluster, 'data.spec.postgresServices.replicas') && cluster.data.spec.postgresServices.replicas.enabled" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr>
								<td class="label" v-if="!hasProp(cluster, 'data.spec.postgresServices.replicas.annotations')">
									Replicas
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.replicas')"></span>
								</td>
								<td class="label" v-else :rowspan="Object.keys(cluster.data.spec.postgresServices.replicas.annotations).length+1">
									Replicas
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.replicas')"></span>
								</td>
								<td class="label">
									Type
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.replicas.type')"></span>
								</td>
								<td colspan="2">
									{{ cluster.data.spec.postgresServices.replicas.type }}
								</td>
							</tr>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.postgresServices.replicas.annotations)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.postgresServices.replicas.annotations).length">
									Annotations
									<span class="helpTooltip"  @mouseover="helpTooltip( 'SGCluster', 'spec.postgresServices.replicas.annotations')"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td>
									{{ item.value }}
								</td>
							</tr>
						</tbody>
					</table>
				</div>		
			</div>
		</template>
		</div>`,
	data: function() {
		return {
	     
	    }
	},
	methods: {

		unparseProps ( props, key = 'annotation' ) {
			var propsArray = [];
			if(!jQuery.isEmptyObject(props)) {
				Object.entries(props).forEach(([k, v]) => {
					var prop = {};
					prop[key] = k;
					prop['value'] = v;
					propsArray.push(prop)
				});
			}
			
            return propsArray
		}

	},
	created: function() {
		
	},
	mounted: function() {

	},
	computed: {

		clusters () {
			//console.log(store.state.currentCluster);
			return store.state.clusters
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		profiles () {
			
			//let profile = store.state.profiles.find(p => ( (this.$route.params.namespace == p.data.metadata.namespace) && (store.state.currentCluster.data.spec.sgInstanceProfile == p.name) ) );
			return store.state.profiles
		},

		backups () {
			return store.state.backups
		}
	},
	beforeDestroy () {
		//clearInterval(this.polling);
		//console.log('Interval cleared');
	} 
})