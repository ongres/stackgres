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
				<template v-for="conf in config" v-if="conf.name == $route.params.name">
					<h2>Configuration Details</h2>
					
					<div class="configurationDetails">
						<CRDSummary :crd="crd" kind="SGPoolingConfig" :details="true"></CRDSummary>
					</div>
				</template>
			</template>
		</div>
	</div>
</template>

<script>
	import { mixin } from './mixins/mixin'
	import store from '../store'
	import CRDSummary from './forms/summary/CRDSummary.vue'

    export default {
        name: 'SGPoolConfigs',

		mixins: [mixin],

		components: {
            CRDSummary
        },

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
			},

			crd () {
				return store.state.sgpoolconfigs.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.name))
			}
		},
	}

</script>