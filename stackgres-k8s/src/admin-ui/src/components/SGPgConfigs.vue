<template>
	<div id="pg-config" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="postgres" class="configurations pgConfig resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted desc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.metadata.name')"></span>
						</th>
						<th class="desc postgresVersion hasTooltip" data-type="version">
							<span @click="sort('data.spec.postgresVersion')" title="Postgres Version">
								PG
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresVersion')"></span>
						</th>
						<th class="config notSortable hasTooltip">
							<span title="Parameters">
								Parameters
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresql.conf')"></span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!config.length">
							<tr class="no-results">
								<td colspan="4" v-if="iCan('create','sgpgconfigs',$route.params.namespace)">
									No configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgpgconfigs/new'" title="Add New Postgres Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="4">
									No configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
						<template v-for="(conf, index) in config">
							<template v-if="(index >= pagination.start) && (index < pagination.end)">
								<tr class="base">
									<td class="hasTooltip configName">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + conf.name" class="noColor">
												{{ conf.name }}
											</router-link>
										</span>
									</td>
									<td class="pgVersion">
										<router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + conf.name" class="noColor">
											{{ conf.data.spec.postgresVersion }}
										</router-link>
									</td>
									<td class="parameters hasTooltip">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + conf.name" class="noColor">
												<template v-for="param in conf.data.status['postgresql.conf']">
													<strong>{{ param.parameter }}:</strong> {{ param.value }}; 
												</template>
											</router-link>
										</span>
									</td>
									<td class="actions">
										<router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + conf.name" target="_blank" class="newTab"></router-link>
										<router-link v-if="iCan('patch','sgpgconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgpgconfig/' + conf.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
										<a v-if="iCan('create','sgpgconfigs',$route.params.namespace)" @click="cloneCRD('SGPostgresConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration"></a>
										<a v-if="iCan('delete','sgpgconfigs',$route.params.namespace)" @click="deleteCRD('sgpgconfigs',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration" :class="conf.data.status.clusters.length ? 'disabled' : ''"></a>
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
									<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.metadata.name')"></span></td>
									<td>{{ conf.name }}</td>
								</tr>
								<tr>
									<td class="label">Postgres Version <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresVersion')"></span></td>
									<td>{{ conf.data.spec.postgresVersion }}</td>
								</tr>
								<tr v-if="conf.data.status.clusters.length">
									<td class="label">Used on  <span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.status.clusters')"></span></td>
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
					<div class="paramDetails" v-if="conf.data.status['postgresql.conf'].length">
						<template v-if="hasParamsSet(conf)">
							<h2>
								Parameters
								<span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.spec.postgresql.conf')"></span>
							</h2>
							<table>
								<tbody>
									<tr v-for="param in conf.data.status['postgresql.conf']" v-if="(!conf.data.status.defaultParameters.hasOwnProperty(param.parameter) || (conf.data.status.defaultParameters.hasOwnProperty(param.parameter) && (conf.data.status.defaultParameters[param.parameter] != param.value)) )">
										<td class="label">
											{{ param.parameter }}
											<a v-if="(typeof param.documentationLink !== 'undefined')" :href="param.documentationLink" target="_blank" :title="'Read documentation about ' + param.parameter" class="paramDoc">
												<!--<svg xmlns="http://www.w3.org/2000/svg" width="14.999" height="14.999" viewBox="0 0 14.999 14.999"><g transform="translate(4.772 3.02)"><path d="M10.271,6.274A1.006,1.006,0,0,1,9.162,5.266a1.236,1.236,0,0,1,1.263-1.2,1,1,0,0,1,1.12,1.006A1.227,1.227,0,0,1,10.271,6.274Z" transform="translate(-7.191 -4.062)" fill="#d3d3d6"/><path d="M9.635,13.986a2.8,2.8,0,0,1-.624-.067,1.807,1.807,0,0,1-.784-.382,1.548,1.548,0,0,1-.45-.681,2,2,0,0,1-.1-.634,3.539,3.539,0,0,1,.077-.636l.016-.081c.076-.307.382-1.486.382-1.486A.573.573,0,0,0,8.178,9.6a.4.4,0,0,0-.365-.223H6.837A.423.423,0,0,1,6.7,9.344a.261.261,0,0,1-.1-.06.252.252,0,0,1-.059-.094,2.271,2.271,0,0,1,.123-.857l.02-.088a.753.753,0,0,1,.046-.163.277.277,0,0,1,.214-.16h3.083a.319.319,0,0,1,.256.127.288.288,0,0,1,.053.252l-.784,3.351a1.41,1.41,0,0,0-.043.265c0,.361.188.538.576.538a1.469,1.469,0,0,0,.467-.1l.131-.043a.9.9,0,0,1,.166-.021c.145.019.2.052.23.1.051.091.232.726.271.877a.639.639,0,0,1,.028.18.312.312,0,0,1-.185.23,3.627,3.627,0,0,1-.356.106,6.275,6.275,0,0,1-.624.145A3.656,3.656,0,0,1,9.635,13.986Z" transform="translate(-6.534 -5.027)" fill="#d3d3d6"/></g><path d="M7.67.035a7.5,7.5,0,1,0,7.5,7.5A7.5,7.5,0,0,0,7.67.035Zm0,13.511a6.012,6.012,0,1,1,6.012-6.012A6.019,6.019,0,0,1,7.67,13.546Z" transform="translate(-0.17 -0.035)" fill="#d3d3d6"/></svg>-->
											</a>
										</td>
										<td class="paramValue">
											{{ param.value }}
										</td>
									</tr>
								</tbody>
							</table>
						</template>

						<template v-if="Object.keys(conf.data.status.defaultParameters).length">
							<h2>
								Default Parameters
								<span class="helpTooltip" :data-tooltip="getTooltip('sgpostgresconfig.status.defaultParameters')"></span>
							</h2>
							<table class="defaultParams">
								<tbody>
									<tr v-for="param in conf.data.status['postgresql.conf']" v-if="( conf.data.status.defaultParameters.hasOwnProperty(param.parameter) && (conf.data.status.defaultParameters[param.parameter] == param.value))">
										<td class="label">
											{{ param.parameter }}
											<a v-if="(typeof param.documentationLink !== 'undefined')" :href="param.documentationLink" target="_blank" :title="'Read documentation about ' + param.parameter" class="paramDoc"></a>
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
        name: 'SGPgConfigs',

		mixins: [mixin],

		data: function() {

			return {
				currentSort: {
					param: 'data.metadata.name',
					type: 'alphabetical'
				},
				currentSortDir: 'asc',
			}
		},
		computed: {

			config () {
				return this.sortTable( [...(store.state.sgpgconfigs.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			tooltips() {
				return store.state.tooltips
			}

		},

		methods: {

			hasParamsSet(conf) {
				let setParam = conf.data.status['postgresql.conf'].find(p => ( (conf.data.status.defaultParameters[p.parameter] != p.value) ))
				return (typeof setParam != 'undefined')
			}

		}

	}
</script>