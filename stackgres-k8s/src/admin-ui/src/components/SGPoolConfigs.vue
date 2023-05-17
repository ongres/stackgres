<template>
	<div id="pool-config" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="connectionpooling" class="configurations poolConfig resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted desc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.metadata.name')"></span>
						</th>
						<th class="config notSortable hasTooltip">
							<span title="Parameters">Parameters</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.spec.pgBouncer.pgbouncer.ini')"></span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!config.length">
							<tr class="no-results">
								<td colspan="3" v-if="iCan('create','sgpoolconfigs',$route.params.namespace)">
									No configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgpoolconfigs/new'" title="Add New Connection Pooling Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="3">
									No configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
						<template v-for="(conf, index) in config">
							<template  v-if="(index >= pagination.start) && (index < pagination.end)">
								<tr class="base">
									<td class="hasTooltip">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + conf.name" class="noColor">
												{{ conf.name }}
											</router-link>
										</span>
									</td>
									<td class="parameters hasTooltip">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + conf.name" class="noColor">
												<template v-for="param in conf.data.status.pgBouncer['pgbouncer.ini']">
													<strong>{{ param.parameter }}:</strong> {{ param.value }}; 
												</template>
											</router-link>
										</span>
									</td>
									<td class="actions">
										<router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + conf.name" target="_blank" class="newTab"></router-link>
										<router-link v-if="iCan('patch','sgpoolconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgpoolconfig/' + conf.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
										<a v-if="iCan('create','sgpoolconfigs',$route.params.namespace)" @click="cloneCRD('SGPoolingConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration"></a>
										<a v-if="iCan('delete','sgpoolconfigs',$route.params.namespace)" @click="deleteCRD('sgpoolconfigs',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration" :class="conf.data.status.clusters.length ? 'disabled' : ''"></a>
									</td>
								</tr>
							</template>
						</template>
					</tbody>
				</table>
				<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < config.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="config.length" @page-change="pageChange" align="center" ref="page"></v-page>
				<div id="nameTooltip">
					<div class="info"></div>
				</div>
			</template>
			<template v-else>
				<h2>Configuration Details</h2>
				<template v-for="conf in config" v-if="conf.name == $route.params.name">
					<div class="configurationDetails">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.metadata.name')"></span></td>
									<td>{{ conf.name }}</td>
								</tr>
								<tr v-if="conf.data.status.clusters.length">
									<td class="label">Used on  <span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.status.clusters')"></span></td>
									<td class="usedOn">
										<ul>
											<li v-for="cluster in conf.data.status.clusters">
												<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
													{{ cluster }}
													<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
												</router-link>
											</li>
										</ul>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
					<div class="paramDetails" v-if="conf.data.spec.pgBouncer['pgbouncer.ini'].length">
						<template v-if="conf.data.status.pgBouncer['pgbouncer.ini'].length != Object.keys(conf.data.status.pgBouncer.defaultParameters).length">
							<h2>
								Parameters
								<span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.spec.pgBouncer.pgbouncer.ini')"></span>
							</h2>
							<table>
								<tbody>
									<tr v-for="param in conf.data.status.pgBouncer['pgbouncer.ini']" v-if="(!conf.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) || (conf.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) && (conf.data.status.pgBouncer.defaultParameters[param.parameter] != param.value)) )">
										<td class="label">
											{{ param.parameter }}
										</td>
										<td class="paramValue">
											{{ param.value }}
										</td>
									</tr>
								</tbody>
							</table>
						</template>

						<template v-if="Object.keys(conf.data.status.pgBouncer.defaultParameters).length">
							<h2>
								Default Parameters
								<span class="helpTooltip" :data-tooltip="getTooltip('sgpoolingconfig.status.pgBouncer.defaultParameters')"></span>
							</h2>
							<table class="defaultParams">
								<tbody>
									<tr v-for="param in conf.data.status.pgBouncer['pgbouncer.ini']" v-if="( conf.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter) && (conf.data.status.pgBouncer.defaultParameters[param.parameter] == param.value))">
										<td class="label">
											{{ param.parameter }}
										</td>
										<td class="paramValue">
											{{ param.value }}
										</td>
									</tr>
								</tbody>
							</table>
						</template>
					</div>
				</template>
			</template>
		</div>
	</div>
</template>

<script>
	import { mixin } from './mixins/mixin'
	import store from '../store'

    export default {
        name: 'SGPoolConfigs',

		mixins: [mixin],


		data: function() {
			return {
				currentSort: {
					param: 'data.metadata.name',
					type: 'alphabetical'
				},
				currentSortDir: 'desc',
			}
		},
		computed: {

			config () {
				return this.sortTable( [...(store.state.sgpoolconfigs.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			tooltips() {
				return store.state.tooltips
			}

		},
	}

</script>