<template>
	<div id="bk-config" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><g fill="#00adb5"><path d="M.955 4.767h10.5a.953.953 0 1 0 0-1.906H.955a.953.953 0 1 0 0 1.906ZM14.795 7.625a.953.953 0 0 0 .955-.953V4.767h4.295a.953.953 0 1 0 0-1.906H15.75V.954a.954.954 0 0 0-1.909 0v5.713a.953.953 0 0 0 .954.958ZM.955 10.967H5.25v1.9a.954.954 0 0 0 1.909 0V7.148a.954.954 0 0 0-1.909 0v1.906H.955a.954.954 0 1 0 0 1.907ZM7.636 15.251H.955a.954.954 0 1 0 0 1.907h6.681a.954.954 0 1 0 0-1.907ZM18.073 9.481a.852.852 0 0 0-.668-.293.944.944 0 0 0-.86.667L14.2 14.867l-2.354-5.011a.959.959 0 0 0-.883-.669.834.834 0 0 0-.663.3 1.09 1.09 0 0 0-.238.726v7.568a1.037 1.037 0 0 0 .22.692.776.776 0 0 0 .624.278.787.787 0 0 0 .631-.284 1.038 1.038 0 0 0 .225-.686v-4.314l1.568 3.248a1.318 1.318 0 0 0 .355.5.819.819 0 0 0 1.012-.01 1.458 1.458 0 0 0 .35-.486l1.557-3.3v4.361a1.037 1.037 0 0 0 .22.692.776.776 0 0 0 .623.278.823.823 0 0 0 .632-.272 1.009 1.009 0 0 0 .235-.7V10.21a1.081 1.081 0 0 0-.241-.729ZM26.1 14.635a2.6 2.6 0 0 1 .4 1.469 2.388 2.388 0 0 1-.77 1.885 3.09 3.09 0 0 1-2.12.681h-3.079a.7.7 0 0 1-.543-.214.849.849 0 0 1-.2-.6v-7.789a.851.851 0 0 1 .2-.6.7.7 0 0 1 .543-.214h2.96a3.041 3.041 0 0 1 2.06.648 2.274 2.274 0 0 1 .746 1.811 2.354 2.354 0 0 1-.352 1.3 2.047 2.047 0 0 1-.973.8 2.038 2.038 0 0 1 1.128.823Zm-4.806-1.417h1.947q1.587 0 1.587-1.322a1.2 1.2 0 0 0-.393-.99 1.872 1.872 0 0 0-1.194-.32h-1.947ZM24.661 17a1.311 1.311 0 0 0 .382-1.042 1.349 1.349 0 0 0-.387-1.056 1.782 1.782 0 0 0-1.213-.347h-2.149v2.779h2.149A1.828 1.828 0 0 0 24.661 17Z"/></g></svg>
					<template v-if="$route.params.hasOwnProperty('name')">
						<router-link :to="'/' + $route.params.namespace + '/sgbackupconfigs'" title="SGBackupConfigList">SGBackupConfigList</router-link>
					</template>
					<template v-else>
						SGBackupConfigList
					</template>
				</li>
				<li v-if="(typeof $route.params.name !== 'undefined')">
					{{ $route.params.name }}
				</li>
			</ul>

			<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgbackupconfig/" target="_blank" title="SGBackupConfig Documentation">SGBackupConfig Documentation</a>
				<div class="crdActionLinks">
					<template v-if="$route.params.hasOwnProperty('name')">
						<template v-for="conf in config" v-if="conf.name == $route.params.name">
							<router-link v-if="iCan('patch','sgbackupconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name + '/edit'" title="Edit Configuration">
								Edit Configuration
							</router-link>
							<a v-if="iCan('create','sgbackupconfigs',$route.params.namespace)" @click="cloneCRD('SGBackupConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration">
								Clone Configuration
							</a>
							<a v-if="iCan('delete','sgbackupconfigs',$route.params.namespace)" @click="deleteCRD('sgbackupconfigs',$route.params.namespace, conf.name, '/' + $route.params.namespace + '/sgbackupconfigs')" title="Delete Configuration"  class="deleteCRD" :class="conf.data.status.clusters.length ? 'disabled' : ''">
								Delete Configuration
							</a>
							<router-link :to="'/' + $route.params.namespace + '/sgbackupconfigs'" title="Close Details">Close Details</router-link>
						</template>
					</template>
					<template v-else>
						<router-link v-if="iCan('create','sgbackupconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgbackupconfigs/new'" class="add">Add New</router-link>
					</template>
				</div>	
			</div>	
		</header>


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
									<tr v-if="hasProp(conf, 'data.spec.storage.s3.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3.path }}
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
									<tr v-if="hasProp(conf, 'data.spec.storage.s3Compatible.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3Compatible.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3Compatible.path }}
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
									<tr v-if="hasProp(conf, 'data.spec.storage.gcs.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.gcs.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.gcs.path }}
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
									<tr v-if="hasProp(conf, 'data.spec.storage.azureBlob.path')">
										<td class="label">
											Path
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.azureBlob.path')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.azureBlob.path }}
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