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
									<tr v-if="hasProp(conf, 'data.spec.s3Compatible.enablePathStyleAddressing')">
										<td class="label">
											Path Style Addressing
											<span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.enablePathStyleAddressing')"></span>
										</td>
										<td colspan="2">
											{{ isEnabled(conf.data.spec.s3Compatible.enablePathStyleAddressing) }}
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
											{{ conf.data.spec.s3Compatible.storageClass }}
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