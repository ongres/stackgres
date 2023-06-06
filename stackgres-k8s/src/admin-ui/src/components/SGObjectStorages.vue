<template>
	<div id="object-storage" v-if="loggedIn && isReady && !notFound">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="objectStorage" class="configurations resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted desc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.metadata.name')"></span>
						</th>
						<th class="desc type hasTooltip" data-type="retention">
							<span @click="sort('data.spec.type.type')" title="Type">Type</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type.type')"></span>
						</th>
						<th class="bucket hasTooltip">
							<span title="Bucket">Bucket</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type.bucket')"></span>
						</th>
                        <th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!config.length">
							<tr class="no-results">
								<td colspan="5" v-if="iCan('create','sgobjectstorages',$route.params.namespace)">
									No configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgobjectstorages/new'" title="Add New Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="5">
									No configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>	
						<template v-for="(conf, index) in config">
							<template  v-if="(index >= pagination.start) && (index < pagination.end)">
									<tr class="base">
										<td class="hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name" class="noColor">
													{{ conf.name }}
												</router-link>
											</span>
										</td>
										<td class="hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name" class="noColor">
													{{ conf.data.spec.type }}
												</router-link>
											</span>
										</td>
										<td class="hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name" class="noColor">
													{{ conf.data.spec[conf.data.spec.type].bucket }}
												</router-link>
											</span>
										</td>
										<td class="actions">
											<router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name" target="_blank" class="newTab"></router-link>
											<router-link v-if="iCan('patch','sgobjectstorages',$route.params.namespace)"  :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
											<a v-if="iCan('create','sgobjectstorages',$route.params.namespace)" @click="cloneCRD('SGObjectStorages', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration"></a>
											<a v-if="iCan('delete','sgobjectstorages',$route.params.namespace)" @click="deleteCRD('sgobjectstorages',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration"  :class="conf.data.status.clusters.length ? 'disabled' : ''"></a>
										</td>
									</tr>
								</router-link>
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
					<h2>Object Storage Details</h2>
					
					<div class="configurationDetails">
						<CRDSummary :crd="crd" kind="SGObjectStorage" :details="true"></CRDSummary>
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
        name: 'SGObjectStorages',

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
				return this.sortTable( [...(store.state.sgobjectstorages.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			crd () {
				return store.state.sgobjectstorages.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.name))
			}

		},
		
	}

</script>