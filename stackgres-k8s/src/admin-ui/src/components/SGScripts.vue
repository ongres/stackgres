<template>
	<div id="script-config" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="script" class="configurations script resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted asc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
						</th>
                        <th class="sorted asc hasTooltip">
							<span @click="sort('data.spec.managedVersions')" title="Managed Versions">
                                Managed Versions
                            </span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions')"></span>
						</th>
                        <th class="sorted asc hasTooltip">
							<span @click="sort('data.spec.continueOnError')" title="Continue on Error">
                                Continue on Error
                            </span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError')"></span>
						</th>
						<th class="hasTooltip notSortable textRight">
							<span title="Script Count">
                                Script Count
                            </span>
							<span class="helpTooltip" data-tooltip="The total count of entries associated to this script"></span>
						</th>
                        <th class="hasTooltip notSortable">
							<span title="Default Script">
                                Default Script
                            </span>
							<span class="helpTooltip" data-tooltip="Indicates if it's a default script which has been created by StackGres"></span>
						</th>
                        <th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!scripts.length">
							<tr class="no-results">
								<td colspan="999" v-if="iCan('create','sgscripts',$route.params.namespace)">
									No script configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgscripts/new'" title="Add New Script Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="3">
									No script configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
						<template v-for="(baseScript, index) in scripts">
							<template  v-if="(index >= pagination.start) && (index < pagination.end)">
								<tr class="base">
									<td class="hasTooltip">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												{{ baseScript.name }}
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="hasProp(baseScript,'data.spec.managedVersions') ? baseScript.data.spec.managedVersions.toString() : 'true'" :data-val="hasProp(baseScript, 'data.spec.managedVersions') ? baseScript.data.spec.managedVersions : 'true'">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												<span></span>
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="hasProp(baseScript,'data.spec.continueOnError') ? baseScript.data.spec.continueOnError.toString() : 'false'" :data-val="hasProp(baseScript, 'data.spec.continueOnError') ? baseScript.data.spec.continueOnError : 'false'">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												<span></span>
											</router-link>
										</span>
									</td>
                                    <td class="hasTooltip textRight">
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
												{{ baseScript.data.spec.scripts.length }}
											</router-link>
										</span>
									</td>
                                    <td class="center icon" :class="isDefaultScript(baseScript) ? 'true' : 'false'" :data-val="isDefaultScript(baseScript)">
                                        <router-link :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name" class="noColor">
                                            <span></span>
                                        </router-link>
                                    </td>
									<td class="actions">
										<router-link v-if="iCan('patch','sgscripts',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgscript/' + baseScript.name + '/edit'" title="Edit Script" class="editCRD" :class="isDefaultScript(baseScript) && 'disabled'"></router-link>
										<a v-if="iCan('create','sgscripts',$route.params.namespace)" @click="cloneCRD('SGScripts', $route.params.namespace, baseScript.name)" class="cloneCRD" title="Clone Script"></a>
										<a v-if="iCan('delete','sgscripts',$route.params.namespace)" @click="deleteCRD('sgscripts',$route.params.namespace, baseScript.name)" class="delete deleteCRD" title="Delete Script" :class="isDefaultScript(baseScript) && 'disabled'"></a>
									</td>
								</tr>
							</template>
						</template>
					</tbody>
				</table>
				<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < scripts.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="scripts .length" @page-change="pageChange" align="center" ref="page"></v-page>
				<div id="nameTooltip">
					<div class="info"></div>
				</div>
			</template>

			<template v-else>
				<template v-for="baseScript in scripts" v-if="baseScript.name == $route.params.name">
					<h2>Script Details</h2>
					
					<div class="configurationDetails">
                         <CRDSummary :crd="crd" kind="SGScript" :details="true"></CRDSummary>
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
        name: 'SGScripts',

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
				currentSortDir: 'asc',
            }
        },

        computed: {

			scripts () {
				return this.sortTable( [...(store.state.sgscripts.filter(s => (s.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			crd () {
				return store.state.sgscripts.find(s => (s.data.metadata.namespace == this.$route.params.namespace) && (s.data.metadata.name == this.$route.params.name))
			}

		},

        methods: {

            isDefaultScript(script) {
                return ( script.data.status.clusters.length && (script.name == (script.data.status.clusters[0] + '-default') ) )
            }

        }
	}

</script>