<template>
	<div id="sg-profile" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#00adb5"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#00adb5"/></g></svg>
					<template v-if="$route.params.hasOwnProperty('name')">
						<router-link :to="'/' + $route.params.namespace + '/sginstanceprofiles'" title="SGInstanceProfileList">SGInstanceProfileList</router-link>
					</template>
					<template v-else>
						SGInstanceProfileList
					</template>
				</li>
				<li v-if="(typeof $route.params.name !== 'undefined')">
					{{ $route.params.name }}
				</li>
			</ul>

			<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sginstanceprofile/" target="_blank" title="SGInstanceProfile Documentation">SGInstanceProfile Documentation</a>

				<div class="crdActionLinks">
					<template v-if="$route.params.hasOwnProperty('name')">
						<template v-for="conf in config" v-if="conf.name == $route.params.name">
							<router-link v-if="iCan('patch','sginstanceprofiles',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sginstanceprofile/' + conf.name + '/edit'" title="Edit Profile">
								Edit Profile
							</router-link>
							<a v-if="iCan('create','sginstanceprofiles',$route.params.namespace)" @click="cloneCRD('SGInstanceProfiles', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Profile">
								Clone Profile
							</a>
							<a v-if="iCan('delete','sginstanceprofiles',$route.params.namespace)" @click="deleteCRD('sginstanceprofiles',$route.params.namespace, conf.name, '/' + $route.params.namespace + '/sginstanceprofiles')" title="Delete Profile" class="deleteCRD" :class="conf.data.status.clusters.length ? 'disabled' : ''">
								Delete Profile
							</a>
							<router-link :to="'/' + $route.params.namespace + '/sginstanceprofiles'" title="Close Details">Close Details</router-link>
						</template>
					</template>
					<template v-else>
						<router-link v-if="iCan('create','sginstanceprofiles',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sginstanceprofiles/new'" class="add">Add New</router-link>
					</template>
				</div>
			</div>
		</header>

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
	import router from '../router'
	import store from '../store'

    export default {
        name: 'InstanceProfile',

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
				return this.sortTable( [...(store.state.profiles.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			tooltips() {
				return store.state.tooltips
			}

		},
	}

</script>