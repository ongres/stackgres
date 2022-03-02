<template>
	<div id="pool-config" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
					<template v-if="$route.params.hasOwnProperty('name')">
						<router-link :to="'/' + $route.params.namespace + '/sgpoolconfigs'" title="SGPoolingConfigList">SGPoolingConfigList</router-link>
					</template>
					<template v-else>
						SGPoolingConfigList
					</template>
				</li>
				<li v-if="(typeof $route.params.name !== 'undefined')">
					{{ $route.params.name }}
				</li>
			</ul>

			<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgpoolingconfig/" target="_blank" title="SGPoolingConfig Documentation">SGPoolingConfig Documentation</a>
				<div class="crdActionLinks">
					<template v-if="$route.params.hasOwnProperty('name')">
						<template v-for="conf in config" v-if="conf.name == $route.params.name">
							<router-link v-if="iCan('patch','sgpoolconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgpoolconfig/' + conf.name + '/edit'" title="Edit Configuration">
								Edit Configuration
							</router-link>
							<a v-if="iCan('create','sgpoolconfigs',$route.params.namespace)" @click="cloneCRD('SGPoolingConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration">
								Clone Configuration
							</a>
							<a v-if="iCan('delete','sgpoolconfigs',$route.params.namespace)" @click="deleteCRD('sgpoolconfigs',$route.params.namespace, conf.name, '/' + $route.params.namespace + '/sgpoolconfigs')" title="Delete Configuration" :class="conf.data.status.clusters.length ? 'disabled' : ''">
								Delete Configuration
							</a>
							<router-link :to="'/' + $route.params.namespace + '/sgpoolconfigs'" title="Close Details">Close Details</router-link>
						</template>
					</template>
					<template v-else>
						<router-link v-if="iCan('create','sgpoolconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgpoolconfigs/new'" class="add">Add New</router-link>
					</template>
				</div>
			</div>
		</header>

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
									<tr v-for="param in conf.data.status.pgBouncer['pgbouncer.ini']" v-if="!conf.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter)">
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
									<tr v-for="param in conf.data.status.pgBouncer['pgbouncer.ini']" v-if="conf.data.status.pgBouncer.defaultParameters.hasOwnProperty(param.parameter)">
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
        name: 'PoolConfig',

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
				return this.sortTable( [...(store.state.poolConfig.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			tooltips() {
				return store.state.tooltips
			}

		},
	}

</script>