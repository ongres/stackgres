<template>
	<div id="cluster-info" v-if="loggedIn && isReady && !notFound">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="Namespace Clusters Overview">{{ $route.params.namespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="Namespace Clusters Overview">SGClusters</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Status">{{ $route.params.name }}</router-link>
					</li>
					<li>
						Configuration
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('create','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/edit'">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgcluster', $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgclusters')" :class="$route.params.namespace + '/sgclusters'">Delete Cluster</a>
					</div>		
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/config'" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="iCan('list','sgbackups',$route.params.namespace)">
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/sgbackups'" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && cluster.data.spec.hasOwnProperty('distributedLogs')">
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/logs'" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="cluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/monitor'" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>

			<div class="content noScroll">
				<h2>Cluster Details</h2>
				<div class="connectionInfo" v-if="hasProp(cluster, 'data.info')" :set="hasPrimary = ( typeof ( cluster.data.pods.find(p => (p.role == 'primary')) ) == 'undefined' )">
					<a @click="setContentTooltip('#connectionInfo', hasPrimary)"> 
						<h2>View Connection Info</h2>
						<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
					</a>

					<div id="connectionInfo" class="hidden">
						<div class="connInfo">
							<div class="textCenter" v-if="( typeof ( cluster.data.pods.find(p => (p.role == 'primary')) ) == 'undefined' )">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)"/></g></svg>
								<h4>Attention</h4>
								<p>
									No pods are available yet for this cluster.<br/>
									You won't be able to connect to it until there's at least one active pod.
								</p>
							</div>
							<template v-else>
								<p>To access StackGres cluster <code>{{ $route.params.namespace + '.' + cluster.name }}</code> you can address one or both of the following DNS entries:
									<ul>
										<li>Read Write DNS: <code>{{ cluster.data.info.primaryDns }}</code> </li>
										<li>Read Only DNS: <code>{{ cluster.data.info.replicasDns }}</code> </li>
									</ul>
								</p>	

								<p>You may connect with Postgres client <code>psql</code> in two different ways:
									<ul>
										<li>
											Local <code>psql</code> (runs within the same pod as Postgres):<br/>
											<template v-for="pod in cluster.data.pods">
												<pre v-if="pod.role == 'primary'">kubectl -n {{ $route.params.namespace }} exec -ti {{ pod.name }} -c postgres-util -- psql{{ cluster.data.info.superuserUsername !== 'postgres' ? ' -U '+cluster.data.info.superuserUsername : '' }}<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
											</template>
										</li>
										<li>
											Externally to StackGres pods, from a container image that contains <code>psql</code> (this option is the only one available if you have disabled the <code>postgres-util</code> sidecar):<br/>
											<pre>kubectl -n {{ $route.params.namespace }} run psql --rm -it --image ongres/postgres-util:v13.2-build-6.2 --restart=Never -- psql -h {{ cluster.name }}-primary {{ cluster.data.info.superuserUsername }} {{ cluster.data.info.superuserUsername }}  <span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
										</li>
									</ul>
								</p>

								<p>The command will ask for the admin user password (prompt may not be shown, just type or paste the password). Use the following command to retrieve it:<br/>
									<pre>kubectl -n {{ $route.params.namespace }} get secret {{ cluster.data.info.superuserSecretName }} --template <template v-pre>'{{</template> printf "%s" (index .data "{{ cluster.data.info.superuserPasswordKey }}" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
								</p>
							</template>
						</div>
					</div>
				</div>
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
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresVersion')"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.postgresVersion }}</td>
						</tr>
						<tr>
							<td class="label">
								Instances
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
							</td>
							<td colspan="3">{{ cluster.data.spec.instances }}</td>
						</tr>
						<tr>
							<td class="label">
								Instance Profile
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
							</td>
							<td colspan="3" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
								<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + cluster.data.spec.sgInstanceProfile">
									{{ cluster.data.spec.sgInstanceProfile }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }}) 
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
								<template v-if="clusterProfileMismatch(cluster, profile)">
									<span class="helpTooltip alert" data-tooltip="This profile has been modified recently. Cluster must be restarted in order to apply such changes."></span>
								</template>
							</td>
						</tr>
						<tr>
							<td class="label" rowspan="3">
								Pods
								<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods')"></span>
							</td>
							<td class="label" :rowspan="Object.keys(cluster.data.spec.pods.persistentVolume).length">
								Persistent Volume
								<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume')"></span>
							</td>
							<td class="label">
								Volume Size
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.pods.persistentVolume.storageClass !== 'undefined')">
							<td class="label">
								Storage Class
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
							</td>
							<td>{{ cluster.data.spec.pods.persistentVolume.size }}</td>
						</tr>
						<tr>
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
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
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter')"></span>
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
								<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations')"></span>
							</td>
							<td class="label">
								Postgres
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.configurations.sgPostgresConfig">
									{{ cluster.data.spec.configurations.sgPostgresConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>								
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgPoolingConfig !== 'undefined')">
							<td class="label">
								Connection Pooling
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.configurations.sgPoolingConfig">
									{{ cluster.data.spec.configurations.sgPoolingConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>	
							</td>
						</tr>
						<tr v-if="(typeof cluster.data.spec.configurations.sgBackupConfig !== 'undefined')">
							<td class="label">
								Managed Backups
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgBackupConfig')"></span>
							</td>
							<td colspan="2">
								<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + cluster.data.spec.configurations.sgBackupConfig">
									{{ cluster.data.spec.configurations.sgBackupConfig }}
									<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
								</router-link>
							</td>
						</tr>
						<tr>
							<td class="label">
								Prometheus Autobind
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
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
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity')"></span>
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
							<tr :set="backup = backups.find( b => (b.data.metadata.uid == cluster.data.spec.initialData.restore.fromBackup.uid) )">
								<td class="label" :rowspan="1 + Object.keys(cluster.data.spec.initialData.restore.fromBackup).length + ( (typeof backup !== 'undefined') ? 1 : 0)">
									Initial Data
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData')"></span>
								</td>
								<td class="label">
									Download Disk Concurrency
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.downloadDiskConcurrency')"></span>
								</td>
								<td colspan="2">
									{{ cluster.data.spec.initialData.restore.downloadDiskConcurrency }}
								</td>
							</tr>
							<tr>
								<td class="label" :rowspan="(cluster.data.spec.initialData.restore.fromBackup.hasOwnProperty('pointInTimeRecovery') ? 1 : 0) + ((typeof backup !== 'undefined') ? 2 : 1)">
									Restored from Backup
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup')"></span>
								</td>
								<td class="label">
									Backup UID
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.uid')"></span>
								</td>
								<td>
									<template v-if="(typeof backup !== 'undefined')">
										<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+backup.data.spec.sgCluster+'/'+backup.data.metadata.name"> 
											{{ cluster.data.spec.initialData.restore.fromBackup.uid }}
											<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
										</router-link>
									</template>
									<template v-else>
										{{ cluster.data.spec.initialData.restore.fromBackup.uid }}
									</template>
								</td>
							</tr>
							<tr v-if="(typeof backup !== 'undefined')">
								<td class="label">
									Date
									<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
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
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</template>
								</td>
							</tr>
							<tr v-if="cluster.data.spec.initialData.restore.fromBackup.hasOwnProperty('pointInTimeRecovery')">
								<td class="label">
									Point-in-Time Recovery
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery')"></span>
								</td>
								<td class="timestamp">
									<span class='date'>
										{{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('date') }}
									</span>
									<span class='time'>
										{{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('time') }}
									</span>
									<span class='ms'>
										{{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('ms') }}
									</span>
									<span class='tzOffset'>{{ showTzOffset() }}</span>
								</td>
							</tr>
						</template>	
					</tbody>
				</table>

				<div class="podsMetadata" v-if="hasProp(cluster, 'data.spec.pods.metadata')">
					<h2>Pods Metadata <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.metadata')"></span></h2>
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
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.metadata')"></span>
								</td>
								<td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.metadata.labels).length">
									Labels
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.metadata.labels')"></span>
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
					<h2>Pods Scheduling <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling')"></span></h2>
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
									<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span>
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
										<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span>
									</td>
									<td class="label" :rowspan="Object.keys(item).length" v-if="!i">
										Toleration #{{ index+1 }}
									</td>
									<td class="label">
										{{ prop }}
										<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations[prop]')"></span>
									</td>
									<td colspan="2">
										{{ value }}
									</td>
								</tr>
							</template>
						</tbody>
					</table>
				</div>

				<div class="scripts" v-if="hasProp(cluster, 'data.spec.initialData.scripts')">
					<h2>Scripts <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts')"></span></h2>
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

				<div class="resourcesMetadata" v-if="hasProp(cluster, 'data.spec.metadata.annotations')">
					<h2>Resources Metadata <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata')"></span></h2>
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
									<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
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
									<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.pods')"></span>
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
									<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
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
					<h2>Postgres Services <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices')"></span></h2>

					<table v-for="(service, serviceName) in cluster.data.spec.postgresServices" class="clusterConfig">
						<thead>
							<th></th>
							<th></th>
							<th></th>
							<th></th>
						</thead>
						<tbody>
							<tr>
								<td class="label capitalize" :rowspan="service.hasOwnProperty('annotations') ? Object.keys(service.annotations).length+2 : 2">
									{{ serviceName }}
									<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName)"></span>
								</td>
								<td class="label">
									Name
								</td>
								<td colspan="2">
									{{ cluster.data.metadata.name }}-{{serviceName}}.{{cluster.data.metadata.namespace}}
								</td>	
							</tr>
							<tr>
								<td class="label">
									Type
									<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName+'.type')"></span>
								</td>
								<td colspan="2">
									{{ service.type }}
								</td>
							</tr>
							<template v-if="service.hasOwnProperty('annotations')">
								<tr v-for="(item, index) in unparseProps(service.annotations)">
									<td v-if="index == 0" class="label" :rowspan="Object.keys(service.annotations).length">
										Annotations
										<span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName+'.annotations')"></span>
									</td>
									<td class="label">
										{{ item.annotation }}
									</td>
									<td>
										{{ item.value }}
									</td>
								</tr>
							</template>
						</tbody>
					</table>
				</div>
				
				<div class="postgresExtensions" v-if="hasProp(cluster, 'data.spec.postgresExtensions') && cluster.data.spec.postgresExtensions.length">
					<h2>Postgres Extensions <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresExtensions')"></span></h2>

					<table class="clusterConfig">
						<thead style="display: table-header-group">
							<th>
								Name
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.name')"></span>
							</th>
							<th>
								Description
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.description')"></span>
							</th>
							<th>
								Version
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.versions')"></span>
							</th>
							<th>
								Link
								<span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.url')"></span>
							</th>
						</thead>
						<tbody>
							<tr v-for="ext in sortExtensions(cluster.data.spec.postgresExtensions, cluster.data.spec.postgresVersion)">
								<template v-for="extInfo in extensionsList" v-if="ext.name == extInfo.name">
									<td class="label">
										{{ ext.name }}
									</td>
									<td class="firstLetter">
										{{ extInfo.abstract }}
									</td>
									<td>
										{{ ext.version }}
									</td>
									<td>
										<a v-if="extInfo.hasOwnProperty('url') && extInfo.url" :href="extInfo.url" target="_blank" class="trimText" :title="extInfo.url">{{ extInfo.url }}</a>
									</td>
								</template>
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
	import axios from 'axios'

    export default {
        name: 'ClusterInfo',

		mixins: [mixin],

		data: function() {
			return {
				extensionsList: [],
			}
		},
		methods: {

			unparseProps ( props, key = 'annotation' ) {
				var propsArray = [];
				if(!$.isEmptyObject(props)) {
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

			sortExtensions(ext, pgVersion = 'latest') {

				const vc = this;

				if(!vc.extensionsList.length) {

					axios
					.get('/stackgres/extensions/' + pgVersion)
					.then(function (response) {
						vc.extensionsList = response.data.extensions
					})
					.catch(function (error) {
						console.log(error.response);
						vc.notify(error.response.data,'error','sgcluster');
					});

				}

				return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
			},

			clusterProfileMismatch(cluster, profile) {
				const vc = this;
				
				if(vc.hasProp(cluster, 'status.pods') && cluster.status.pods.length) {
					let mismatch = cluster.status.pods.find(pod => (( pod.cpuRequested != ( (pod.cpuRequested.includes('m') && !profile.data.spec.cpu.includes('m')) ? ( (profile.data.spec.cpu * 1000) + 'm' ) : profile.data.spec.cpu ) ) || (pod.memoryRequested.replace('.00','') != profile.data.spec.memory)))				
					return (typeof mismatch !== 'undefined')
				} else {
					return false;
				}

			}

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
		max-width: 250px;
		width: 100%;
	}
</style>