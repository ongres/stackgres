<template>
	<div id="bk-config" v-if="iCanLoad">
		<div class="content">
			<template v-if="!$route.params.hasOwnProperty('name')">
				<table id="backup" class="configurations backupConfig resizable fullWidth" v-columns-resizable>
					<thead class="sort">
						<th class="sorted desc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.metadata.name')"></span>
						</th>
						<th class="desc retention hasTooltip textRight" data-type="retention">
							<span @click="sort('data.spec.baseBackups.retention')" title="Retention">Retention</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.retention')"></span>
						</th>
						<th class="desc cronSchedule hasTooltip">
							<span @click="sort('data.spec.baseBackups.cronSchedule')" title="Full Schedule">Full Schedule</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.cronSchedule')"></span>
						</th>
						<th class="desc compression hasTooltip" data-type="compression">
							<span @click="sort('data.spec.baseBackups.compression')" title="Compression Method">Compression Method</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.compression')"></span>
						</th>
						<th class="desc uploadDiskConcurrency hasTooltip textRight" data-type="concurrency">
							<span @click="sort('data.spec.baseBackups.performance.uploadDiskConcurrency')" title="Upload Disk Concurrency">Upload Disk Concurrency</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.uploadDiskConcurrency')"></span>
						</th>
						<th class="desc storageType hasTooltip">
							<span @click="sort('data.spec.storage.type')" title="Storage Type">Storage Type</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.type')"></span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!config.length">
							<tr class="no-results">
								<td colspan="8" v-if="iCan('create','sgbackupconfigs',$route.params.namespace)">
									No configurations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgbackupconfigs/new'" title="Add New Backup Configuration">create a new one?</router-link>
								</td>
								<td v-else colspan="8">
									No configurations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>	
						<template v-for="(conf, index) in config">
							<template  v-if="(index >= pagination.start) && (index < pagination.end)">
									<tr class="base">
										<td class="hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
													{{ conf.name }}
												</router-link>
											</span>
										</td>
										<td class="fontZero textRight">
											<template v-if="(typeof conf.data.spec.baseBackups.retention !== 'undefined')">
												<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
													{{ conf.data.spec.baseBackups.retention }}
												</router-link>
											</template>
										</td>
										<td class="fontZero hasTooltip">
											<template v-if="(typeof conf.data.spec.baseBackups.cronSchedule !== 'undefined')">
												<span>
													<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
														{{ tzCrontab(conf.data.spec.baseBackups.cronSchedule) | prettyCRON }}
													</router-link>
												</span>
											</template>
										</td>
										<td class="fontZero">
											<template v-if="(typeof conf.data.spec.baseBackups.compression !== 'undefined')">
												<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
													{{ conf.data.spec.baseBackups.compression }}
												</router-link>
											</template>
										</td>
										<td class="fontZero textRight">
											<template v-if="( (typeof conf.data.spec.baseBackups.performance !== 'undefined') && (typeof conf.data.spec.baseBackups.performance.uploadDiskConcurrency !== 'undefined') )">
												<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
													{{ conf.data.spec.baseBackups.performance.uploadDiskConcurrency }}
												</router-link>
											</template>
										</td>
										<td class="fontZero hasTooltip">
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" class="noColor">
													{{ conf.data.spec.storage.type }}
												</router-link>
											</span>
										</td>
										<td class="actions">
											<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" target="_blank" class="newTab"></router-link>
											<router-link v-if="iCan('patch','sgbackupconfigs',$route.params.namespace)"  :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
											<a v-if="iCan('create','sgbackupconfigs',$route.params.namespace)" @click="cloneCRD('SGBackupConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration"></a>
											<a v-if="iCan('delete','sgbackupconfigs',$route.params.namespace)" @click="deleteCRD('sgbackupconfigs',$route.params.namespace, conf.name)" class="delete deleteCRD" title="Delete Configuration"  :class="conf.data.status.clusters.length ? 'disabled' : ''"></a>
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
					<h2>Configuration Details</h2>

					<div class="configurationDetails">
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.metadata.name')"></span></td>
									<td>{{ conf.name }}</td>
								</tr>
								<tr v-if="(typeof conf.data.spec.baseBackups.retention !== 'undefined')">
									<td class="label">
										Retention
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.retention')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.retention }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.cronSchedule')">
									<td class="label">
										Full Schedule
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.cronSchedule')"></span>
									</td>
									<td>
										{{ tzCrontab(conf.data.spec.baseBackups.cronSchedule) | prettyCRON }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.compression')">
									<td class="label">
										Compression Method
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.compression')"></span>
									</td>
									<td>
										{{ conf.data.spec.baseBackups.compression }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.maxNetworkBandwidth')">
									<td class="label">
										Max Network Bandwidth
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.maxNetworkBandwidth')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.performance.maxNetworkBandwidth }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.maxDiskBandwidth')">
									<td class="label">
										Max Disk Bandwidth
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.maxDiskBandwidth')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.performance.maxDiskBandwidth }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.uploadDiskConcurrency')">
									<td class="label">
										Upload Disk Concurrency
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.uploadDiskConcurrency')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.performance.uploadDiskConcurrency }}
									</td>
								</tr>
								<tr v-if="conf.data.status.clusters.length">
									<td class="label">Used on  <span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.status.clusters')"></span></td>
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
					<div class="configurationDetails storageDetails">
						<h2>
							Storage Details
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage')"></span>
						</h2>
						<table class="crdDetails">
							<tbody>
								<tr>
									<td class="label">
										Storage Type
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.type')"></span>
									</td>
									<td colspan="2">
										{{ conf.data.spec.storage.type }}
									</td>
								</tr>
								<template v-if="conf.data.spec.storage.type == 's3'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3.region')">
										<td class="label">
											Region
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.region')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3.region }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3.storageClass')">
										<td class="label">
											Storage Class
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.storageClass')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3.storageClass }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											AWS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.awsCredentials')"></span>
										</td>
										<td class="label">
											Access Key ID
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Secret Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.storage.type === 's3Compatible'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.bucket }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3Compatible.enablePathStyleAddressing')">
										<td class="label">
											Path Style Addressing
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.enablePathStyleAddressing')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.enablePathStyleAddressing ? 'Enabled' : 'Disabled'}}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3Compatible.endpoint')">
										<td class="label">
											Endpoint
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.endpoint')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.endpoint }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3Compatible.region')">
										<td class="label">
											Region
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.region')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.region }}
										</td>
									</tr>
									<tr v-if="hasProp(conf, 'data.spec.storage.s3Compatible.storageClass')">
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.storageClass')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.path }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											AWS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.awsCredentials')"></span>
										</td>
										<td class="label">
											Access Key ID
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Secret Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.storage.type === 'gcs'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.gcs.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.gcs.bucket }}
										</td>
									</tr>
									<tr>
										<td class="label">
											GCS Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.gcs.gcpCredentials')"></span>
										</td>
										<td class="label">
											Service Account JSON
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
								</template>
								<template v-else-if="conf.data.spec.storage.type === 'azureBlob'">
									<tr>
										<td class="label">
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.azureBlob.bucket')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.azureBlob.bucket }}
										</td>
									</tr>
									<tr>
										<td rowspan="2" class="label">
											Azure Credentials
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.azureBlob.azureCredentials')"></span>
										</td>
										<td class="label">
											Storage Account
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount')"></span>
										</td>
										<td>
											********
										</td>
									</tr>
									<tr>
										<td class="label">
											Access Key
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey')"></span>
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
        name: 'SGBackupConfigs',

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
				return this.sortTable( [...(store.state.sgbackupconfigs.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
			},

			tooltips () {
				return store.state.tooltips
			}

		},
		
	}

</script>

<style scoped>
	table.resizable th[data-type="retention"] {
		max-width: 95px;
	}

	table.resizable th[data-type="compression"], table.resizable th[data-type="concurrency"] {
		max-width: 125px;
	}
</style>