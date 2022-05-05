<template>
	<div id="sg-profile" v-if="loggedIn && isReady && !notFound">
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
								<td v-if="iCan('create','sginstanceprofiles',$route.params.namespace)" colspan="5">
									No profiles have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sginstanceprofiles/new'" title="Add New Instance Profile">create a new one?</router-link>
								</td>
								<td v-else colspan="5">
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
											<a v-if="iCan('delete','sginstanceprofiles',$route.params.namespace)" @click="deleteCRD('sginstanceprofiles',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration" :class="conf.data.status.clusters.length ? 'disabled' : ''"></a>
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
				<h2>Profile Details</h2>
				<div class="configurationDetails" v-for="conf in config" v-if="conf.name == $route.params.name">
					<table class="crdDetails">
						<tbody>
							<tr>
								<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.metadata.name')"></span></td>
								<td>{{ conf.name }}</td>
							</tr>
							<tr>
								<td class="label">RAM <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span></td>
								<td class="textRight">{{ conf.data.spec.memory }}</td>
							</tr>
							<tr v-if="hasProp(conf, 'data.spec.hugePages.hugepages-2Mi')">
								<td class="label">Huge Pages 2Mi <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-2Mi')"></span></td>
								<td class="textRight">{{ conf.data.spec.hugePages['hugepages-2Mi']}}</td>
							</tr>
							<tr v-if="hasProp(conf, 'data.spec.hugePages.hugepages-1Gi')">
								<td class="label">Huge Pages 1Gi <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-1Gi')"></span></td>
								<td class="textRight">{{ conf.data.spec.hugePages['hugepages-1Gi']}}</td>
							</tr>							
							<tr>
								<td class="label">CPU <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span></td>
								<td class="textRight">{{ conf.data.spec.cpu }}</td>
							</tr>
							<tr v-if="conf.data.status.clusters.length">
								<td class="label">Used on <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.status.clusters')"></span></td>
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

			tooltips() {
				return store.state.tooltips
			}

		},
	}

</script>