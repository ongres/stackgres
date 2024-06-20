<template>
    <div id="logs-cluster" v-if="iCanLoad">
        <div class="content">
            <template v-if="!$route.params.hasOwnProperty('name')">
                <table id="logs" class="logsCluster pgConfig resizable fullWidth" v-columns-resizable>
                    <thead class="sort">
                        <th class="sorted desc name hasTooltip">
                            <span @click="sort('data.metadata.name')" title="Name">
                                Name
                            </span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.name')"></span>
                        </th>
                        <th class="desc volumeSize hasTooltip textRight">
                            <span @click="sort('data.spec.persistentVolume.size', 'memory')" title="Disk">
                                Disk
                            </span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                        </th>
                        <th class="hasTooltip textRight notSortable">
                            <span>CPU</span>
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span>
                        </th>
                        <th class="hasTooltip textRight notSortable">
                            <span>Memory</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span>
                        </th>
                        <th class="actions"></th>
                    </thead>
                    <tbody>
                        <template v-if="clusters === null">
							<tr class="no-results">
								<td colspan="999">
									Loading data...
								</td>
							</tr>
						</template>
						<template v-else-if="!clusters.length">
							<tr class="no-results">
								<td :colspan="5" v-if="iCan('create','sgdistributedlogs',$route.params.namespace)">
                                    No logs servers have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgdistributedlogs/new'" title="Add New Logs Server">create a new one?</router-link>
                                </td>
                                <td v-else colspan="5">
                                    No logs servers have been found. You don't have enough permissions to create a new one
                                </td>
							</tr>
						</template>
                        <template v-else>
                            <template v-for="(cluster, index) in clusters">
                                <template  v-if="(index >= pagination.start) && (index < pagination.end)">
                                    <tr class="base">
                                        <td class="hasTooltip clusterName">
                                            <span>
                                                <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                                    {{ cluster.name }}
                                                </router-link>
                                            </span>
                                        </td>
                                        <td class="volumeSize fontZero textRight">
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                                {{ cluster.data.spec.persistentVolume.size }}
                                            </router-link>
                                        </td>
                                        <td class="cpu textRight">
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                                {{ getProfileSpec(cluster.data.spec.sgInstanceProfile, 'cpu') }}
                                            </router-link>
                                        </td>
                                        <td class="ram textRight">
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                                {{ getProfileSpec(cluster.data.spec.sgInstanceProfile, 'memory') }}
                                            </router-link>
                                        </td>
                                        <td class="actions">
                                            <router-link v-if="iCan('patch','sgdistributedlogs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.data.metadata.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
                                            <a v-if="iCan('create','sgdistributedlogs',$route.params.namespace)" @click="cloneCRD('SGDistributedLogs', $route.params.namespace, cluster.data.metadata.name)" class="cloneCRD" title="Clone Logs Server"></a>
                                            <a v-if="iCan('delete','sgdistributedlogs',$route.params.namespace)" @click="deleteCRD('sgdistributedlogs',$route.params.namespace, cluster.data.metadata.name)" class="delete deleteCRD" title="Delete Configuration" :class="cluster.data.status.clusters.length ? 'disabled' : ''"></a>
                                        </td>
                                    </tr>
                                </template>
                            </template>
                        </template>
                    </tbody>
                </table>
                <v-page :key="'pagination-'+pagination.rows" v-if="( (clusters !== null) && (pagination.rows < clusters.length) )" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="clusters.length" @page-change="pageChange" align="center" ref="page"></v-page>
                <div id="nameTooltip">
                    <div class="info"></div>
                </div>
            </template>
            <template v-else>
                <template v-if="clusters === null">
					<div class="warningText">
						Loading data...
					</div>
				</template>
				<template v-else>
                    <h2>Logs Server Details</h2>
                    
                    <div class="configurationDetails">                      
                        <CRDSummary :crd="crd" kind="SGDistributedLogs" :details="true"></CRDSummary>
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
        name: 'SGDistributedLogs',

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
        methods: {
            
            unparseProps ( props, key = 'annotation' ) {
                var propsArray = [];
                if(!$.isEmptyObject(props)) {
                    Object.entries(props).forEach(([k, v]) => {
                        var prop = {};
                        prop[key] = k;
                        prop['value'] = v;
                        propsArray.push(prop)
                    });
                }
                
                return propsArray
            },

            getProfileSpec(profile, spec) {
                if(store.state.sginstanceprofiles !== null) {
                    return store.state.sginstanceprofiles.find(p => (p.data.metadata.namespace == this.$route.params.namespace) && (p.data.metadata.name == profile)).data.spec[spec]
                } else {
                    return {}
                }
            }

        },
    
        computed: {

            clusters () {
                return (
					(store.state.sgdistributedlogs !== null)
						? this.sortTable([...(store.state.sgdistributedlogs.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
						: null
				)
            },
            
            tooltips() {
                return store.state.tooltips
            },
            
            crd () {
                return (
					(store.state.sgdistributedlogs !== null)
						? store.state.sgdistributedlogs.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.name))
						: null
				)
			}

        }
    }
</script>

<style scoped>
    td.tolerationSeconds {
		text-align: right;
	}
</style>