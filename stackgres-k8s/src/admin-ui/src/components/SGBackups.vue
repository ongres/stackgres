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

						<router-link
							v-if="isCluster && iCan('create','sgbackups',$route.params.namespace)"
							:to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/sgbackups/new'"
							title="Add New Backup"
							class="add addClusterBackup"
						>
							Create Backup
						</router-link>

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
									<template v-for="version in postgresVersionsList">
										<label :for="'pg' + version" :key="'pgVersion-' + version">
											<input v-model="filters.postgresVersion" data-filter="postgresVersion" type="checkbox" class="xCheckbox" :id="'pg' + version" :name="'pg' + version" :value="version"/>
											<span>{{ version }}</span>
										</label>
									</template>
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
							<th class="notSortable type">
								<span title="Backup Type">Type</span>
								<span class="helpTooltip" data-tooltip='Indicates if the backup is either a "Base Backup" or a "Snapshot"'></span>
							</th>							
							<th class="desc size hasTooltip textRight">
								<span @click="sort('data.status.backupInformation.size.uncompressed', 'memory')" title="Size (compressed)">Size</span>
								<span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.size.compressed')"></span>
							</th>
							<th class="desc postgresVersion hasTooltip" v-if="!isCluster" data-type="version">
								<span @click="sort('data.status.backupInformation.postgresVersion')" title="Postgres Version">PG</span>
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
							<template v-if="backups === null">
								<tr class="no-results">
									<td colspan="999">
										Loading data...
									</td>
								</tr>
							</template>
							<template v-else-if="!backups.length">
								<tr class="no-results">
									<td :colspan="999" v-if="iCan('create','sgbackups',$route.params.namespace)">
										No backups matched your search terms, would  you like to <router-link :to="'/' + $route.params.namespace + '/sgbackups/new'" title="Add New Backup">create a new one?</router-link>
									</td>
									<td v-else colspan="999">
										No backups have been found. You don't have enough permissions to create a new one
									</td>
								</tr>
							</template>
							<template v-else>
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
												<td class="type">
													<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
														{{ hasProp(back, 'data.status.volumeSnapshot') ? 'Snapshot' : 'Base Backup' }}
													</router-link>
												</td>
												<td class="size hasTooltip textRight" :data-val="(back.data.status.process.status == 'Completed') ? back.data.status.backupInformation.size.uncompressed : ''">
													<router-link :to="'/' + $route.params.namespace + (isCluster ? '/sgcluster/' + $route.params.name : '') + '/sgbackup/' + back.data.metadata.name" class="noColor">
														<span>
															<template v-if="back.data.status.process.status === 'Completed'">
																{{ back.data.status.backupInformation.size.compressed | formatBytes }}
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
													<template v-if="(back.data.status.process.status === 'Completed') && iCan('create','sgclusters',$route.params.namespace)">
														<router-link :to="'/' + $route.params.namespace + '/sgclusters/new?restoreFromBackup=' + back.name + '&postgresVersion=' + back.data.status.backupInformation.postgresVersion.substring(0, 2)" title="Restore Backup" class="restoreBackup"></router-link>
													</template>
													<template v-else>
														<a class="restoreBackup disabled"></a>
													</template>
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
												<td class="type"></td>
												<td class="size"></td>
												<td class="postgresVersion" v-if="!isCluster"></td>
												<td class="name hasTooltip">
													<span>{{ back.name }}</span>
												</td>
												<td class="clusterName hasTooltip stackgres" v-if="!isCluster">
													<span>{{ back.data.spec.sgCluster }}</span>
												</td>
												<td class="actions">
													<a class="restoreBackup disabled"></a>
													<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)" :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackup/' + back.data.metadata.name + '/edit'" title="Edit Backup" class="editCRD"></router-link>
													<a v-if="iCan('delete','sgbackups',$route.params.namespace)" @click="deleteCRD('sgbackups',$route.params.namespace, back.data.metadata.name)" class="delete deleteCRD" title="Delete Backup"></a>
												</td>
											</tr>
										</template>
									</template>
								</template>
							</template>
						</tbody>
					</table>
					<v-page :key="'pagination-'+pagination.rows" v-if="( (backups !== null) && (pagination.rows < backups.length) )" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="backups.length" @page-change="pageChange" align="center" ref="page"></v-page>
					<div id="nameTooltip">
						<div class="info"></div>
					</div>
				</div>
			</template>
			
			<template v-else>
				<template v-if="backups === null">
					<div class="warningText">
						Loading data...
					</div>
				</template>
				<template v-else>
					<h2>Backup Details</h2>

					<template v-if="isCluster">
						<div class="titleLinks crdActionLinks">
							<router-link v-if="iCan('patch','sgbackups',$route.params.namespace)"  :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackup/' + crd.data.metadata.name + '/edit'" title="Edit Backup">
								Edit Backup
							</router-link>
							<a v-if="iCan('delete','sgbackups',$route.params.namespace)" @click="deleteCRD('sgbackups',$route.params.namespace, $route.params.backupname, '/' + $route.params.namespace + '/sgbackups')" title="Delete Backup" class="deleteCRD">
								Delete Backup
							</a>
							<router-link :to="'/' + $route.params.namespace + (isCluster ? ( '/sgcluster/' + $route.params.name ) : '' ) + '/sgbackups'" title="Close Details">Close Details</router-link>
						</div>
					</template>

					<CRDSummary :crd="crd" kind="SGBackup" :details="true"></CRDSummary>
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
	import CRDSummary from './forms/summary/CRDSummary.vue'

    export default {
        name: 'SGBackups',

		mixins: [mixin],
		
		components: {
            CRDSummary
        },

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

				if(store.state.sgbackups === null) {
					return null
				} else {
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
				}
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
			},

			crd () {
                return (
					(store.state.sgbackups !== null)
						? store.state.sgbackups.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.backupname))
						: null
				)
			},

			postgresVersionsList() {
				return Object.keys(store.state.postgresVersions.vanilla)
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