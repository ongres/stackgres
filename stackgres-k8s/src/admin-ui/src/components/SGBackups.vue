<template>
	<div id="sg-backup" v-if="iCanLoad">
		<div class="content" :class="isCluster ? 'cluster' : ''">
			<template v-if="!$route.params.hasOwnProperty('backupname')">
				<div id="backups">
					<div class="toolbar">
						<div class="searchBar">
							<input id="keyword" v-model="filters.keyword" class="search" placeholder="Search Backup..." autocomplete="off">
							<button @click="filterBackups('keyword')" class="btn">APPLY</button>
							<button @click="clearFilters('keyword')" class="btn clear border keyword" v-if="filters.keyword.length">CLEAR</button>
						</div>

						<router-link v-if="isCluster && iCan('create','sgbackups',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/sgbackups/new'" title="Add New Backup" class="btn addClusterBackup">Add Backup</router-link>

						<div class="filter" :class="(filters.dateStart.length && filters.dateEnd.length) ? 'filtered' : ''">
							<span class="toggle date">DATE RANGE <input v-model="filters.datePicker" id="datePicker" autocomplete="off" @focus="initDatePicker()" /></span>
						</div>

						<div class="filter filters">
							<span class="toggle" :class="isFiltered ? 'active' : ''">FILTER</span>

							<ul class="options">
								<li>
									<span>Managed Lifecycle (request)</span>
									<label for="managedLifecycle">
										<input v-model="filters.managedLifecycle" data-filter="managedLifecycle" type="checkbox" class="xCheckbox" id="managedLifecycle" name="managedLifecycle" value="true"/>
										<span>YES</span>
									</label>
									<label for="notPermanent">
										<input v-model="filters.managedLifecycle" data-filter="managedLifecycle" type="checkbox" class="xCheckbox" id="notPermanent" name="notPermanent" value="false"/>
										<span>NO</span>
									</label>
								</li>

								<li>
									<span>Status</span>
									<label for="isCompleted">
										<input v-model="filters.status" data-filter="status" type="checkbox" id="isCompleted" name="isCompleted" value="Completed"/>
										<span>Completed</span>
									</label>
									<label for="notCompleted">
										<input v-model="filters.status" data-filter="status" type="checkbox" id="notCompleted" name="notCompleted" value="Running"/>
										<span>Running</span>
									</label>
									<label for="backupPending">
										<input v-model="filters.status" data-filter="status" type="checkbox" id="backupPending" name="backupPending" value="Pending"/>
										<span>Pending</span>
									</label>
									<label for="backupFailed">
										<input v-model="filters.status" data-filter="status" type="checkbox" id="backupFailed" name="backupFailed" value="Failed"/>
										<span>Failed</span>
									</label>
								</li>

								<li v-if="!isCluster">
									<span>Postgres Version</span>
									<label for="pg11">
										<input v-model="filters.postgresVersion" data-filter="postgresVersion" type="checkbox" class="xCheckbox" id="pg11" name="pg11" value="11"/>
										<span>11</span>
									</label>
									<label for="pg12">
										<input v-model="filters.postgresVersion" data-filter="postgresVersion" type="checkbox" class="xCheckbox" id="pg12" name="pg12" value="12"/>
										<span>12</span>
									</label>
								</li>
								<li v-if="!isCluster">
									<span>Cluster</span>
									<select v-model="filters.clusterName">
										<option value="">All Clusters</option>
										<template v-for="cluster in clusters">
											<option v-if="cluster.data.metadata.namespace == $route.params.namespace">{{ cluster.data.metadata.name }}</option>
										</template>
									</select>
								</li>

								<li>
									<hr>
									<a class="btn" @click="filterBackups('others')">APPLY</a> <a class="btn clear border" @click="clearFilters('others')" v-if="isFiltered">CLEAR</a>
								</li>
							</ul>
						</div>
					</div>
					<table class="backups resizable fullWidth" v-columns-resizable>
						<thead class="sort">
							<th class="sorted desc timestamp hasTooltip" data-type="timestamp">
								<span @click="sort('data.status.process.timing.stored','timestamp')" title="Timestamp">Timestamp</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
							</th>
							<th class="desc managedLifecycle hasTooltip" data-type="lifecycle">
								<span @click="sort('data.spec.managedLifecycle')" title="Managed Lifecycle (request)">Managed Lifecycle (request)</span>
								<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.managedLifecycle')"></span>
							</th>
							<th class="desc phase center hasTooltip" data-type="phase">
								<span @click="sort('data.status.process.status')" title="Status">Status</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.status')"></span>
							</th>
							<th class="desc size hasTooltip textRight">
								<span @click="sort('data.status.backupInformation.size.uncompressed', 'memory')" title="Size uncompressed (compressed)">Size uncompressed (compressed)</span>
								<span class="helpTooltip" data-tooltip="Size (in bytes) of the uncompressed backup (Size (in bytes) of the compressed backup)."></span>
							</th>
							<th class="desc postgresVersion hasTooltip" v-if="!isCluster" data-type="version">
								<span@click="sort('data.status.backupInformation.postgresVersion')" title="Postgres Version">PG</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.postgresVersion')"></span>
							</th>
							<th class="desc name hasTooltip">
								<span @click="sort('data.metadata.name')" title="Name">Name</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.name')"></span>
							</th>
							<th class="desc clusterName hasTooltip" v-if="!isCluster">
								<span @click="sort('data.spec.sgCluster')" title="Source Cluster">Source Cluster</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.sgCluster')"></span>
							</th>
							<th class="actions"></th>
						</thead>
						<tbody>
							<template v-if="!backups.length">
								<tr class="no-results">
									<td :colspan="999" v-if="iCan('create','sgbackups',$route.params.namespace)">
										No backups matched your search terms, would  you like to <router-link :to="'/' + $route.params.namespace + '/sgbackups/new'" title="Add New Backup">create a new one?</router-link>
									</td>
									<td v-else colspan="999">
										No backups have been found. You don't have enough permissions to create a new one
									</td>
								</tr>
							</template>	
							<template v-for="(back, index) in backups">
								<template v-if="(index >= pagination.start) && (index < pagination.end)">
									<template v-if="( hasProp(back, 'data.status.process.status') && (back.data.status.process.status !== 'Pending') )">
										<tr class="base">
											<td class="timestamp hasTooltip" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.process.timing.stored.substr(0,19).replace('T',' ') : ''">
												<span>
													<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
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
															<span class='tzOffset'>{{ showTzOffset() }}</span>
														</template>
													</router-link>
												</span>
											</td>
											<td class="managedLifecycle center icon hasTooltip" :class="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle.toString() : 'false'" :data-val="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle : 'false'">
												<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
													<span></span>
												</router-link>
											</td>
											<td class="phase center" :class="back.data.status.process.status">
												<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
													<span>
														{{ back.data.status.process.status }}
													</span>
												</router-link>
											</td>
											<td class="size hasTooltip textRight" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.backupInformation.size.uncompressed : ''">
												<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
													<span>
														<template v-if="back.data.status.process.status === 'Completed'">
															{{ back.data.status.backupInformation.size.uncompressed | formatBytes }} ({{ back.data.status.backupInformation.size.compressed | formatBytes }})
														</template>
													</span>
												</router-link>
											</td>
											<td class="postgresVersion hasTooltip" :class="[(back.data.status.process.status === 'Completed') ? 'pg'+(back.data.status.backupInformation.postgresVersion.substr(0,2)) : '']"  v-if="!isCluster" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.backupInformation.postgresVersion : ''">
												<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
													<span>
														<template v-if="back.data.status.process.status === 'Completed'">
															{{ back.data.status.backupInformation.postgresVersion | prefix }}
														</template>
													</span>
												</router-link>
											</td>
											<td class="name hasTooltip" :data-val="back.data.metadata.name">
												<span>
													<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
														{{ back.data.metadata.name }}
													</router-link>
												</span>
											</td>
											<td class="clusterName hasTooltip" :class="back.data.spec.sgCluster" v-if="!isCluster" :data-val="back.data.spec.sgCluster">
												<span>
													<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
														{{ back.data.spec.sgCluster }}
													</router-link>
												</span>
											</td>
											<td class="actions">
												<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" target="_blank" class="newTab"></router-link>
												<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)"  :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackup/' + back.data.metadata.name + '/edit'" title="Edit Backup" class="editCRD"></router-link>
												<a v-if="iCan('delete','sgbackups',$route.params.namespace)"  @click="deleteCRD('sgbackups',$route.params.namespace, back.data.metadata.name)" class="delete deleteCRD" title="Delete Backup"></a>
											</td>
										</tr>
									</template>
									<template v-else>
										<tr class="base pending">
											<td class="timestamp"></td>
											<td class="managedLifecycle center icon" :class="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle.toString() : 'false'" :data-val="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle : 'false'"></td>
											<td class="phase center Pending">
												<span>Pending</span>
											</td>
											<td class="size"></td>
											<td class="postgresVersion" v-if="!isCluster"></td>
											<td class="name hasTooltip">
												<span>{{ back.name }}</span>
											</td>
											<td class="clusterName hasTooltip stackgres" v-if="!isCluster">
												<span>{{ back.data.spec.sgCluster }}</span>
											</td>
											<td class="actions">
												<a target="_blank" class="newTab disabled"></a>
												<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)" :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackup/' + back.data.metadata.name + '/edit'" title="Edit Backup" class="editCRD"></router-link>
												<a v-if="iCan('delete','sgbackups',$route.params.namespace)" @click="deleteCRD('sgbackups',$route.params.namespace, back.data.metadata.name)" class="delete deleteCRD" title="Delete Backup"></a>
											</td>
										</tr>
									</template>
								</template>
							</template>
						</tbody>
					</table>
					<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < backups.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="backups.length" @page-change="pageChange" align="center" ref="page"></v-page>
					<div id="nameTooltip">
						<div class="info"></div>
					</div>
				</div>
			</template>
			<template v-else>
				<template v-for="back in backups" v-if="back.name == $route.params.backupname">
					<div class="relative">
						<h2>Backup Details</h2>
						<template v-if="isCluster">
							<div class="titleLinks crdActionLinks">
								<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)"  :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackup/' + back.data.metadata.name + '/edit'" title="Edit Backup">
									Edit Backup
								</router-link>
								<a v-if="iCan('delete','sgbackups',$route.params.namespace)" @click="deleteCRD('sgbackups',$route.params.namespace, $route.params.backupname, '/' + $route.params.namespace + '/sgbackups')" title="Delete Backup" class="deleteCRD">
									Delete Backup
								</a>
								<router-link :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackups'" title="Close Details">Close Details</router-link>
							</div>
						</template>
					</div>

					<template v-if="back.data.status.process.status === 'Completed'">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">
										Name
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.name')"></span>
									</td>
									<td>
										{{ back.name }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Status
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.status')"></span>
									</td>
									<td class="phase" :class="back.data.status.process.status">
										<span>{{ back.data.status.process.status }}</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										Size uncompressed
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.size.uncompressed')"></span>
									</td>
									<td class="textRight">
										{{ back.data.status.backupInformation.size.uncompressed | formatBytes }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Size compressed
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.size.compressed')"></span>
									</td>
									<td class="textRight">
										{{ back.data.status.backupInformation.size.compressed | formatBytes }}
									</td>
								</tr>
								<tr>
									<td class="label">
										PG
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.postgresVersion')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.postgresVersion | prefix }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Name
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.name')"></span>
									</td>
									<td>
										{{ back.data.metadata.name }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Source Cluster
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.sgCluster')"></span>
									</td>
									<td>
										{{ back.data.spec.sgCluster }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Start Time
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.start')"></span>
									</td>
									<td class="timestamp">
										<span class='date'>
											{{ back.data.status.process.timing.start | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ back.data.status.process.timing.start | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ back.data.status.process.timing.start | formatTimestamp('ms') }}
										</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										Elapsed
										<span class="helpTooltip" data-tooltip="Total time transcurred between the start time of backup and the time at which the backup is safely stored in the object storage."></span>
									</td>
									<td class="timestamp">
										<span class='time'>
											{{ back.duration | formatTimestamp('time', false) }}
										</span>
										<span class='ms'>
											{{ back.duration | formatTimestamp('ms', false) }}
										</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										LSN (start ⇢ end)
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.lsn.start') + ' ⇢ ' + getTooltip('sgbackup.status.backupInformation.lsn.end')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.lsn.start }} ⇢ {{ back.data.status.backupInformation.lsn.end }}
									</td>
								</tr>
								<tr>
									<td class="label">
										UID
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.uid')"></span>
									</td>
									<td colspan="2">
										{{ back.data.metadata.uid }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Source Pod
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.hostname')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.hostname }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Timeline
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.timeline')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.timeline }}
									</td>
								</tr>
								<tr>
									<td class="label">
										System Identifier
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.systemIdentifier')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.systemIdentifier }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Job Pod
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.jobPod')"></span>
									</td>
									<td>
										{{ back.data.status.process.jobPod }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Managed Lifecycle (request)
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.managedLifecycle')"></span>
									</td>
									<td class="managedLifecycle" :class="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle.toString() : 'false'" :data-val="hasProp(back,'data.spec.managedLifecycle') ? back.data.spec.managedLifecycle : 'false'"></td>
								</tr>
								<tr>
									<td class="label">
										Managed Lifecycle (status)
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.managedLifecycle')"></span>
									</td>
									<td>
										{{ back.data.status.process.managedLifecycle ? 'Enabled' : 'Disabled' }}
									</td>
								</tr>
								<tr>
									<td class="label">
										End Time
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.end')"></span>
									</td>
									<td class="timestamp">
										<span class='date'>
											{{ back.data.status.process.timing.end | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ back.data.status.process.timing.end | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ back.data.status.process.timing.end | formatTimestamp('ms') }}
										</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										Stored Time
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
									</td>
									<td class="timestamp">
										<span class='date'>
											{{ back.data.status.process.timing.stored | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ back.data.status.process.timing.stored | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ back.data.status.process.timing.stored | formatTimestamp('ms') }}
										</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										Hostname
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.hostname')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.hostname }}
									</td>
								</tr>
								<tr>
									<td class="label">
										PG Data
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.pgData')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.pgData }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Start Wal File
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.startWalFile')"></span>
									</td>
									<td>
										{{ back.data.status.backupInformation.startWalFile }}
									</td>
								</tr>
								<tr v-if="(typeof back.data.status.backupInformation.controlData !== 'undefined')" class="controlData">
									<td class="label">
										Control Data
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.controlData')"></span>
									</td>
									<td>
										<a @click="setContentTooltip('#controlData')"> 
											View Control Data
											<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
										</a>

										<div id="controlData" class="hidden">
											<table class="crdDetails">
												<tr v-for="(value, key) in back.data.status.backupInformation.controlData">
													<td class="label">{{ key }}</td>
													<td class="value">{{ value }}</td>
												</tr>
											</table>
										</div>
									</td>
								</tr>
								<tr v-if="hasProp(back, 'data.status.backupPath')">
									<td class="label">
										Backup Path
										<span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupPath')"></span>
									</td>
									<td>
										{{ back.data.status.backupPath }}
									</td>
								</tr>
							</tbody>
						</table>
					</template>
					<template v-else-if="( hasProp(back, 'data.status.process.status') && (back.data.status.process.status === 'Failed') )">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">
										Name
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.name')"></span>
									</td>
									<td>
										{{ back.name }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Status
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.status')"></span>
									</td>
									<td class="phase" :class="back.data.status.process.status">
										<span>{{ back.data.status.process.status }}</span>
									</td>
								</tr>
								<tr>
									<td class="label">
										Failure Cause
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.failure')"></span>
									</td>
									<td>
										<vue-markdown :source="back.data.status.process.failure"></vue-markdown>
									</td>
								</tr>
							</tbody>
						</table>
					</template>
				</template>
			</template>
		</div>
		
	</div>
</template>

<script>
	import store from '../store'
	import router from '../router'
	import { mixin } from './mixins/mixin'
	import moment from 'moment'

    export default {
        name: 'SGBackups',

		mixins: [mixin],

		data: function() {

			return {
				datePickerLoaded: false,
				currentSort: {
					param: 'data.status.process.timing.stored',
					type: 'timestamp'
				},
				currentSortDir: 'desc',
				filters: {
					clusterName: '',
					keyword: '',
					managedLifecycle: [],
					status: [],
					postgresVersion: [],
					datePicker: '',
					dateStart: '',
					dateEnd: '',
				},
				activeFilters: {
					clusterName: '',
					keyword: '',
					managedLifecycle: [],
					status: [],
					postgresVersion: [],
					datePicker: ''
				}
			}
		},
		computed: {

			backups () {
				const vc = this
				
				store.state.sgbackups.forEach( function(bk, index) {

					let show = true

					if(vc.activeFilters.keyword.length)
						show = JSON.stringify(bk).includes(vc.activeFilters.keyword)
						
					if(vc.activeFilters.managedLifecycle.length && show)
						show = (!bk.data.spec.hasOwnProperty('managedLifecycle') && (vc.activeFilters.managedLifecycle[0] == 'false')) || ( bk.data.spec.hasOwnProperty('managedLifecycle') && (bk.data.spec.managedLifecycle.toString() === vc.activeFilters.managedLifecycle[0]))

					if(vc.activeFilters.status.length && vc.hasProp(bk, 'data.status.process.status')  && show)
						show = vc.activeFilters.status.includes(bk.data.status.process.status)
					else if (vc.activeFilters.status.length && !vc.hasProp(bk, 'data.status.process.status'))
						show = false

					if(vc.activeFilters.postgresVersion.length && vc.hasProp(bk, 'data.status.backupInformation.postgresVersion') && (bk.data.status.process.status === 'Completed') && show )
						show = (bk.data.status.backupInformation.postgresVersion.substr(0,2)=== vc.activeFilters.postgresVersion[0]);
					else if (vc.activeFilters.postgresVersion.length)
						show = false

					if(vc.activeFilters.clusterName.length && show)
						show = (vc.activeFilters.clusterName == bk.data.spec.sgCluster)
					
					if(vc.filters.dateStart.length && vc.filters.dateEnd.length && vc.hasProp(bk, 'data.status.process.status') && (bk.data.status.process.status == 'Completed') && show ) {
						let timestamp = moment(new Date(bk.data.status.process.timing.stored))
						let start = moment(new Date(vc.filters.dateStart))
						let end = moment(new Date(vc.filters.dateEnd))

						show = timestamp.isBetween( start, end, null, '[]' )
					} else if (vc.activeFilters.datePicker.length)
						show = false

					if(bk.show != show) {
						store.commit('showBackup',{
							pos: index,
							isVisible: show
						})
					}

				})

				return vc.sortTable( [...(store.state.sgbackups.filter(back => ( ( (back.data.metadata.namespace == vc.$route.params.namespace) && !vc.isCluster) || (vc.isCluster && (back.data.spec.sgCluster == vc.$route.params.name ) && (back.data.metadata.namespace == vc.$route.params.namespace ) ) ) && back.show ))], vc.currentSort.param, vc.currentSortDir, vc.currentSort.type)
			},

			clusters () {
				return store.state.sgclusters
			},

			isCluster() {
				return this.$route.name.includes('ClusterBackups')
			},

			tooltips() {
				return store.state.tooltips
			},

			isFiltered() {
				return (this.filters.managedLifecycle.length || this.filters.status.length || this.filters.postgresVersion.length || this.filters.clusterName.length)
			}

		},
		
		methods: {

			clearFilters(section) {
				const vc = this
				
				if(section == 'others') {
					vc.filters.clusterName = '';
					vc.activeFilters.clusterName = '';
					
					vc.filters.managedLifecycle = [];
					vc.activeFilters.managedLifecycle = [];
					
					vc.filters.status = [];
					vc.activeFilters.status = [];
					
					vc.filters.postgresVersion = [];
					vc.activeFilters.postgresVersion = [];

					$('.filter.open .active').removeClass('active');

				} else if (section == 'keyword') {
					vc.filters.keyword = '';
					vc.activeFilters.keyword = '';
				}

				router.push(vc.$route.path + vc.getActiveFilters())

			},

			filterBackups(section) {
				const vc = this

				switch(section) {

					case 'keyword':
						vc.activeFilters.keyword = vc.filters.keyword
						break;
					
					case 'datePicker':
						vc.activeFilters.datePicker = vc.filters.datePicker
						break;
					
					case 'others':
						Object.keys(vc.filters).forEach(function(filter) {
							if(!['keyword', 'datePicker'].includes(filter))
								vc.activeFilters[filter] = vc.filters[filter]
						})
						break;

				}

				router.push(vc.$route.path + vc.getActiveFilters())

				vc.pagination.start = 0;
				vc.pagination.current = 0;
				
			},

			getBackupDuration( start, stored ) {
				let begin = moment(start);
				let finish = moment(stored);
				return(new Date(moment.duration(finish.diff(begin))).toISOString());
			},

			getActiveFilters() {
				const vc = this
				let queryString = ''
				
				Object.keys(vc.activeFilters).forEach(function(filter) {
					if(vc.activeFilters[filter].length) {
						if(!queryString.length)
							queryString = '?'
						else
							queryString += '&'
						
						queryString += filter + '=' + vc.activeFilters[filter]
					}
				})

				return queryString
			},

			clearDatePicker() {
				const vc = this;

				vc.filters.datePicker = '';
				vc.filters.dateStart = '';
				vc.filters.dateEnd = '';
				vc.activeFilters.datePicker = '';
				router.push(vc.$route.path + vc.getActiveFilters())

				$('.daterangepicker td.deactivate').removeClass('deactivate')
				$('#datePicker').focus()
				$('.daterangepicker td.active').removeClass('active')
				$('.daterangepicker td.in-range').removeClass('in-range')
				$('#datePicker').parent().removeClass('open');
			},

			initDatePicker() {
				const vc = this;
					
				if(!vc.datePickerLoaded) {
						
					$('#datePicker').daterangepicker({
						"parentEl": "#backups",
						"autoApply": true,
						"timePicker": true,
						"timePicker24Hour": true,
						"timePickerSeconds": true,
						"opens": "left",
						locale: {
							cancelLabel: "Clear"
						}
					}, function(start, end, label) {
						vc.filters.dateStart = start.format('YYYY-MM-DD HH:mm:ss');
						vc.filters.dateEnd = end.format('YYYY-MM-DD HH:mm:ss');
						vc.filters.datePicker = vc.filters.dateStart+' / '+vc.filters.dateEnd;
						vc.filterBackups('datePicker');
					});
				
			
					$('#datePicker').on('show.daterangepicker', function(ev, picker) {

						if(!vc.filters.datePicker.length) {
							$('.daterangepicker td.active').addClass('deactivate')
							$('.daterangepicker td.in-range').removeClass('in-range')
						}

						$('#datePicker').parent().addClass('open');
					});

					$('#datePicker').on('hide.daterangepicker', function(ev, picker) {
						$('#datePicker').parent().removeClass('open');

						if(vc.filters.datePicker.length)
							$('.daterangepicker td.deactivate').removeClass('deactivate')
					});

					$('#datePicker').on('cancel.daterangepicker', function(ev, picker) {
						vc.clearDatePicker();
					});

					$('#datePicker').on('apply.daterangepicker', function(ev, picker) {
						$('#datePicker').focus()
						$('#datePicker').parent().removeClass('open');
					});

					vc.datePickerLoaded = true;
				}
			}
		},

		mounted: function() {

			// Load datepicker
			require('daterangepicker');
			
			const vc = this;

			// Detect if URL contains filters
			if(location.search.length) {
				var urlFilters = JSON.parse('{"' + decodeURI(location.search.substring(1)).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g,'":"').replace(/%3A/g,':').replace(/%2F/g,'/') + '"}')
				
				Object.keys(urlFilters).forEach(function(filter) {

					if(['managedLifecycle','status','postgresVersion'].includes(filter)) { // Array filters
						vc.filters[filter] = urlFilters[filter].split(',')
						vc.activeFilters[filter] = urlFilters[filter].split(',')
					} else if (filter != 'datePicker') {
						vc.filters[filter] = urlFilters[filter]
						vc.activeFilters[filter] = urlFilters[filter]
					}

					if (filter == 'datePicker') {
						vc.filters.dateStart = urlFilters[filter].split('/')[0]
						vc.filters.dateEnd = urlFilters[filter].split('/')[1]
					}
					
				})
			}

			$(document).ready(function(){

				$(document).on('change', '.filter select', function () {
					if($(this).val().length)
						$(this).addClass('active')
					else
						$(this).removeClass('active')
				});

				$(document).on('click', 'input[type="checkbox"]', function () {

					if($(this).is(':checked')) {
						$(this).addClass('active');

						if($(this).hasClass("xCheckbox")) {
							$(this).parents('li').find(':checked:not(#'+$(this).prop('id')+')').removeClass('active').prop('checked', false);
						
							if(vc.filters[$(this).data('filter')].length)
								vc.filters[$(this).data('filter')] = [$(this).val()];
						}
						
					} else {
						$(this).removeClass('active');
						
						if($(this).hasClass("xCheckbox"))
							vc[$(this).data('filter')] = [];
					}					
				});	

			});

		},

		beforeDestroy: function() {
            $('.daterangepicker').remove()
        }

	}
</script>

<style scoped>
	.toolbar .btn.border {
		font-size: .8rem;
	}

	#backups .options {
		width: 370px;
	}

	th.actions, td.actions {
        width: 95px !important;
        min-width: 95px;
        max-width: 95px;
    }

	table.resizable th[data-type="phase"] {
		max-width: 105px;
	}

	table.resizable th[data-type="lifecycle"] {
		max-width: 240px;
	}
</style>

<style>
	#backups .daterangepicker {
		top: 220px !important;
		left: 390px !important;
		right: auto !important;
	}

	.cluster #backups .daterangepicker {
		top: 275px !important;
	}
</style>