<template>
	<div id="cluster-info" v-if="loggedIn && isReady && !notFound">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">SGClusters</router-link>
					</li>
					<li>
						<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status">{{ $route.params.name }}</router-link>
					</li>
					<li>
						Configuration
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('create','sgclusters',$route.params.namespace)" :to="'/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" v-on:click="deleteCRD('sgcluster', $route.params.namespace, $route.params.name, '/overview/'+$route.params.namespace)" :class="'/overview/'+$route.params.namespace">Delete Cluster</a>
					</div>		
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="iCan('list','sgbackups',$route.params.namespace)">
						<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && cluster.data.spec.hasOwnProperty('distributedLogs')">
						<router-link :to="'/cluster/logs/'+$route.params.namespace+'/'+$route.params.name" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="cluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/cluster/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>

			<div class="content">
				<h2>Cluster Details</h2>
				<table class="clusterConfig" v-if="tooltips.hasOwnProperty('sgcluster')">
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
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresVersion.description"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.postgresVersion }}</td>
						</tr>
						<tr>
							<td class="label">
								Instances
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.instances.description"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.instances }}</td>
						</tr>
						<tr>
							<td class="label">
								Instance Profile
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.sgInstanceProfile.description"></span>
							</td>
							<td colspan="3" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
								<router-link :to="'/profiles/'+$route.params.namespace+'/'+cluster.data.spec.sgInstanceProfile">
									{{ cluster.data.spec.sgInstanceProfile }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }}) 
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
								<template v-if="hasProp(cluster, 'status.cpuRequested') && hasProp(cluster, 'status.memoryRequested') && ( ( cluster.status.cpuRequested != (cluster.status.cpuRequested.includes('m') ? ( (profile.data.spec.cpu * 1000) + 'm' ) : profile.data.spec.cpu ) ) || (cluster.status.memoryRequested.replace('.00','') != profile.data.spec.memory) )">
									<span class="helpTooltip alert" data-tooltip="This profile has been modified recently. Cluster must be restarted in order to apply such changes."></span>
								</template>
							</td>
						</tr>
						<tr>
							<td class="label" rowspan="3">
								Pods
								<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.description"></span>
							</td>
							<td class="label" :rowspan="Object.keys(cluster.data.spec.pods.persistentVolume).length">
								Persistent Volume
								<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.persistentVolume.description"></span>
							</td>
							<td class="label">
								Volume Size
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.pods.persistentVolume.size.description"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.pods.persistentVolume.storageClass !== 'undefined')">
							<td class="label">
								Storage Class
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.pods.persistentVolume.storageClass.description"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr>
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.configurations.sgPoolingConfig.description"></span>
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
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.pods.disableMetricsExporter.description"></span>
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
								<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.configurations.description"></span>
							</td>
							<td class="label">
								Postgres
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.configurations.sgPostgresConfig.description"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/configurations/postgres/'+$route.params.namespace+'/'+cluster.data.spec.configurations.sgPostgresConfig">
									{{ cluster.data.spec.configurations.sgPostgresConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>								
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgPoolingConfig !== 'undefined')">
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.configurations.sgPoolingConfig.description"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/configurations/connectionpooling/'+$route.params.namespace+'/'+cluster.data.spec.configurations.sgPoolingConfig">
									{{ cluster.data.spec.configurations.sgPoolingConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>	
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgBackupConfig !== 'undefined')">
							<td class="label">
								Managed Backups
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.configurations.sgBackupConfig.description"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/configurations/backup/'+$route.params.namespace+'/'+cluster.data.spec.configurations.sgBackupConfig">
									{{ cluster.data.spec.configurations.sgBackupConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
							</td>
						</tr>
						<tr>
							<td class="label">
								Prometheus Autobind
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.prometheusAutobind.description"></span>
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
						<tr v-if="(typeof cluster.data.spec.nonProductionOptions !== 'undefined')">
							<td class="label">
								Non-Production Settings
							</td>
							<td class="label">
								Cluster Pod Anti Affinity
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity.description"></span>
							</td>
							<td colspan="2">
								<template v-if="(typeof cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity !== 'undefined')">
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
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.description"></span>
										</td>
										<td class="label">
											Download Disk Concurrency
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.restore.downloadDiskConcurrency.description"></span>
										</td>
										<td colspan="2">
											{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}
										</td>
									</tr>
									<tr>
										<td class="label" rowspan="2">
											Restored from Backup
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.restore.fromBackup.description"></span>
										</td>
										<td class="label">
											Backup UID
										</td>
										<td>
											<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+backup.data.spec.sgCluster+'/'+backup.data.metadata.uid"> 
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
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.description"></span>
										</td>
										<td class="label">
											Download Disk Concurrency
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.restore.downloadDiskConcurrency.description"></span>
										</td>
										<td colspan="2">
											{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}
										</td>
									</tr>
									<tr>
										<td class="label" rowspan="2">
											Restored from Backup
											<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.restore.fromBackup.description"></span>
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

				<div class="podsMetadata" v-if="hasProp(cluster, 'data.spec.pods.metadata')">
					<h2>Pods Metadata <span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.metadata.description"></span></h2>
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
									<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.metadata.description"></span>
								</td>
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.metadata.labels).length">
									Labels
									<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.metadata.labels.description"></span>
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

				<div class="podsScheduling" v-if="hasProp(cluster, 'data.spec.pods.scheduling')">
					<h2>Pods Scheduling <span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.scheduling.description"></span></h2>
					<table class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.pods.scheduling.nodeSelector)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.scheduling.nodeSelector).length">
									Node selectors
									<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.scheduling.nodeSelector.description"></span>
								</td>
								<td class="label">
									{{ item.annotation }}
								</td>
								<td colspan="2">
									{{ item.value }}
								</td>
							</tr>
							<template v-for="(item, index) in cluster.data.spec.pods.scheduling.tolerations">
								<tr v-for="(value, prop, i) in item">
									<td v-if="!index && !i" class="label" :rowspan="countObjectArrayKeys(cluster.data.spec.pods.scheduling.tolerations)">
										Tolerations
										<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.scheduling.tolerations.description"></span>
									</td>
									<td class="label" :rowspan="Object.keys(item).length" v-if="!i">
										Toleration #{{ index+1 }}
									</td>
									<td class="label">
										{{ prop }}
										<span class="helpTooltip" :data-tooltip="tooltips.sgcluster.spec.pods.scheduling.tolerations[prop].description"></span>
									</td>
									<td colspan="2">
										{{ value }}
									</td>
								</tr>
							</template>
						</tbody>
					</table>
				</div>

				<div class="scripts" v-if="hasProp(cluster, 'data.spec.initialData.scripts') && tooltips.hasOwnProperty('sgcluster')">
					<h2>Scripts <span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.initialData.scripts.description"></span></h2>
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
											<div :id="'script-'+index" class="hidden">
												<pre v-if="item.hasOwnProperty('script')">{{ item.script }}</pre>
												<pre v-else-if="hasProp(item, 'scriptFrom.configMapScript')">{{ item.scriptFrom.configMapScript }}</pre>
												<template v-else-if="hasProp(item, 'scriptFrom.secretScript')">
													<pre>{{ item.scriptFrom.secretScript }}</pre>
													<span class="toggleSecret">
														<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
													</span>
												</template>
											</div>
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
											<div :id="'script-'+index" class="hidden">
												<pre v-if="item.hasOwnProperty('script')">{{ item.script }}</pre>
												<pre v-else-if="hasProp(item, 'scriptFrom.configMapScript')">{{ item.scriptFrom.configMapScript }}</pre>
												<template v-else-if="hasProp(item, 'scriptFrom.secretScript')">
													<pre class="blur">{{ item.scriptFrom.secretScript }}</pre>
													<span class="toggleSecret">
														<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
													</span>
												</template>
											</div>
										</td>
									</tr>
								</template>
							</template>
						</tbody>
					</table>
				</div>

				<div class="resourcesMetadata" v-if="hasProp(cluster, 'data.spec.metadata.annotations') && tooltips.hasOwnProperty('sgcluster')">
					<h2>Resources Metadata <span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.metadata.description"></span></h2>
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
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.metadata.annotations.allResources.description"></span>
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
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.metadata.annotations.pods.description"></span>
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
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.metadata.annotations.services.description"></span>
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

				<div class="postgresServices" v-if="tooltips.hasOwnProperty('sgcluster') && hasProp(cluster, 'data.spec.postgresServices') && ((hasProp(cluster, 'data.spec.postgresServices.primary') && cluster.data.spec.postgresServices.primary.enabled) || (hasProp(cluster, 'data.spec.postgresServices.replicas') && cluster.data.spec.postgresServices.replicas.enabled))">
					<h2>Postgres Services <span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.description"></span></h2>
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
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.primary.description"></span>
								</td>
								<td class="label" v-else :rowspan="Object.keys(cluster.data.spec.postgresServices.primary.annotations).length+1">
									Primary
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.primary.description"></span>
								</td>
								<td class="label">
									Type
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.primary.type.description"></span>
								</td>
								<td colspan="2">
									{{ cluster.data.spec.postgresServices.primary.type }}
								</td>
							</tr>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.postgresServices.primary.annotations)">
								<td v-if="index == 0" class="label" :rowspan="Object.keys(cluster.data.spec.postgresServices.primary.annotations).length">
									Annotations
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.primary.annotations.description"></span>
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
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.replicas.description"></span>
								</td>
								<td class="label" v-else :rowspan="Object.keys(cluster.data.spec.postgresServices.replicas.annotations).length+1">
									Replicas
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.replicas.description"></span>
								</td>
								<td class="label">
									Type
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.replicas.type.description"></span>
								</td>
								<td colspan="2">
									{{ cluster.data.spec.postgresServices.replicas.type }}
								</td>
							</tr>
							<tr v-for="(item, index) in unparseProps(cluster.data.spec.postgresServices.replicas.annotations)">
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.postgresServices.replicas.annotations).length">
									Annotations
									<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresServices.replicas.annotations.description"></span>
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
				
				<div class="postgresExtensions" v-if="hasProp(cluster, 'data.spec.postgresExtensions') && cluster.data.spec.postgresExtensions.length">
					<h2>Postgres Extensions <span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresExtensions.description"></span></h2>

					<table class="clusterConfig">
						<thead style="display: table-header-group">
							<th>
								Name
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresExtensions.name.description"></span>
							</th>
							<th>
								Publisher
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresExtensions.publisher.description"></span>
							</th>
							<th>
								Version
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresExtensions.version.description"></span>
							</th>
							<th>
								Repository
								<span class="helpTooltip"  :data-tooltip="tooltips.sgcluster.spec.postgresExtensions.repository.description"></span>
							</th>
						</thead>
						<tbody>
							<tr v-for="ext in sortExtensions(cluster.data.spec.postgresExtensions)">
								<td class="label">
									{{ ext.name }}
								</td>
								<td>
									{{ ext.publisher }}
								</td>
								<td>
									{{ ext.version }}
								</td>
								<td>
									<span class="trimText" :title="ext.repository">{{ ext.repository }}</span>
								</td>
							</tr>
						</tbody>
					</table>
				</div>

			</div>
		</template>
	</div>
</template>

<script>
	import store from '../store'
	import { mixin } from './mixins/mixin'

    export default {
        name: 'ClusterInfo',

		mixins: [mixin],

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
			},

			countObjectArrayKeys(objectArray) {
				let count = 0;

				objectArray.forEach(function(obj, index) {
					count += Object.keys(obj).length
				})

				return count
			},

			sortExtensions(ext) {
				return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
			}

		},
		created: function() {
			
		},
		mounted: function() {
			$(document).on('click','.toggleSecret', function() {
				$('pre.blur, .toggleSecret').toggleClass('show')
			})
		},
		computed: {

			tooltips () {
				return store.state.tooltips
			},

			clusters () {
				return store.state.clusters
			},

			profiles () {
				return store.state.profiles
			},

			backups () {
				return store.state.backups
			}
		},

	}
</script>

<style scoped>
	.clusterConfig td {
		position: relative;
	}

	.helpTooltip.alert {
		position: absolute;
		right: 30px;
		top: 11px;
	}

	.postgresExtensions th {
		padding-left: 10px;
	}

	.trimText {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		display: block;
		width: 250px;
	}
</style>