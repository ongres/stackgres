var Backups = Vue.component("sg-backup", {
	template: `
		<div id="sg-backup">
			<header v-if="isCluster">
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						StackGres Clusters
					</li>
					<li>
						{{ $route.params.name }}
					</li>
				</ul>

				<div class="actions">
					<router-link :to="'/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link> <a v-on:click="deleteCRD('cluster', currentNamespace, currentCluster.name, '/overview/'+currentNamespace)" :class="'/overview/'+currentNamespace">Delete Cluster</a>
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="currentCluster.hasBackups">
						<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="currentCluster.data.graffanaEmbedded">
						<router-link id="grafana-btn" :to="'/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>
			<header v-else>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
						<template v-if="$route.params.name !== undefined">
							{{ $route.params.name }}
						</template>
						<template v-else>
							Cluster Backups
						</template>
					</li>
				</ul>
				<router-link :to="'/crd/create/backup/'+currentNamespace" class="add">Add New</router-link>
			</header>

			<div class="content">
				<div id="backups">
					<div class="toolbar">
						<input id="keyword" v-model="keyword" @keyup="filterTable" class="search" placeholder="Search Backup...">

						<div class="filter">
							<span class="toggle">FILTER</span>

							<ul class="options">
								<li>
									<span>Permanent</span>
									<label for="isPermanent">
										<input v-model="isPermanent" type="checkbox" id="isPermanent" name="isPermanent" value="true" @change="filterTable"/>
										<span>YES</span>
									</label>
									<label for="notPermanent">
										<input v-model="isPermanent" type="checkbox" id="notPermanent" name="isPermanent" value="false"  @change="filterTable"/>
										<span>NO</span>
									</label>
								</li>

								<li>
									<span>Phase</span>
									<label for="isCompleted">
										<input v-model="phase" type="checkbox" id="isCompleted" name="phase" value="Completed" @change="filterTable"/>
										<span>Completed</span>
									</label>
									<label for="notCompleted">
										<input v-model="phase" type="checkbox" id="notCompleted" name="phase" value="Pending" @change="filterTable"/>
										<span>Pending</span>
									</label>
								</li>

								<li v-if="!isCluster">
									<span>Postgres Version</span>
									<label for="pg11">
										<input v-model="pgVersion" type="checkbox" id="pg11" name="pg11" value="pg11" @change="filterTable" />
										<span>11</span>
									</label>
									<label for="pg12">
										<input v-model="pgVersion" type="checkbox" id="pg12" name="pg12" value="pg12" @change="filterTable" />
										<span>12</span>
									</label>
								</li>
								
								<li>
									<span>Tested</span>
									<label for="isTested">
										<input v-model="tested" type="checkbox" id="isTested" name="tested" value="true" @change="filterTable" />
										<span>YES</span>
									</label>
									<label for="notTested">
										<input v-model="tested" type="checkbox" id="notTested" name="tested" value="false" @change="filterTable" />
										<span>NO</span>
									</label>
								</li>

								<li v-if="!isCluster">
									<span>Cluster</span>
									<select v-model="clusterName" @change="filterTable">
										<option value="">All Clusters</option>
										<template v-for="cluster in allClusters">
											<option v-if="cluster.data.metadata.namespace == currentNamespace">{{ cluster.data.metadata.name }}</option>
										</template>
									</select>
								</li>
							</ul>
						</div>
					</div>
					<table>
						<thead class="sort">
							<th @click="sort('data.status.time')" class="sorted desc timestamp">
								<span>Timestamp</span>
							</th>
							<th @click="sort('data.spec.isPermanent')" class="icon desc isPermanent">
								<span>Permanent</span>
							</th>
							<th @click="sort('data.status.phase')" class="desc phase center">
								<span>Phase</span>
							</th>
							<th @click="sort('data.status.uncompressedSize')" class="desc size">
								<span>Size</span>
							</th>
							<th @click="sort('data.status.pgVersion')" class="desc pgVersion" v-if="!isCluster">
								<span>PG</span>
							</th>
							<th @click="sort('data.status.tested')" class="icon desc tested">
								<span>Tested</span>
							</th>
							<th @click="sort('data.metadata.name')" class="desc name">
								<span>Name</span>
							</th>
							<th @click="sort('data.spec.cluster')" class="desc clusterName" v-if="!isCluster">
								<span>Cluster Name</span>
							</th>
							<th class="actions"></th>
							<!--<th class="details"></th>-->
						</thead>
						<tbody>
							<tr class="no-results">
								<td :colspan="(isCluster) ? 7 : 9">
									No records matched your search terms
								</td>
							</tr>
							<template v-for="back in backups" v-if="( ( (back.data.metadata.namespace == currentNamespace) && !isCluster ) || (isCluster && (back.data.spec.cluster == currentCluster.name ) && (back.data.metadata.namespace == currentCluster.data.metadata.namespace ) ) )">
								<tr class="base" :class="back.data.status.phase">
										<td class="timestamp">
											<template v-if="back.data.status.phase == 'Completed'">
												<span class='date'>
													{{ back.data.status.time | formatTimestamp('date') }}
												</span>
												<span class='time'>
													{{ back.data.status.time | formatTimestamp('time') }}
												</span>
												<span class='ms'>
													{{ back.data.status.time | formatTimestamp('ms') }} Z
												</span>
											</template>
										</td>
										<td class="isPermanent center icon" :class="[(back.data.spec.isPermanent) ? 'true' : 'false']"></td>
										<td class="phase center" :class="back.data.status.phase">
											<span>{{ back.data.status.phase }}</span>
										</td>
										<td class="size">
											<template v-if="back.data.status.phase === 'Completed'">
												{{ back.data.status.uncompressedSize | formatBytes }}
											</template>
										</td>
										<td class="pgVersion" :class="[(back.data.status.phase === 'Completed') ? 'pg'+(back.data.status.pgVersion.substr(0,2)) : '']"  v-if="!isCluster">
											<template v-if="back.data.status.phase === 'Completed'">
												{{ back.data.status.pgVersion | prefix }}
											</template>											
										</td>
										<td class="tested center icon" :class="[(back.data.status.tested) ? 'true' : 'false']"></td>
										<td class="name">{{ back.data.metadata.name }}</td>
										<td class="clusterName" :class="back.data.spec.cluster" v-if="!isCluster">{{ back.data.spec.cluster }}</td>
									<td class="actions">
										<a class="open" title="Backup Details">
											<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
										</a>
										<router-link :to="'/crd/edit/backup/'+$route.params.namespace+'/'+back.data.spec.cluster+'/'+back.name" title="Edit Backup">
											<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
										</router-link>
										<a v-on:click="deleteCRD('backup',currentNamespace, back.data.metadata.name)" class="delete" title="Delete Backup">
											<svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
										</a>
									</td>
								</tr>
								<tr class="details" v-if="back.data.status.phase === 'Completed'">
									<td :colspan="(isCluster) ? 7 : 9">
										<!--<h4>Backup Details</h4>-->

										<table>
											<thead>
												<th>Start Time</th>
												<th>Duration</th>
												<th>LSN (Start - End)</th>
												<th colspan="2">UID</th>
												<th>Source Cluster</th>
												<th>Compressed Size</th>
												<th>Compression Method</th>
												<th>Storage Type</th>
											</thead>
											<tbody>
												<tr>
													<td class="timestamp">
														<span class='date'>
															{{ back.data.status.startTime | formatTimestamp('date') }}
														</span>
														<span class='time'>
															{{ back.data.status.startTime | formatTimestamp('time') }}
														</span>
														<span class='ms'>
															{{ back.data.status.startTime | formatTimestamp('ms') }} Z
														</span>
													</td>
													<td class="timestamp">
														<span class='time'>
															{{ back.duration | formatTimestamp('time') }}
														</span>
														<span class='ms'>
															{{ back.duration | formatTimestamp('ms') }}
														</span>
													</td>
													<td>
														({{ back.data.status.startLsn }} - {{ back.data.status.finishLsn }})
													</td>
													<td colspan="2">
														{{ back.data.metadata.uid }}
													</td>
													<td>
														{{ back.data.status.hostname }}
													</td>
													<td>
														{{ back.data.status.compressedSize | formatBytes }}
													</td>
													<td>
														{{ back.data.status.backupConfig.compressionMethod }}
													</td>
													<td>
														{{ back.data.status.backupConfig.storage.type }}
													</td>
												</tr>
											</tbody>
										</table>
										<table>
											<thead>
												<th>Backup Configuration</th>
											</thead>
											<tbody>
												<tr>
													<td>
														<ul class="yaml">
															<template v-if="back.data.status.backupConfig.storage.type === 's3'">
																<li>
																	<strong class="label">credentials:</strong> 
																	<ul>
																		<li>
																			<strong class="label">accessKey:</strong>																				
																			<ul>
																				<li><strong class="label">key:</strong> {{ back.data.status.backupConfig.storage.s3.credentials.accessKey.key }}</li>
																				<li><strong class="label">name:</strong> {{ back.data.status.backupConfig.storage.s3.credentials.accessKey.name }}</li>
																			</ul>
																		</li>
																		<li>
																			<strong class="label">secretKey:</strong>																				
																			<ul>
																				<li><strong class="label">key:</strong> {{ back.data.status.backupConfig.storage.s3.credentials.secretKey.key }}</li>
																				<li><strong class="label">name:</strong> {{ back.data.status.backupConfig.storage.s3.credentials.secretKey.name }}</li>
																			</ul>
																		</li>
																	</ul>
																</li>
																<li>
																	<strong class="label">endpoint:</strong> {{ back.data.status.backupConfig.storage.s3.endpoint }}
																</li>
																<li>
																	<strong class="label">forcePathStyle:</strong> {{ back.data.status.backupConfig.storage.s3.forcePathStyle }}
																</li>
																<li>
																	<strong class="label">prefix:</strong> {{ back.data.status.backupConfig.storage.s3.prefix }}
																</li>
																<li>
																	<strong class="label">region:</strong> {{ back.data.status.backupConfig.storage.s3.region }}
																</li>

															</template>
															<template v-else-if="back.data.status.backupConfig.storage.type === 's3compatible'">
																<li>
																	<strong class="label">credentials:</strong> 
																	<ul>
																		<li>
																			<strong class="label">accessKeySelector:</strong>																				
																			<ul>
																				<li><strong class="label">name:</strong> {{ back.data.status.backupConfig.storage.s3compatible.credentials.accessKeySelector.name }}</li>
																				<li><strong class="label">key:</strong> {{ back.data.status.backupConfig.storage.s3compatible.credentials.accessKeySelector.key }}</li>
																			</ul>
																		</li>
																		<li>
																			<strong class="label">secretKeySelector:</strong>																				
																			<ul>
																				<li><strong class="label">name:</strong> {{ back.data.status.backupConfig.storage.s3compatible.credentials.secretKeySelector.name }}</li>
																				<li><strong class="label">key:</strong> {{ back.data.status.backupConfig.storage.s3compatible.credentials.secretKeySelector.key }}</li>
																			</ul>
																		</li>
																	</ul>
																</li>
																<!--<li>
																	<strong class="label">endpoint:</strong> {{ back.data.status.backupConfig.storage.s3.endpoint }}
																</li>
																<li>
																	<strong class="label">forcePathStyle:</strong> {{ back.data.status.backupConfig.storage.s3.forcePathStyle }}
																</li>
																<li>
																	<strong class="label">prefix:</strong> {{ back.data.status.backupConfig.storage.s3.prefix }}
																</li>
																<li>
																	<strong class="label">region:</strong> {{ back.data.status.backupConfig.storage.s3.region }}
																</li>-->

															</template>													
															<li v-else v-for="(item, index) in back.data.status.backupConfig.storage[back.data.status.backupConfig.storage.type]">
																<strong class="label">{{ index }}:</strong> {{ item }}
															</li>
														</ul>
													</td>
												</tr>
											</tbody>
										</table>
										

										<!--<hr>
										<span>UID</span>
										{{ back.data.metadata.uid }}

										<hr>
										<span>Pod</span>
										{{ back.data.status.pod }}

										<hr>
										<span>Name</span>
										{{ back.data.status.name }}

										<hr>
										<span>WAL File Name</span>
										{{ back.data.status.walFileName }}

										<hr>
										<span>Start Time</span>
										{{ back.data.status.startTime }}

										<hr>
										<span>Finish Time</span>
										{{ back.data.status.finishTime }}
										
										<hr>
										<span>Hostname</span>
										{{ back.data.status.hostname }}

										<hr>
										<span>Data Directory</span>
										{{ back.data.status.dataDir }}
										
										<hr>
										<span>Start Lsn</span>
										{{ back.data.status.startLsn }}

										<hr>
										<span>Finish Lsn</span>
										{{ back.data.status.finishLsn }}

										<hr>
										<span>System Identifier</span>
										{{ back.data.status.systemIdentifier }}

										<hr>
										<span>Compressed size</span>
										{{ back.data.status.compressedSize | formatBytes }}

										<hr>
										<h4 class="basic">Backup Configuration</h4>

										<hr>
										<span>Type</span>
										{{ back.data.status.backupConfig.storage.type }}

										<hr>
										<span>Compression Method</span>
										{{ back.data.status.backupConfig.compressionMethod }}-->
									</td>
								</tr>
							</template>
						</tbody>
					</table>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.status.time',
			currentSortDir: 'desc',
			clusterName: '',
			keyword: '',
			isPermanent: [],
			phase: [],
			pgVersion: [],
			tested: [],
		}
	},
	computed: {

		backups () {
			//return store.state.backups
			return sortTable( store.state.backups, this.currentSort, this.currentSortDir )
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		allClusters () {
			return store.state.clusters
		},

		currentCluster () {
			return store.state.currentCluster
		},

		isCluster() {
			return  vm.$route.params.cluster !== undefined
		}

	},
	methods: {
		
		sort: function(s) {
			
			//if s == current sort, reverse
			if(s === this.currentSort) {
			  this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
			}
			this.currentSort = s;

		},

		deleteBackup: function(backupName, backupNamespace) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const backup = {
					name: backupName,
					namespace: backupNamespace
				}
	
				const res = axios
				.delete(
					apiURL+'backup/', 
					{
						data: {
							"metadata": {
								"name": backup.name,
								"namespace": backup.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Backup <strong>'+backupName+'</strong> deleted successfully', 'message');
					vm.fetchAPI();
					//router.push('/backups/'+store.state.currentNamespace);                        
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		},

		filterTable: function() {

			let bk = this;

			$("table tr.base").each(function () {

				let show = true;
				let r = $(this);
				let checkFilters = ['isPermanent', 'phase', 'pgVersion', 'tested'];

				// Filter by Keyword
				if(bk.keyword.length && (r.text().toLowerCase().indexOf(bk.keyword.toLowerCase()) === -1) )
					show = false;

				checkFilters.forEach(function(f){

					if(bk[f].length){
						let hasClass = 0;

						bk[f].forEach(function(c){
							if(r.children('.'+c).length)
								hasClass++;
						});

						if(!hasClass)
							show = false;
					}
					
				})

				/* //Filter by isPermanent
				if(bk.isPermanent.length && (!r.children(".isPermanent."+bk.isPermanent).length))
					show = false;

				//Filter by phase
				if(bk.phase.length && (!r.children(".phase."+bk.phase).length))
					show = false;

				//Filter by pgVersion
				if(bk.pgVersion.length){

					let hasClass = 0;
					
					bk.pgVersion.forEach(function(item){
						if(r.children('.'+item).length)
							hasClass++;
					});

					if(hasClass < 1)
						show = false;

				}

				//Filter by tested
				if(bk.tested.length && (!r.children(".tested."+bk.tested).length))
					show = false; */

				//Filter by clusterName
				if(bk.clusterName.length && (!r.children(".clusterName."+bk.clusterName).length))
					show = false;

				if(!show)
					r.addClass("not-found");
				else
					r.removeClass("not-found");
				
				if(!$("tr.base:not(.not-found)").length)
					$("tr.no-results").show();
				else
					$("tr.no-results").hide();

			});
			
		}
	}
})
