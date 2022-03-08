<template>
	<div id="object-storage" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 18.8"><g fill="#36A8FF"><path d="M1 4.8h10.5c.5 0 1-.4 1-1s-.4-1-1-1H1c-.5 0-1 .4-1 1s.5 1 1 1zM14.8 7.6c.5 0 1-.4 1-1V4.8h4.3c.5 0 1-.4 1-1s-.4-1-1-1h-4.3V1c0-.5-.4-1-1-1s-1 .4-1 1v5.7c.1.5.5.9 1 .9zM1 11h4.3v1.9c0 .5.4 1 1 1s1-.4 1-1V7.1c0-.5-.4-1-1-1s-1 .4-1 1V9H1c-.5 0-1 .5-1 1s.4.9 1 1c-.1 0 0 0 0 0zM7.7 15.3H1c-.5 0-1 .4-1 .9s.4 1 .9 1h6.8c.5 0 1-.4 1-.9 0-.6-.4-1-1-1z"/><g><path d="M14.275 18.7c-.8.1-1.6-.1-2.3-.6-.7-.4-1.2-1-1.5-1.7-.4-.8-.6-1.6-.6-2.5 0-.9.2-1.8.5-2.6.3-.7.9-1.3 1.5-1.7.7-.4 1.5-.6 2.3-.6.8 0 1.6.2 2.3.6.7.4 1.2 1 1.5 1.7.5.8.7 1.7.7 2.6 0 .9-.2 1.8-.5 2.6-.4.7-.9 1.2-1.6 1.6-.7.5-1.5.7-2.3.6zm0-1.6c.7 0 1.4-.3 1.8-.8.5-.7.7-1.6.6-2.4.1-.9-.2-1.7-.6-2.4-.5-.6-1.1-.9-1.8-.9s-1.4.3-1.8.9c-.4.7-.7 1.5-.6 2.4-.1.8.2 1.7.6 2.4.4.5 1.1.8 1.8.8zM22.875 18.7c-.6 0-1.3-.1-1.9-.2-.5-.1-1-.4-1.4-.7 0-.1-.1-.2-.2-.3-.1-.2-.1-.3-.1-.5s.1-.4.2-.6c.1-.2.3-.2.4-.3h.3c.1 0 .2.1.3.2.3.2.7.4 1.1.5.5.3.9.3 1.3.3s.9-.1 1.3-.3c.3-.2.5-.5.4-.9 0-.3-.2-.5-.4-.7-.5-.2-1-.4-1.5-.5-.6-.1-1.3-.3-1.9-.6-.4-.2-.8-.5-1-.9-.2-.3-.3-.8-.3-1.2 0-.5.2-1.1.5-1.5.3-.5.8-.8 1.3-1.1.6-.3 1.2-.4 1.8-.4 1.1 0 2.1.3 3 1l.3.3c.1.1.1.3.1.4 0 .2-.1.4-.2.6-.1.2-.3.2-.4.3h-.3c-.1 0-.2-.1-.3-.2-.3-.2-.6-.4-1-.5-.4-.1-.7-.2-1.1-.2-.4 0-.9.1-1.2.3-.3.2-.5.5-.4.9 0 .2.1.4.2.5.2.2.4.3.6.4.1.1.5.2.9.3.9.2 1.7.5 2.5 1 .5.4.8 1.1.8 1.7 0 .5-.1 1.1-.4 1.5-.3.5-.8.8-1.3 1-.7.3-1.3.5-2 .4z"/></g></g></svg>
					<template v-if="$route.params.hasOwnProperty('name')">
						<router-link :to="'/' + $route.params.namespace + '/sgobjectstorages'" title="SGObjectStorageList">SGObjectStorageList</router-link>
					</template>
					<template v-else>
						SGObjectStorageList
					</template>
				</li>
				<li v-if="(typeof $route.params.name !== 'undefined')">
					{{ $route.params.name }}
				</li>
			</ul>

			<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgobjectstorage/" target="_blank" title="SGObjectStorage Documentation">SGObjectStorage Documentation</a>
				<div>
					<template v-if="$route.params.hasOwnProperty('name')">
						<template v-for="conf in config" v-if="conf.name == $route.params.name">
							<router-link v-if="iCan('patch','sgobjectstorages',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name + '/edit'" title="Edit Configuration">
								Edit Configuration
							</router-link>
							<a v-if="iCan('create','sgobjectstorages',$route.params.namespace)" @click="cloneCRD('SGObjectStorages', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration">
								Clone Configuration
							</a>
							<a v-if="iCan('delete','sgobjectstorages',$route.params.namespace)" @click="deleteCRD('sgobjectstorages',$route.params.namespace, conf.name, '/' + $route.params.namespace + '/sgobjectstorages')" title="Delete Configuration"  :class="conf.data.status.clusters.length ? 'disabled' : ''">
								Delete Configuration
							</a>
							<router-link class="borderLeft" :to="'/' + $route.params.namespace + '/sgobjectstorages'" title="Close Details">Close Details</router-link>
						</template>
					</template>
					<template v-else>
						<router-link v-if="iCan('create','sgobjectstorages',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgobjectstorages/new'" class="add">Add New</router-link>
					</template>
				</div>	
			</div>	
		</header>


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
                        <th class="path hasTooltip">
							<span title="Path">Path</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type.path')"></span>
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
										<td class="hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + conf.name" class="noColor">
													{{ conf.data.spec[conf.data.spec.type].path }}
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
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">
										Name 
										<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.metadata.name')"></span>
									</td>
									<td colspan="2">
										{{ conf.name }}
									</td>
								</tr>
								<tr>
									<td class="label">
										Storage Type
										<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type')"></span>
									</td>
									<td colspan="2">
										{{ conf.data.spec.type }}
									</td>
								</tr>
								<template v-if="conf.data.spec.type == 's3'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3.path }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3.region')">
										<td class="label">
											Region
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.region')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3.region }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3.storageClass')">
										<td class="label">
											Storage Class
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.storageClass')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3.storageClass }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											AWS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials')"></span>
										</td>
										<td class="label">
											Access Key ID
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Secret Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.type === 's3Compatible'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.path }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.enablePathStyleAddressing')">
										<td class="label">
											Path Style Addressing
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.enablePathStyleAddressing')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.enablePathStyleAddressing ? 'Enabled' : 'Disabled'}}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.endpoint')">
										<td class="label">
											Endpoint
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.endpoint')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.endpoint }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.region')">
										<td class="label">
											Region
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.region')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.region }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.storageClass')">
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.storageClass')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.s3Compatible.path }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											AWS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials')"></span>
										</td>
										<td class="label">
											Access Key ID
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Secret Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.type === 'gcs'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.gcs.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.gcs.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.gcs.path }}
										</td>
									</tr>
									<tr>
										<td class="label">
											GCS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials')"></span>
										</td>
										<td class="label">
											Service Account JSON
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.type === 'azureBlob'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.azureBlob.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.azureBlob.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.azureBlob.path }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											Azure Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials')"></span>
										</td>
										<td class="label">
											Storage Account
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
							</tbody>
						</table>
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
        name: 'SGObjectStorages',

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
				return this.sortTable( [...(store.state.sgobjectstorages.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			}

		},
		
	}

</script>