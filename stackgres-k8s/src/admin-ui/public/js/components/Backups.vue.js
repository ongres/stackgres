var Backups = Vue.component("Backups", {
	template: `
		<div id="sg-backup" v-if="loggedIn && isReady">
			<header v-if="isCluster">
				<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
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
							Backups
						</li>
					</ul>

					<div class="actions" v-if="typeof cluster.data !== 'undefined'">
						<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
						<div>
							<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', currentNamespace, $route.params.name)">Clone Cluster Configuration</a>
							<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/admin/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link>
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
				</template>
			</header>
			<header v-else>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
							SGBackupList
					</li>
					<li v-if="typeof $route.params.uid !== 'undefined'">
							{{ $route.params.uid }}
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/04-backups/" target="_blank" title="SGBackup Documentation">SGBackup Documentation</a>
					<div>
						<router-link v-if="iCan('create','sgbackups',$route.params.namespace)" :to="'/admin/crd/create/backup/'+currentNamespace" class="add">Add New</router-link>
					</div>
				</div>	
			</header>

			<div class="content">
				<div id="backups">
					<div class="toolbar">
						<div class="searchBar">
							<input id="keyword" v-model="keyword" class="search" placeholder="Search Backup..." autocomplete="off" @keyup="toggleClear('keyword')">
							<a @click="filterBackups" class="btn">APPLY</a>
							<a @click="clearFilters('keyword')" class="btn clear border keyword" style="display:none">CLEAR</a>
						</div>

						<router-link v-if="isCluster && iCan('create','sgbackups',$route.params.namespace)" :to="'/admin/crd/create/backup/'+$route.params.namespace+'/'+$route.params.name" title="Add New Backup" class="btn addClusterBackup">Add Backup</router-link>

						<div class="filter">
							<span class="toggle date">DATE RANGE <input v-model="datePicker" id="datePicker" autocomplete="off"></span>
						</div>

						<div class="filter filters">
							<span class="toggle">FILTER</span>

							<ul class="options">
								<li>
									<span>Managed Lifecycle (request)</span>
									<label for="managedLifecycle">
										<input v-model="managedLifecycle" data-filter="managedLifecycle" type="checkbox" class="xCheckbox" id="managedLifecycle" name="managedLifecycle" value="true"/>
										<span>YES</span>
									</label>
									<label for="notPermanent">
										<input v-model="managedLifecycle" data-filter="managedLifecycle" type="checkbox" class="xCheckbox" id="notPermanent" name="notPermanent" value="false"/>
										<span>NO</span>
									</label>
								</li>

								<li>
									<span>Status</span>
									<label for="isCompleted">
										<input v-model="status" data-filter="status" type="checkbox" id="isCompleted" name="isCompleted" value="Completed"/>
										<span>Completed</span>
									</label>
									<label for="notCompleted">
										<input v-model="status" data-filter="status" type="checkbox" id="notCompleted" name="notCompleted" value="Running"/>
										<span>Running</span>
									</label>
									<label for="backupFailed">
										<input v-model="status" data-filter="status" type="checkbox" id="backupFailed" name="backupFailed" value="Failed"/>
										<span>Failed</span>
									</label>
								</li>

								<li v-if="!isCluster">
									<span>Postgres Version</span>
									<label for="pg11">
										<input v-model="postgresVersion" data-filter="postgresVersion" type="checkbox" class="xCheckbox" id="pg11" name="pg11" value="11"/>
										<span>11</span>
									</label>
									<label for="pg12">
										<input v-model="postgresVersion" data-filter="postgresVersion" type="checkbox" class="xCheckbox" id="pg12" name="pg12" value="12"/>
										<span>12</span>
									</label>
								</li>
								
								<!--<li>
									<span>Tested</span>
									<label for="isTested">
										<input v-model="tested" type="checkbox" class="xCheckbox" id="isTested" name="tested" value="true"/>
										<span>YES</span>
									</label>
									<label for="notTested">
										<input v-model="tested" type="checkbox" class="xCheckbox" id="notTested" name="tested" value="false"/>
										<span>NO</span>
									</label>
								</li>-->

								<li v-if="!isCluster">
									<span>Cluster</span>
									<select v-model="clusterName" @change="toggleClear('filters')">
										<option value="">All Clusters</option>
										<template v-for="cluster in clusters">
											<option v-if="cluster.data.metadata.namespace == currentNamespace">{{ cluster.data.metadata.name }}</option>
										</template>
									</select>
								</li>

								<li>
									<hr>
									<a class="btn" @click="filterBackups">APPLY</a> <a class="btn clear border" @click="clearFilters('filters')" style="display:none">CLEAR</a>
								</li>
							</ul>
						</div>
					</div>
					<table class="backups" v-if="tooltips.hasOwnProperty('sgbackup')">
						<thead class="sort">
							<th class="sorted desc timestamp">
								<span @click="sort('data.status.process.timing.stored')">Timestamp</span>
								<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.timing.stored.description"></span>
							</th>
							<th class="desc managedLifecycle">
								<span @click="sort('data.spec.managedLifecycle')" >Managed Lifecycle (request)</span>
								<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.spec.managedLifecycle.description"></span>
							</th>
							<th class="desc phase center">
								<span @click="sort('data.status.process.status')" >Status</span>
								<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.status.description"></span>
							</th>
							<th class="desc size">
								<span @click="sort('data.status.backupInformation.size.uncompressed')">Size uncompressed (compressed)</span>
								<span class="helpTooltip" data-tooltip="Size (in bytes) of the uncompressed backup (Size (in bytes) of the compressed backup)."></span>
							</th>
							<th class="desc postgresVersion" v-if="!isCluster">
								<span@click="sort('data.status.backupInformation.postgresVersion')" >PG</span>
								<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.postgresVersion.description"></span>
							</th>
							<!--<th @click="sort('data.status.tested')" class="icon desc tested">
								<span>Tested</span>
							</th>-->
							<th class="desc name">
								<span @click="sort('data.metadata.name')">Name</span>
								<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.metadata.name.description"></span>
							</th>
							<th class="desc clusterName" v-if="!isCluster">
								<span @click="sort('data.spec.sgCluster')" >Source Cluster</span>
								<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.spec.sgCluster.description"></span>
							</th>
							<th class="actions"></th>
							<!--<th class="details"></th>-->
						</thead>
						<tbody>
							<tr class="no-results">
								<td :colspan="999" v-if="iCan('create','sgbackups',$route.params.namespace)">
									No records matched your search terms, would  you like to <router-link :to="'/admin/crd/create/backup/'+$route.params.namespace" title="Add New Backup">create a new one?</router-link>
								</td>
								<td v-else colspan="999">
									No backups have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
							<template v-for="(back, index) in backups" v-if="( ( (back.data.metadata.namespace == currentNamespace) && !isCluster && back.show ) || (isCluster && (back.data.spec.sgCluster == $route.params.name ) && (back.data.metadata.namespace == $route.params.namespace ) && back.show ) )">
								<template v-if="back.data.status">
									<tr :id="back.data.metadata.uid" :class="[ back.data.status.process.status != 'Running' ? 'base' : '', back.data.status.process.status+' sgbackup-'+back.data.metadata.namespace+'-'+back.data.metadata.name, $route.params.uid == back.data.metadata.uid ? 'open' : '']" :data-cluster="back.data.spec.sgCluster" :data-uid="back.data.metadata.uid">
											<td class="timestamp" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.process.timing.stored.substr(0,19).replace('T',' ') : ''">
												<template v-if="back.data.status.process.status == 'Completed'">
													<span class='date'>
														{{ back.data.status.process.timing.stored | formatTimestamp('date') }}
													</span>
													<span class='time'>
														{{ back.data.status.process.timing.stored | formatTimestamp('time') }}
													</span>
													<span class='ms'>
														{{ back.data.status.process.timing.stored | formatTimestamp('ms') }}
													</span>
													Z
												</template>
											</td>
											<td class="managedLifecycle center icon" :class="[(back.data.spec.managedLifecycle) ? 'true' : 'false']" :data-val="back.data.spec.managedLifecycle"></td>
											<td class="phase center" :class="back.data.status.process.status">
												<span>{{ back.data.status.process.status }}</span>
											</td>
											<td class="size" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.backupInformation.size.uncompressed : ''">
												<template v-if="back.data.status.process.status === 'Completed'">
												{{ back.data.status.backupInformation.size.uncompressed | formatBytes }} ({{ back.data.status.backupInformation.size.compressed | formatBytes }})
												</template>
											</td>
											<td class="postgresVersion" :class="[(back.data.status.process.status === 'Completed') ? 'pg'+(back.data.status.backupInformation.postgresVersion.substr(0,2)) : '']"  v-if="!isCluster" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.backupInformation.postgresVersion : ''">
												<template v-if="back.data.status.process.status === 'Completed'">
													{{ back.data.status.backupInformation.postgresVersion | prefix }}
												</template>											
											</td>
											<!--<td class="tested center icon" :class="[(back.data.status.tested) ? 'true' : 'false']"></td>-->
											<td class="name hasTooltip" :data-val="back.data.metadata.name">
												<span>{{ back.data.metadata.name }}</span>
											</td>
											<td class="clusterName hasTooltip" :class="back.data.spec.sgCluster" v-if="!isCluster" :data-val="back.data.spec.sgCluster">
												<span>{{ back.data.spec.sgCluster }}</span>
											</td>
										<td class="actions">
											<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)"  :to="'/admin/crd/edit/backup/'+$route.params.namespace+'/'+back.data.metadata.uid" title="Edit Backup">
												<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
											</router-link>
											<a v-if="iCan('delete','sgbackups',$route.params.namespace)"  v-on:click="deleteCRD('sgbackup',currentNamespace, back.data.metadata.name)" class="delete" title="Delete Backup">
												<svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
											</a>
										</td>
									</tr>
									<tr class="details" :class="[ $route.params.uid == back.data.metadata.uid ? 'open' : '', 'sgbackup-'+back.data.metadata.namespace+'-'+back.data.metadata.name]" :style="$route.params.uid == back.data.metadata.uid ? 'display: table-row;' : ''" v-if="back.data.status.process.status === 'Completed'">
										<td :colspan="(isCluster) ? 6 : 8">
											<div>
												<table>
													<thead>
														<th colspan="2">Backup Details</th>
													</thead>
													<tbody>
														<tr>
															<td class="label">
																Status
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.status.description"></span>
															</td>
															<td class="phase" :class="back.data.status.process.status">
																<span>{{ back.data.status.process.status }}</span>
															</td>
														</tr>
														<tr>
															<td class="label">
																Size uncompressed
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.size.uncompressed.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.size.uncompressed | formatBytes }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Size compressed
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.size.compressed.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.size.compressed | formatBytes }}
															</td>
														</tr>
														<tr>
															<td class="label">
																PG
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.postgresVersion.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.postgresVersion | prefix }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Name
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.metadata.name.description"></span>
															</td>
															<td>
																{{ back.data.metadata.name }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Source Cluster
																<span class="helpTooltip" :data-tooltip="tooltips.sgbackup.spec.sgCluster.description"></span>
															</td>
															<td>
																{{ back.data.spec.sgCluster }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Start Time
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.timing.start.description"></span>
															</td>
															<td class="timestamp">
																<span class='date'>
																	{{ back.data.status.process.timing.start | formatTimestamp('date') }}
																</span>
																<span class='time'>
																	{{ back.data.status.process.timing.start | formatTimestamp('time') }}
																</span>
																<span class='ms'>
																	{{ back.data.status.process.timing.start | formatTimestamp('ms') }} Z
																</span>
															</td>
														</tr>
														<tr>
															<td class="label">
																Elapsed
																<span class="helpTooltip" data-tooltip="Total time transcurred between the start time of backup and the time at which the backup is safely stored in the object storage."></span>
															</td>
															<td class="timestamp">
																<span class='time'>
																	{{ back.duration | formatTimestamp('time') }}
																</span>
																<span class='ms'>
																	{{ back.duration | formatTimestamp('ms') }}
																</span>
															</td>
														</tr>
														<tr>
															<td class="label">
																LSN (start ⇢ end)
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.lsn.start.description + ' ⇢ ' + tooltips.sgbackup.status.backupInformation.lsn.end.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.lsn.start }} ⇢ {{ back.data.status.backupInformation.lsn.end }}
															</td>
														</tr>
														<tr>
															<td class="label">
																UID
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.metadata.uid.description"></span>
															</td>
															<td colspan="2">
																{{ back.data.metadata.uid }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Source Pod
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.hostname.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.hostname }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Timeline
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.startWalFile.description"></span>
															</td>
															<td>
																{{ parseInt(back.data.status.backupInformation.startWalFile.substr(8)) }}
															</td>
														</tr>
														<tr>
															<td class="label">
																System Identifier
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.systemIdentifier.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.systemIdentifier }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Job Pod
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.jobPod.description"></span>
															</td>
															<td>
																{{ back.data.status.process.jobPod }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Managed Lifecycle (request)
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.spec.managedLifecycle.description"></span>
															</td>
															<td class="managedLifecycle" :class="[(back.data.spec.managedLifecycle) ? 'true' : 'false']" :data-val="back.data.spec.managedLifecycle"></td>
														</tr>
														<tr>
															<td class="label">
																Managed Lifecycle (status)
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.managedLifecycle.description"></span>
															</td>
															<td>
																{{ back.data.status.process.managedLifecycle ? 'Enabled' : 'Disabled' }}
															</td>
														</tr>
														<tr>
															<td class="label">
																End Time
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.timing.end.description"></span>
															</td>
															<td class="timestamp">
																<span class='date'>
																	{{ back.data.status.process.timing.end | formatTimestamp('date') }}
																</span>
																<span class='time'>
																	{{ back.data.status.process.timing.end | formatTimestamp('time') }}
																</span>
																<span class='ms'>
																	{{ back.data.status.process.timing.end | formatTimestamp('ms') }} Z
																</span>
															</td>
														</tr>
														<tr>
															<td class="label">
																Stored Time
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.process.timing.stored.description"></span>
															</td>
															<td class="timestamp">
																<span class='date'>
																	{{ back.data.status.process.timing.stored | formatTimestamp('date') }}
																</span>
																<span class='time'>
																	{{ back.data.status.process.timing.stored | formatTimestamp('time') }}
																</span>
																<span class='ms'>
																	{{ back.data.status.process.timing.stored | formatTimestamp('ms') }} Z
																</span>
															</td>
														</tr>
														<tr>
															<td class="label">
																Hostname
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.hostname.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.hostname }}
															</td>
														</tr>
														<tr>
															<td class="label">
																PG Data
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.pgData.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.pgData }}
															</td>
														</tr>
														<tr>
															<td class="label">
																Start Wal File
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.backupInformation.startWalFile.description"></span>
															</td>
															<td>
																{{ back.data.status.backupInformation.startWalFile }}
															</td>
														</tr>
														<tr v-if="(typeof back.data.status.backupInformation.controlData !== 'undefined')" class="controlData">
															<td class="label">
																Control Data
															</td>
															<td>
																<a @click="setContentTooltip('#controlData-'+index)"> 
																	View Control Data
																	<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
																</a>

																<div :id="'controlData-'+index" class="hidden">
																	<!--<pre>{{ back.data.status.backupInformation.controlData }}</pre>-->
																	<table>
																		<tr v-for="(value, key) in back.data.status.backupInformation.controlData">
																			<td class="label">{{ key }}</td>
																			<td class="value">{{ value }}</td>
																		</tr>
																	</table>
																</div>
															</td>
														</tr>
													</tbody>
												</table>
											</div>

											<div>
												<table>
													<thead>
														<th colspan="2" class="label">
															Storage Details
															<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.description"></span>
														</th>
													</thead>
													<tbody>
														<tr>
															<td class="label">
																Storage Type
																<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.type.description"></span>
															</td>
															<td>
																{{ back.data.status.sgBackupConfig.storage.type }}
															</td>
														</tr>
														<template v-if="back.data.status.sgBackupConfig.storage.type == 's3'">
															<tr>
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.bucket.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3.bucket }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3.path')">
																<td class="label">
																	Path
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.path.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3.path }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3.region')">
																<td class="label">
																	Region
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.region.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3.region }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3.storageClass')">
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.storageClass.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3.path }}
																</td>
															</tr>
															<tr>
																<td colspan="2" class="label">
																	AWS Credentials
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.awsCredentials.description"></span>
																</td>
															</tr>
															<tr>
																<td class="label">
																	Access Key ID
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.awsCredentials.secretKeySelectors.accessKeyId.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
															<tr>
																<td class="label">
																	Secret Access Key
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
														</template>
														<template v-else-if="back.data.status.sgBackupConfig.storage.type === 's3Compatible'">
															<tr>
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.bucket.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.bucket }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3Compatible.path')">
																<td class="label">
																	Path
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.path.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.path }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3Compatible.enablePathStyleAddressing')">
																<td class="label">
																	Path Style Addressing
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.enablePathStyleAddressing.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.enablePathStyleAddressing ? 'Enabled' : 'Disabled'}}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3Compatible.endpoint')">
																<td class="label">
																	Endpoint
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.endpoint.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.endpoint }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3Compatible.region')">
																<td class="label">
																	Region
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.region.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.region }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.s3Compatible.storageClass')">
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.storageClass.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.s3Compatible.path }}
																</td>
															</tr>
															<tr>
																<td colspan="2" class="label">
																	AWS Credentials
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.description"></span>
																</td>
															</tr>
															<tr>
																<td class="label">
																	Access Key ID
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
															<tr>
																<td class="label">
																	Secret Access Key
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
														</template>
														<template v-else-if="back.data.status.sgBackupConfig.storage.type === 'gcs'">
															<tr>
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.gcs.bucket.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.gcs.bucket }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.gcs.path')">
																<td class="label">
																	Path
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.gcs.path.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.gcs.path }}
																</td>
															</tr>
															<tr>
																<td colspan="2" class="label">
																	GCS Credentials
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.gcs.gcpCredentials.description"></span>
																</td>
															</tr>
															<tr>
																<td class="label">
																	Service Account JSON
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.gcs.gcpCredentials.serviceAccountJSON.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
														</template>
														<template v-else-if="back.data.status.sgBackupConfig.storage.type === 'azureBlob'">
															<tr>
																<td class="label">
																	Bucket
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.azureBlob.bucket.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.azureBlob.bucket }}
																</td>
															</tr>
															<tr v-if="hasProp(back, 'data.status.sgBackupConfig.storage.azureBlob.path')">
																<td class="label">
																	Path
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.azureBlob.path.description"></span>
																</td>
																<td>
																	{{ back.data.status.sgBackupConfig.storage.azureBlob.path }}
																</td>
															</tr>
															<tr>
																<td colspan="2" class="label">
																	Azure Credentials
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.description"></span>
																</td>
															</tr>
															<tr>
																<td class="label">
																	Storage Account
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
															<tr>
																<td class="label">
																	Access Key
																	<span  class="helpTooltip" :data-tooltip="tooltips.sgbackup.status.sgBackupConfig.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey.description"></span>
																</td>
																<td>
																	********
																</td>
															</tr>
														</template>
													</tbody>
												</table>
											</div>
										</td>
									</tr>
									<tr class="details Running" :class="'backup-'+back.data.metadata.namespace+'-'+back.data.metadata.name" v-else-if="back.data.status.process.status === 'Running'">
										<td :colspan="(isCluster) ? 6 : 8" class="center">
											<strong>Backup Running</strong><br/>
										</td>
									</tr>
									<tr class="details Failed" :class="[ $route.params.uid == back.data.metadata.uid ? 'open' : '', 'sgbackup-'+back.data.metadata.namespace+'-'+back.data.metadata.name]" :style="$route.params.uid == back.data.metadata.uid ? 'display: table-row;' : ''" v-else-if="back.data.status.process.status === 'Failed'">
										<td :colspan="(isCluster) ? 6 : 8" class="center">
											<strong>Failure Cause</strong><br/>
											<vue-markdown :source="back.data.status.process.failure"></vue-markdown>
										</td>
									</tr>
								</template>
								<template v-else>
									<tr>
										<td></td>
										<td class="managedLifecycle center icon" :class="[(back.data.spec.managedLifecycle) ? 'true' : 'false']" :data-val="back.data.spec.managedLifecycle"></td>
										<td class="phase center Pending">
											<span>Pending</span>
										</td>
										<td></td>
										<td></td>
										<td>{{ back.name }}</td>
										<td>{{ back.data.spec.sgCluster }}</td>
										<td></td>
									</tr>
								</template>
							</template>
						</tbody>
					</table>
				</div>
			</div>
			<div id="nameTooltip">
				<div class="info"></div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.status.process.timing.stored',
			currentSortDir: 'desc',
			clusterName: '',
			keyword: '',
			managedLifecycle: [],
			status: [],
			postgresVersion: [],
			tested: [],
			datePicker: '',
			dateStart: '',
			dateEnd: ''
		}
	},
	computed: {

		backups () {
			return sortTable( store.state.backups, this.currentSort, this.currentSortDir)
			//return store.state.backups
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		clusters () {
			return store.state.clusters
		},

		isCluster() {
			return this.$route.params.hasOwnProperty('cluster')
		},

		tooltips() {
			return store.state.tooltips
		}

	},
	mounted: function() {
		
	},
	methods: {

		toggleClear( filter ){

			switch(filter) {
				case 'keyword':
					if($('#keyword').val().length)
						$('.searchBar .clear').fadeIn()
					else
						$('.searchBar .clear').fadeOut()
					
						break;
				case 'filters':
					if($('.filters .options .active').length)
						$('.filters .clear').fadeIn()
					else
						$('.filters .clear').fadeOut()
			}

		},

		clearFilters: function(section) {
			if(section == 'filters') {
				this.clusterName = '';
				this.managedLifecycle = [];
				this.status = [];
				this.postgresVersion = [];
				this.tested = [];
				$('.filter.open .active').removeClass('active');

				$('.filters .clear').fadeOut()

			} else if (section == 'keyword') {
				this.keyword = '';
				$('#keyword').removeClass('active')

				$('.searchBar .clear').fadeOut()
			}

			this.filterBackups();
		},

		filterBackups: function() {

			//console.log("filterBackups");

			let vc = this;
			var count = 0;

			//if( vc.keyword.length || vc.managedLifecycle || vc.datePicker.length || vc.status.length || vc.postgresVersion.length || vc.clusterName.length ) {
				
				store.state.backups.forEach( function(bk, index) {
					
					let show = true;

					// Filter by Keyword
					if(vc.keyword.length && show) {
						
						let text = JSON.stringify(bk)
						
						// console.log("KEYWORD");
						// console.log(text)
						// console.log(vc.keyword)
						// console.log("EXISTS: "+text.indexOf(vc.keyword));

						show = !(text.indexOf(vc.keyword) === -1);
					}
					
					//Filter by managedLifecycle
					if(vc.managedLifecycle.length && show){
						let isPerm = bk.data.spec.managedLifecycle;
						show = (isPerm.toString() === vc.managedLifecycle[0]);
					}
	
					// Filter by Date
					if( vc.datePicker.length && show && (bk.data.status.process.status !== 'Failed') ){
						let timestamp = moment(bk.data.status.process.timing.stored, 'YYYY-MM-DD HH:mm:ss');
						let start = moment(vc.dateStart, 'YYYY-MM-DD HH:mm:ss');
						let end = moment(vc.dateEnd, 'YYYY-MM-DD HH:mm:ss');
						
						// console.log(timestamp);
						// console.log(start);
						// console.log(end);
						// console.log(timestamp.isBetween( start, end, null, '[]' ));
	
						show = timestamp.isBetween( start, end, null, '[]' );
					} else if (vc.datePicker.length && (bk.data.status.process.status === 'Failed'))
						show = false;
	
					//Filter by Status
					if(vc.status.length && show)
						show = (vc.status.includes(bk.data.status.process.status));
	
					//Filter by postgresVersion
					if(vc.postgresVersion.length && show && (bk.data.status.process.status === 'Completed') )
						show = (bk.data.status.backupInformation.postgresVersion.substr(0,2)=== vc.postgresVersion[0]);
					else if(vc.postgresVersion.length && (bk.data.status.process.status !== 'Completed') )
						show = false;
					
					//Filter by clusterName
					if(vc.clusterName.length && show)
						show = (vc.clusterName == bk.data.spec.sgCluster);
	
					if(bk.show != show) {
						store.commit('showBackup',{
							pos: index,
							isVisible: show
						});
					}
	
				});
	
				//console.log(store.state.backups);
		
			/* } else {
				console.log("SHOW ALL");

				store.state.backups.forEach( function(item, index) {
					store.commit('showBackup',{
						pos: index,
						isVisible: true
					});
				});
			}*/

			//console.log("#"+count);
			return store.state.backups;
			
		}
	},

	mounted: function() {

		const vc = this;
		
		//vc.filterBackups();

		$(document).ready(function(){
			$('#datePicker').daterangepicker({
				"autoApply": true,
				"timePicker": true,
				"timePicker24Hour": true,
				"timePickerSeconds": true,
				//"startDate": "04/02/2020",
				//"endDate": "04/08/2020",
				"opens": "left",
				locale: {
					cancelLabel: "Clear"
				}
			}, function(start, end, label) {
				vc.dateStart = start.format('YYYY-MM-DD HH:mm:ss');
				vc.dateEnd = end.format('YYYY-MM-DD HH:mm:ss');
				vc.datePicker = vc.dateStart+' / '+vc.dateEnd;
				vc.filterBackups();
			});
			
			$(document).on('click', '.toggle.date.open', function(){
				//$('#datePicker').trigger('hide.daterangepicker');
				//console.log('.toggle.date.open');
				/*
				if(vc.datePicker.length)
					$('.applyBtn').click();
				else
					$('.cancelBtn').click();
				*/
			});

			$('#datePicker').on('show.daterangepicker', function(ev, picker) {
				//console.log('show.daterangepicker');
				$('#datePicker').parent().addClass('open');
			});

			$('#datePicker').on('hide.daterangepicker', function(ev, picker) {
				//console.log('hide.daterangepicker');
				$('#datePicker').parent().removeClass('open');
			});

			$('#datePicker').on('cancel.daterangepicker', function(ev, picker) {
				//console.log('cancel.daterangepicker');
				vc.datePicker = '';
				$('#datePicker').parent().removeClass('open');
				vc.filterBackups();
			});

			$('#datePicker').on('apply.daterangepicker', function(ev, picker) {
				//console.log('apply.daterangepicker');
				$('#datePicker').parent().removeClass('open');
			});

			$(document).on('change', '.filter select', function () {
				if($(this).val().length)
					$(this).addClass('active')
				else
					$(this).removeClass('active')
			});

			$(document).on('click', 'input[type="checkbox"]', function () {

				if($(this).is(':checked')) {
					$(this).addClass('active');

					//console.log("L: "+vc[$(this).data('filter')].length);
					if($(this).hasClass("xCheckbox")) {
						$(this).parents('li').find(':checked:not(#'+$(this).prop('id')+')').removeClass('active').prop('checked', false);
					
						if(vc[$(this).data('filter')].length)
							vc[$(this).data('filter')] = [$(this).val()];
					}
					
				} else {
					$(this).removeClass('active');
					
					if($(this).hasClass("xCheckbox"))
						vc[$(this).data('filter')] = [];
				}

				vc.toggleClear('filters');
				
			});		

			$('tr.base').click(function() {
				$(this).find('td.name').toggleClass("hasTooltip");
				$(this).find('td.clusterName').toggleClass("hasTooltip");
			});

			$(document).on("click", "table.backups tr.base a.open", function(){
				$(this).parent().parent().next().toggle().addClass("open");
				$(this).parent().parent().toggleClass("open");
			});

			$(document).on("click", "table.backups tr.base td:not(.actions)", function() {
				if(!$(this).parent().hasClass('open')) {
					if(vc.$route.params.hasOwnProperty('cluster'))
						router.push('/admin/cluster/backups/'+store.state.currentNamespace+'/'+$(this).parent().data('cluster')+'/'+$(this).parent().data('uid'))
					else
						router.push('/admin/backups/'+store.state.currentNamespace+'/'+$(this).parent().data('cluster')+'/'+$(this).parent().data('uid'))
				}
				else {
					if(vc.$route.params.hasOwnProperty('cluster'))
						router.push('/admin/cluster/backups/'+store.state.currentNamespace+'/'+$(this).parent().data('cluster'))
					else
						router.push('/admin/backups/'+store.state.currentNamespace)
				}
			});

		});

	}
})
