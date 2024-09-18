<template>
	<div id="sgstreams-overview" v-if="iCanLoad">
        <div class="content">
            <template v-if="!$route.params.hasOwnProperty('name')">
                <table class="clusterOverview resizable fullWidth" v-if="iCan('list','sgstreams',$route.params.namespace)" v-columns-resizable>
                    <thead class="sort">
                        <th class="sorted asc name hasTooltip">
                            <span @click="sort('data.metadata.name')" title="StackGres Stream">StackGres Stream</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.metadata.name')"></span>
                        </th>

                        <th class="asc sourceType hasTooltip">
                            <span @click="sort('data.spec.source.type')" title="Source Type">Source Type</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.type')"></span>
                        </th>

                        <th class="notSortable source hasTooltip">
                            <span title="Source">Source</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source')"></span>
                        </th>

                        <th class="asc targetType hasTooltip">
                            <span @click="sort('data.spec.target.type')" title="Target Type">Target Type</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.type')"></span>
                        </th>

                        <th class="notSortable source hasTooltip">
                            <span title="Target">Target</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target')"></span>
                        </th>

                        <th class="asc disk hasTooltip textRight">
                            <span @click="sort('data.spec.pods.persistentVolume.size', 'memory')" title="Disk">Disk</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume.size')"></span>
                        </th>

                        <th class="actions"></th>
                    </thead>
                    <tbody>
                        <template v-if="sgStreams === null">
                            <tr class="no-results">
                                <td colspan="999">
                                    Loading data...
                                </td>
                            </tr>
                        </template>
                        <template v-else-if="!sgStreams.length">
                            <tr class="no-results">
                                <td colspan="7" v-if="iCan('create','sgstreams',$route.params.namespace)">
                                    No streams have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgstreams/new'" title="Add New Stream">create a new one?</router-link>
                                </td>
                                <td v-else colspan="7">
                                    No streams have been found. You don't have enough permissions to create a new one
                                </td>
                            </tr>
                        </template>		
                        <template v-else>
                            <template v-for="(stream, index) in sgStreams">
                                <template v-if="(index >= pagination.start) && (index < pagination.end)">
                                    <tr class="base">
                                        <td class="streamName hasTooltip">											
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ stream.name }}
                                            </router-link>
                                        </td>
                                        <td class="sourceType hasTooltip">
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ stream.data.spec.source.type }}
                                            </router-link>
                                        </td>
                                        <td class="source hasTooltip">
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ (stream.data.spec.source.type == 'SGCluster') ? stream.data.spec.source.sgCluster.name : stream.data.spec.source.postgres.host }}
                                            </router-link>
                                        </td>
                                        <td class="targetType hasTooltip">
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ stream.data.spec.target.type }}
                                            </router-link>
                                        </td>
                                        <td class="target hasTooltip">
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ 
                                                    (stream.data.spec.target.type === 'SGCluster')
                                                        ? stream.data.spec.target.sgCluster.name
                                                        : (stream.data.spec.target.type === 'CloudEvent')
                                                            ? stream.data.spec.target.cloudEvent.http.url
                                                            : stream.data.spec.target.pgLambda.knative.http.url
                                                }}
                                            </router-link>
                                        </td>
                                        <td class="volumeSize textRight">
                                            <router-link :to="'/' + $route.params.namespace + '/sgstream/' + stream.name" title="Stream Config" class="noColor">
                                                {{ stream.data.spec.pods.persistentVolume.size }}
                                            </router-link>
                                        </td>
                                        <td class="actions">
                                            <router-link v-if="iCan('patch','sgstreams',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgstream/' + stream.name + '/edit'" title="Edit stream" class="editCRD"></router-link>
                                            <a v-if="iCan('create','sgstreams',$route.params.namespace)" @click="cloneCRD('sgstreams', $route.params.namespace, stream.name)" class="cloneCRD" title="Clone stream"></a>
                                            <a v-if="iCan('delete','sgstreams',$route.params.namespace)" @click="deleteCRD('sgstreams', $route.params.namespace, stream.name)" title="Delete stream" class="deleteCRD"></a>
                                        </td>
                                    </tr>
                                </template>
                            </template>
                        </template>
                    </tbody>
                </table>
                <v-page :key="'pagination-'+pagination.rows" v-if="( (sgStreams !== null) && (pagination.rows < sgStreams.length) )" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="sgstreams.length" @page-change="pageChange" align="center" ref="page"></v-page>
            </template>
            <template v-else>
                <template v-if="crd === null">
					<div class="warningText">
						Loading data...
					</div>
				</template>
				<template v-else>
					<h2>Stream Details</h2>
					
					<div class="configurationDetails">
						<CRDSummary :crd="crd" kind="SGStream" :details="true"></CRDSummary>
					</div>
				</template>
            </template>
        </div>
		<div id="nameTooltip">
			<div class="info"></div>
		</div>
	</div>
</template>

<script>
	import store from '@/store'
	import { mixin } from '../../mixins/mixin'
    import CRDSummary from '../../forms/summary/CRDSummary.vue'

    export default {
        name: 'SGStreams',

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
			sgStreams () {
				return (
					(store.state.sgstreams !== null)
						? this.sortTable([...(store.state.sgstreams.filter(crd => (crd.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type)
						: null
				)
			},

			crd () {
				return (
					(store.state.sgstreams !== null)
						? store.state.sgstreams.find(crd => (crd.data.metadata.namespace == this.$route.params.namespace) && (crd.data.metadata.name == this.$route.params.name))
						: null
				)
			}

		},

        methods: {

            getTargetType(type) {
                let targetTypes = {
                    SGCluster: 'sgCluster',
                    CloudEvent: 'cloudEvent',
                    PgLambda: 'pgLambda'
                }
                
                return targetTypes[type]
            }

        }
	}
</script>