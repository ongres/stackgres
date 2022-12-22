<template>
	<div id="sg-profile" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="profiles" class="profiles pgConfig resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="asc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.metadata.name')"></span>
						</th>
						<th class="sorted asc memory hasTooltip textRight">
							<span @click="sort('data.spec.memory', 'memory')" title="RAM">
								RAM
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span>
						</th>
						<th class="sorted asc memory hasTooltip textRight">
							<span @click="sort('data.spec.hugePages.hugepages-2Mi', 'memory')" title="Huge Pages 2Mi">
								Huge Pages 2Mi
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-2Mi')"></span>
						</th>
						<th class="sorted asc memory hasTooltip textRight">
							<span @click="sort('data.spec.hugePages.hugepages-1Gi', 'memory')" title="Huge Pages 1Gi">
								Huge Pages 1Gi
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-1Gi')"></span>
						</th>
						<th class="asc cpu hasTooltip textRight">
							<span @click="sort('data.spec.cpu', 'cpu')" title="CPU">
								CPU
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!config.length">
							<tr class="no-results">
								<td v-if="iCan('create','sginstanceprofiles',$route.params.namespace)" colspan="6">
									No profiles have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sginstanceprofiles/new'" title="Add New Instance Profile">create a new one?</router-link>
								</td>
								<td v-else colspan="6">
									No configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
						<template v-else>
							<template v-for="(conf, index) in config">
								<template  v-if="(index >= pagination.start) && (index < pagination.end)">
									<tr class="base">
										<td class="hasTooltip configName">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" class="noColor">
													{{ conf.name }}
												</router-link>
											</span>
										</td>
										<td class="memory fontZero textRight">
											<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" class="noColor">
												{{ conf.data.spec.memory }}
											</router-link>
										</td>
										<td class="memory fontZero textRight">
											<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" class="noColor">
												{{ hasProp(conf, 'data.spec.hugePages.hugepages-2Mi') ? conf.data.spec.hugePages['hugepages-2Mi'] : '' }}
											</router-link>
										</td>
										<td class="memory fontZero textRight">
											<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" class="noColor">
												{{ hasProp(conf, 'data.spec.hugePages.hugepages-1Gi') ? conf.data.spec.hugePages['hugepages-1Gi'] : '' }}
											</router-link>
										</td>
										<td class="cpu fontZero textRight">
											<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" class="noColor">
												{{ conf.data.spec.cpu }}
											</router-link>
										</td>
										<td class="actions">
											<router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name" target="_blank" class="newTab"></router-link>
											<router-link v-if="iCan('patch','sginstanceprofiles',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
											<a v-if="iCan('create','sginstanceprofiles',$route.params.namespace)" @click="cloneCRD('SGInstanceProfiles', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Profile"></a>
											<a v-if="iCan('delete','sginstanceprofiles',$route.params.namespace)" @click="deleteCRD('sginstanceprofiles',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration" :class="(conf.data.status.clusters.length || (typeof logsClusters.find(l => ( (l.data.metadata.namespace == conf.data.metadata.namespace) && (l.data.spec.sgInstanceProfile == conf.name) ))) != 'undefined') ? 'disabled' : ''"></a>
										</td>
									</tr>
								</template>
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
					<h2>Profile Details</h2>
					<div class="configurationDetails">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.metadata.name')"></span></td>
									<td :colspan="2">{{ conf.name }}</td>
								</tr>
								<tr>
									<td class="label">RAM <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span></td>
									<td :colspan="2" class="textRight">{{ conf.data.spec.memory }}</td>
								</tr>
								<tr>
									<td class="label">CPU <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span></td>
									<td :colspan="2" class="textRight">{{ conf.data.spec.cpu }}</td>
								</tr>
								<template v-if="hasProp(conf, 'data.spec.hugePages')">
									<tr v-for="(value, key, index) in conf.data.spec.hugePages">
										<td v-if="!index" class="label" :rowspan="Object.keys(conf.data.spec.hugePages).length">
											Huge Pages
											<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages')"></span>
										</td>
										<td class="label">
											Huge Pages {{ key.slice(-3) }}
											<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.' + key)"></span>
										</td>
										<td class="textRight">
											{{ value }}
										</td>
									</tr>
								</template>				
								<tr v-if="conf.data.status.clusters.length || ((typeof logsClusters.find(l => ( (l.data.metadata.namespace == conf.data.metadata.namespace) && (l.data.spec.sgInstanceProfile == conf.name) ))) != 'undefined' )">
									<td class="label">Used on <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.status.clusters').replace('[SGClusters](https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/)', 'resources')"></span></td>
									<td class="usedOn">
										<ul>
											<li v-for="cluster in conf.data.status.clusters">
												<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
													{{ cluster }}
													<span class="eyeIcon"></span>
												</router-link>
											</li>
											<template v-for="lcluster in logsClusters">
												<li v-if="(lcluster.data.metadata.namespace == conf.data.metadata.namespace) && (lcluster.data.spec.sgInstanceProfile == conf.name)">
													<router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + lcluster.name" title="Logs Server Details">
														{{ lcluster.name }}
														<span class="eyeIcon"></span>
													</router-link>
												</li>
											</template>
										</ul>
									</td>
								</tr>
							</tbody>
						</table>
					</div>

					<template v-if="conf.data.spec.hasOwnProperty('containers')">
						<h2>
							Containers Specs
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers')"></span>
						</h2>
						<div class="configurationDetails">
							<table class="crdDetails">
								<tbody>
									<template v-for="(container, containerName) in conf.data.spec.containers">
										<tr>
											<td class="label" :rowspan="(2 + ( container.hasOwnProperty('hugePages') && Object.keys(container.hugePages).length ) )">
												{{ containerName }}
											</td>
											<td class="label">
												CPU
												<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.cpu')"></span>
											</td>    
											<td colspan="2">
												{{ container.cpu }}
											</td>
										</tr>
										<tr>
											<td class="label">
												Memory
												<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.memory')"></span>
											</td>    
											<td colspan="2">
												{{ container.memory }}
											</td>
										</tr>
										<template v-if="container.hasOwnProperty('hugePages')">
											<tr>
												<td class="label" :rowspan="Object.keys(container.hugePages).length">
													Huge Pages
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages')"></span>
												</td>
												<td class="label">
													Huge Pages {{ container.hugePages.hasOwnProperty('hugepages-2Mi') ? '2Mi' : '1Gi' }}
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.' + (container.hugePages.hasOwnProperty('hugepages-2Mi') ? 'hugepages-2Mi' : 'hugepages-1Gi') )"></span>
												</td>
												<td class="textRight">
													{{ container.hugePages.hasOwnProperty('hugepages-2Mi') ? container.hugePages['hugepages-2Mi'] : container.hugePages['hugepages-1Gi'] }}
												</td>
											</tr>
											<tr v-if="( container.hugePages.hasOwnProperty('hugepages-2Mi') && container.hugePages.hasOwnProperty('hugepages-1Gi') )">
												<td class="label">
													Huge Pages 1Gi
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
												</td>
												<td class="textRight">
													{{ container.hugePages['hugepages-1Gi'] }}
												</td>
											</tr>
										</template>
									</template>
								</tbody>
							</table>
						</div>
					</template>

					<template v-if="conf.data.spec.hasOwnProperty('initContainers')">
						<h2>
							Init Containers Specs
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers')"></span>
						</h2>
						<div class="configurationDetails">
							<table class="crdDetails">
								<tbody>
									<template v-for="(container, containerName) in conf.data.spec.initContainers">
										<tr>
											<td class="label" :rowspan="(2 + ( container.hasOwnProperty('hugePages') && Object.keys(container.hugePages).length ) )">
												{{ containerName }}
											</td>
											<td class="label">
												CPU
												<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.cpu')"></span>
											</td>    
											<td colspan="2">
												{{ container.cpu }}
											</td>
										</tr>
										<tr>
											<td class="label">
												Memory
												<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.memory')"></span>
											</td>    
											<td colspan="2">
												{{ container.memory }}
											</td>
										</tr>
										<template v-if="container.hasOwnProperty('hugePages')">
											<tr>
												<td class="label" :rowspan="Object.keys(container.hugePages).length">
													Huge Pages
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages')"></span>
												</td>
												<td class="label">
													Huge Pages {{ container.hugePages.hasOwnProperty('hugepages-2Mi') ? '2Mi' : '1Gi' }}
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.' + (container.hugePages.hasOwnProperty('hugepages-2Mi') ? 'hugepages-2Mi' : 'hugepages-1Gi') )"></span>
												</td>
												<td class="textRight">
													{{ container.hugePages.hasOwnProperty('hugepages-2Mi') ? container.hugePages['hugepages-2Mi'] : container.hugePages['hugepages-1Gi'] }}
												</td>
											</tr>
											<tr v-if="( container.hugePages.hasOwnProperty('hugepages-2Mi') && container.hugePages.hasOwnProperty('hugepages-1Gi') )">
												<td class="label">
													Huge Pages 1Gi
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
												</td>
												<td class="textRight">
													{{ container.hugePages['hugepages-1Gi'] }}
												</td>
											</tr>
										</template>
									</template>
								</tbody>
							</table>
						</div>
					</template>

					<template v-if="conf.data.spec.hasOwnProperty('requests')">
						<h2>
							Requests Specs
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests')"></span>
						</h2>
						
						<div class="configurationDetails">
							<table class="crdDetails">
								<tbody>
									<tr v-if="conf.data.spec.requests.hasOwnProperty('cpu')">
										<td class="label">
											CPU
											<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.cpu')"></span>
										</td>    
										<td colspan="3">
											{{ conf.data.spec.requests.cpu }}
										</td>
									</tr>
									<tr v-if="conf.data.spec.requests.hasOwnProperty('memory')">
										<td class="label">
											Memory
											<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.memory')"></span>
										</td>    
										<td colspan="3">
											{{ conf.data.spec.requests.memory }}
										</td>
									</tr>
									<template v-if="conf.data.spec.requests.hasOwnProperty('containers')">
										<template v-for="(container, containerName, index) in conf.data.spec.requests.containers">
											<tr>
												<td class="label" v-if="!index" :rowspan="( Object.keys(conf.data.spec.requests.containers).length * 2)">
													Containers
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers')"></span>
												</td>
												<td class="label" rowspan="2">
													{{ containerName }}
												</td>
												<td class="label">
													CPU
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.cpu')"></span>
												</td>    
												<td>
													{{ container.cpu }}
												</td>
											</tr>
											<tr>
												<td class="label">
													Memory
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.memory')"></span>
												</td>    
												<td colspan="2">
													{{ container.memory }}
												</td>
											</tr>
										</template>
									</template>
									<template v-if="conf.data.spec.requests.hasOwnProperty('initContainers')">
										<template v-for="(container, containerName, index) in conf.data.spec.requests.initContainers">
											<tr>
												<td class="label" v-if="!index" :rowspan="( Object.keys(conf.data.spec.requests.initContainers).length * 2)">
													Init Containers
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers')"></span>
												</td>
												<td class="label" rowspan="2">
													{{ containerName }}
												</td>
												<td class="label">
													CPU
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.cpu')"></span>
												</td>    
												<td>
													{{ container.cpu }}
												</td>
											</tr>
											<tr>
												<td class="label">
													Memory
													<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.memory')"></span>
												</td>    
												<td colspan="2">
													{{ container.memory }}
												</td>
											</tr>
										</template>
									</template>
								</tbody>
							</table>
						</div>
					</template>
				</template>
			</template>
		</div>
	</div>
</template>

<script>
	import { mixin } from './mixins/mixin'
	import store from '../store'

    export default {
        name: 'SGInstanceProfiles',

		mixins: [mixin],

		data: function() {

			return {
				currentSort: {
					param: 'data.spec.memory',
					type: 'memory'
				},
				currentSortDir: 'asc',
			}
		},
		computed: {

			config () {
				return this.sortTable( [...(store.state.sginstanceprofiles.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			logsClusters(){
                return store.state.sgdistributedlogs
            },

			tooltips() {
				return store.state.tooltips
			}

		},
	}

</script>