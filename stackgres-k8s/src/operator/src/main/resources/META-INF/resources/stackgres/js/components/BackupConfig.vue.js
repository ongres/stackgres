var BackupConfig = Vue.component("backup-config", {
	template: `
		<div id="bk-config">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21 20"><path d="M.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm19.09-1.905h-10.5a.953.953 0 100 1.9h10.5a.953.953 0 100-1.9zm0 6.191h-8.59v-1.9a.955.955 0 00-1.91 0v5.715a.955.955 0 001.91 0v-1.9h8.59a.953.953 0 100-1.905zm-12.409 0H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"/></svg>
						Configurations
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
						<router-link :to="'/backup/'+currentNamespace" title="Backup">Backup</router-link>
					</li>
					<li v-if="typeof $route.params.name !== 'undefined'">
						{{ $route.params.name }}
					</li>
				</ul>
				<router-link to="/crd/create/backupconfig/" class="add">Add New</router-link>
			</header>


			<div class="content">
				<table class="backupConfig">
					<thead class="sort">
						<th @click="sort('data.metadata.name')" class="sorted desc name">
							<span>Name</span>
						</th>
						<th @click="sort('data.spec.baseBackups.retention')" class="desc retention">
							<span>Retention</span>
						</th>
						<th @click="sort('data.spec.baseBackups.cronSchedule')" class="desc cronSchedule">
							<span>Full Schedule</span>
						</th>
						<th @click="sort('data.spec.baseBackups.compression')" class="desc compression">
							<span>Compression Method</span>
						</th>
						<th @click="sort('data.spec.baseBackups.performance.uploadDiskConcurrency')" class="desc uploadDiskConcurrency">
							<span>Upload Disk Concurrency</span>
						</th>
						<!--<th @click="sort('data.spec.tarSizeThreshold')" class="desc tarSizeThreshold">
							<span>Tar Size Threshold</span>
						</th>-->
						<th @click="sort('data.spec.storage.type')" class="desc storageType">
							<span>Storage Type</span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<tr class="no-results">
							<td colspan="8">
								No configurations have been found, would you like to <router-link to="/crd/create/backupconfig/" title="Add New Backup Configuration">create a new one?</router-link>
							</td>
						</tr>
						<template v-for="conf in config" v-if="conf.data.metadata.namespace == currentNamespace">
							<tr :class="[ $route.params.name == conf.name ? 'open' : '', 'sgbackupconfig-'+conf.data.metadata.namespace+'-'+conf.name ]" class="base">
								<td>{{ conf.name }}</td>
								<td>
									<template v-if="typeof conf.data.spec.baseBackups.retention !== 'undefined'">
										{{ conf.data.spec.baseBackups.retention }}
									</template>
								</td>
								<td>
									<template v-if="typeof conf.data.spec.baseBackups.retention !== 'undefined'">
										{{ conf.data.spec.baseBackups.cronSchedule | prettyCRON }}
									</template>
								</td>
								<td>
									<template v-if="typeof conf.data.spec.baseBackups.retention !== 'undefined'">
										{{ conf.data.spec.baseBackups.compression }}
									</template>
								</td>
								<td>
									<template v-if="( (typeof conf.data.spec.baseBackups.performance !== 'undefined') && (typeof conf.data.spec.baseBackups.performance.uploadDiskConcurrency !== 'undefined') )">
										{{ conf.data.spec.baseBackups.performance.uploadDiskConcurrency }}
									</template>
								</td>
								<td>{{ conf.data.spec.storage.type }}</td>
								<td class="actions">
									<router-link :to="'/configurations/backup/'+conf.data.metadata.namespace+'/'+conf.name" title="Configuration Details">
										<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
									</router-link>
									<router-link :to="'/crd/edit/backupconfig/'+currentNamespace+'/'+conf.name" title="Edit Configuration">
										<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
									</router-link>
									<a v-on:click="deleteCRD('sgbackupconfig',currentNamespace, conf.name)" class="delete" title="Delete Configuration">
										<svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
									</a>
								</td>
							</tr>
							<tr :class="[ $route.params.name == conf.name ? 'open' : '', 'sgbackupconfig-'+conf.data.metadata.namespace+'-'+conf.name ]" class="details" :style="$route.params.name == conf.name ? 'display: table-row;' : ''">
								<td colspan="8">
									<ul class="yaml">
										<template v-if="conf.data.spec.storage.type === 's3'">
											<li>
												<strong class="label">bucket:</strong> {{ conf.data.spec.storage.s3.bucket }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3.path !== 'undefined'">
												<strong class="label">path:</strong> {{ conf.data.spec.storage.s3.path }}
											</li>
											<li>
												<strong class="label">awsCredentials:</strong> 
												<ul>
													<li>
														<strong class="label">accessKeyId:</strong> ****
													</li>
													<li>
														<strong class="label">secretAccessKey:</strong> ****
													</li>
												</ul>
											</li>
											<li v-if="typeof conf.data.spec.storage.s3.region !== 'undefined'">
												<strong class="label">region:</strong> {{ conf.data.spec.storage.s3.region }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3.storageClass !== 'undefined'">
												<strong class="label">storageClass:</strong> {{ conf.data.spec.storage.s3.storageClass }}
											</li>
										</template>
										<template v-else-if="conf.data.spec.storage.type === 's3Compatible'">
											<li>
												<strong class="label">bucket:</strong> {{ conf.data.spec.storage.s3Compatible.bucket }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3Compatible.path !== 'undefined'">
												<strong class="label">path:</strong> {{ conf.data.spec.storage.s3Compatible.path }}
											</li>
											<li>
												<strong class="label">awsCredentials:</strong> 
												<ul>
													<li>
														<strong class="label">accessKeyId:</strong> ****
													</li>
													<li>
														<strong class="label">secretAccessKey:</strong> ****
													</li>
												</ul>
											</li>
											<li v-if="typeof conf.data.spec.storage.s3Compatible.region !== 'undefined'">
												<strong class="label">region:</strong> {{ conf.data.spec.storage.s3Compatible.region }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3Compatible.storageClass !== 'undefined'">
												<strong class="label">storageClass:</strong> {{ conf.data.spec.storage.s3Compatible.storageClass }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3Compatible.endpoint !== 'undefined'">
												<strong class="label">endpoint:</strong> {{ conf.data.spec.storage.s3Compatible.endpoint }}
											</li>
											<li v-if="typeof conf.data.spec.storage.s3Compatible.enablePathStyleAddressing !== 'undefined'">
												<strong class="label">enablePathStyleAddressing:</strong> {{ conf.data.spec.storage.s3Compatible.enablePathStyleAddressing }}
											</li>
										</template>
										<template v-else-if="conf.data.spec.storage.type === 'gcs'">
											<li>
												<strong class="label">bucket:</strong> {{ conf.data.spec.storage.gcs.bucket }}
											</li>
											<li v-if="typeof conf.data.spec.storage.gcs.path !== 'undefined'">
												<strong class="label">path:</strong> {{ conf.data.spec.storage.gcs.path }}
											</li>
											<li>
												<strong class="label">gcpCredentials:</strong> 
												<ul>
													<li>
														<strong class="label">serviceAccountJSON:</strong> ****
													</li>
												</ul>
											</li>
										</template>
										<template v-else-if="conf.data.spec.storage.type === 'azureBlob'">
											<li>
												<strong class="label">bucket:</strong> {{ conf.data.spec.storage.azureBlob.bucket }}
											</li>
											<li v-if="typeof conf.data.spec.storage.azureBlob.path !== 'undefined'">
												<strong class="label">path:</strong> {{ conf.data.spec.storage.azureBlob.path }}
											</li>
											<li>
												<strong class="label">azureCredentials:</strong> 
												<ul>
													<li>
														<strong class="label">storageAccount:</strong> ****
													</li>
													<li>
														<strong class="label">accessKey:</strong> ****
													</li>
												</ul>
											</li>
										</template>
									</ul>
								</td>
							</tr>
						</template>
					</tbody>
				</table>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.metadata.name',
			currentSortDir: 'desc',
		}
	},
	computed: {

		config () {
			//return store.state.backupConfig
			return sortTable( store.state.backupConfig, this.currentSort, this.currentSortDir )
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	},
	methods: {
		
	}
})
