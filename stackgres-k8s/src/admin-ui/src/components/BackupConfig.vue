<template>
	<div id="bk-config" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
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
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.maxNetworkBandwitdh')">
									<td class="label">
										Max Network Bandwidth
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.maxNetworkBandwitdh')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.performance.maxNetworkBandwitdh }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.maxDiskBandwitdh')">
									<td class="label">
										Max Disk Bandwitdh
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.maxDiskBandwitdh')"></span>
									</td>
									<td class="textRight">
										{{ conf.data.spec.baseBackups.performance.maxDiskBandwitdh }}
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
        name: 'BackupConfig',

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
				return this.sortTable( [...(store.state.backupConfig.filter(conf => (conf.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
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