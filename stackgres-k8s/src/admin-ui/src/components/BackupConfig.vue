<template>
	<div id="bk-config" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="Namespace Clusters Overview">{{ $route.params.namespace }}</router-link>
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
				<div>
					<template v-if="$route.params.hasOwnProperty('name')">
						<template v-for="conf in config" v-if="conf.name == $route.params.name">
							<router-link v-if="iCan('patch','sgbackupconfigs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name + '/edit'" title="Edit Configuration">
								Edit Configuration
							</router-link>
							<a v-if="iCan('create','sgbackupconfigs',$route.params.namespace)" @click="cloneCRD('SGBackupConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration">
								Clone Configuration
							</a>
							<a v-if="iCan('delete','sgbackupconfigs',$route.params.namespace)" @click="deleteCRD('sgbackupconfigs',$route.params.namespace, conf.name, '/' + $route.params.namespace + '/sgbackupconfigs')" title="Delete Configuration"  :class="conf.data.status.clusters.length ? 'disabled' : ''">
								Delete Configuration
							</a>
							<router-link class="borderLeft" :to="'/' + $route.params.namespace + '/sgbackupconfigs'" title="Close Details">Close Details</router-link>
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
				<table id="backup" class="configurations backupConfig resizable" v-columns-resizable>
					<thead class="sort">
						<th class="sorted desc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="Name">
								Name
							</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.metadata.name')"></span>
						</th>
						<th class="desc retention hasTooltip">
							<span @click="sort('data.spec.baseBackups.retention')" title="Retention">Retention</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.retention')"></span>
						</th>
						<th class="desc cronSchedule hasTooltip">
							<span @click="sort('data.spec.baseBackups.cronSchedule')" title="Full Schedule">Full Schedule</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.cronSchedule')"></span>
						</th>
						<th class="desc compression hasTooltip">
							<span @click="sort('data.spec.baseBackups.compression')" title="Compression Method">Compression Method</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.compression')"></span>
						</th>
						<th class="desc uploadDiskConcurrency hasTooltip">
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
										<td class="fontZero">
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
										<td class="fontZero">
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
											<router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name" target="_blank" class="newTab">
												<svg xmlns="http://www.w3.org/2000/svg" width="15.001" height="12.751" viewBox="0 0 15.001 12.751"><g transform="translate(167.001 -31.5) rotate(90)"><path d="M37.875,168.688a.752.752,0,0,1-.53-.219l-5.625-5.626a.75.75,0,0,1,0-1.061l2.813-2.813a.75.75,0,0,1,1.06,1.061l-2.283,2.282,4.566,4.566,4.566-4.566-2.283-2.282a.75.75,0,0,1,1.06-1.061l2.813,2.813a.75.75,0,0,1,0,1.061l-5.625,5.626A.752.752,0,0,1,37.875,168.688Z" transform="translate(0 -1.687)" fill="#00adb5"/><path d="M42.156,155.033l-2.813-2.813a.752.752,0,0,0-1.061,0l-2.813,2.813a.75.75,0,1,0,1.06,1.061l1.533-1.534v5.3a.75.75,0,1,0,1.5,0v-5.3l1.533,1.534a.75.75,0,1,0,1.06-1.061Z" transform="translate(-0.937 0)" fill="#00adb5"/></g></svg>
											</router-link>
											<router-link v-if="iCan('patch','sgbackupconfigs',$route.params.namespace)"  :to="'/' + $route.params.namespace + '/sgbackupconfig/' + conf.name + '/edit'" title="Edit Configuration">
												<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
											</router-link>
											<a v-if="iCan('create','sgbackupconfigs',$route.params.namespace)" @click="cloneCRD('SGBackupConfigs', $route.params.namespace, conf.name)" class="cloneCRD" title="Clone Configuration"><svg xmlns="http://www.w3.org/2000/svg" width="13.9" height="16" viewBox="0 0 20 20"><g><path fill="#00ADB5" d="M2.5,20c-0.5,0-1-0.4-1-1V5c0-0.5,0.4-1,1-1c0.6,0,1,0.4,1,1v12.4c0,0.3,0.3,0.6,0.6,0.6h9.4c0.5,0,1,0.4,1,1c0,0.5-0.4,1-1,1H2.5z"/><path fill="#00ADB5" d="M6.5,16c-0.5,0-0.9-0.4-0.9-0.9V0.9C5.6,0.4,6,0,6.5,0h11.1c0.5,0,0.9,0.4,0.9,0.9v14.1c0,0.5-0.4,0.9-0.9,0.9H6.5z M8,1.8c-0.3,0-0.6,0.3-0.6,0.6v11.2c0,0.3,0.3,0.6,0.6,0.6h8.1c0.3,0,0.6-0.3,0.6-0.6V2.4c0-0.3-0.3-0.6-0.6-0.6H8z"/><path fill="#00ADB5" d="M14.1,5.3H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,4.9,14.6,5.3,14.1,5.3z"/><path fill="#00ADB5" d="M14.1,8.8H10C9.5,8.8,9.1,8.4,9.1,8v0c0-0.5,0.4-0.9,0.9-0.9h4.1C14.6,7.1,15,7.5,15,8v0C15,8.4,14.6,8.8,14.1,8.8z"/><path fill="#00ADB5" d="M14.1,12.4H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,12,14.6,12.4,14.1,12.4z"/></g></svg></a>
											<a v-if="iCan('delete','sgbackupconfigs',$route.params.namespace)" @click="deleteCRD('sgbackupconfigs',$route.params.namespace, conf.name)" class="delete" title="Delete Configuration"  :class="conf.data.status.clusters.length ? 'disabled' : ''">
												<svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
											</a>
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
									<td>
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
										Max Network Bandwitdh
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.maxNetworkBandwitdh')"></span>
									</td>
									<td>
										{{ conf.data.spec.baseBackups.performance.maxNetworkBandwitdh }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.maxDiskBandwitdh')">
									<td class="label">
										Max Disk Bandwitdh
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.maxDiskBandwitdh')"></span>
									</td>
									<td>
										{{ conf.data.spec.baseBackups.performance.maxDiskBandwitdh }}
									</td>
								</tr>
								<tr v-if="hasProp(conf, 'data.spec.baseBackups.performance.uploadDiskConcurrency')">
									<td class="label">
										Upload Disk Concurrency
										<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.baseBackups.performance.uploadDiskConcurrency')"></span>
									</td>
									<td>
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
											Bucket
											<span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.s3.storageClass')"></span>
										</td>
										<td colspan="2">
											{{ conf.data.spec.storage.s3.path }}
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
	import router from '../router'
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