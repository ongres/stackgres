var Backups = Vue.component("sg-backup", {
	template: `
		<div id="sg-backup">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						{{ $route.params.namespace }}
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
						<template v-if="$route.params.name !== undefined">
							{{ $route.params.name }}
						</template>
						<template v-else>
							cluster backups
						</template>
					</li>
				</ul>
				<!--<router-link to="/crd/create/pgconfig/" class="btn">Create New Configuration</router-link>-->
			</header>

			<div class="content">
				<div class="backups boxes">
					<div v-for="back in backups" class="box" v-bind:class="{'show':($route.params.name == back.name)}" v-if="back.data.metadata.namespace == currentNamespace">
						<div class="table">
							<div class="head row">
								<div class="col name">
									<h4>Name</h4>
								</div>

								<div class="col">
									<h4>Is Permanent</h4>
								</div>
								<div class="col">
									<h4>Phase</h4>
								</div>
								<template v-if="back.data.status.phase === 'Completed'">
									<div class="col double">
										<h4>Timestamp</h4>
									</div>
									<div class="col">
										<h4>Size</h4>
									</div>
									<div class="col">
										<h4>PG Version</h4>
									</div>
									<div class="col">
										<h4>Tested</h4>
									</div>
									<div class="col details">
										<a class="btn" href="javascript:void(0)">Details</a>
									</div>
								</template>
							</div>
							<div class="row">
								<div class="col name" :data-name="back.data.metadata.name">
									{{ back.data.metadata.name }}
								</div>
								<div class="col upper">
									{{ back.data.spec.isPermanent }}
								</div>
								<div class="col">
									{{ back.data.status.phase }}
								</div>
								<template v-if="back.data.status.phase === 'Completed'">
									<div class="col double">
										{{ back.data.status.time }}
									</div>
									<div class="col">
										{{ back.data.status.uncompressedSize | formatBytes }}
									</div>
									<div class="col">
										{{ back.data.status.pgVersion }}
									</div>
									<div class="col">
										<template v-if="back.data.status.tested">
											TRUE
										</template>
										<template v-else>
											FALSE
										</template>										
									</div>
								</template>
								<div class="col details"></div>
							</div>
						</div>

						<hr>
						<h4 class="basic">Backup Details</h4>

						<div class="row">

							<div class="col">
								<hr>
								<span>UID</span>
								{{ back.data.metadata.uid }}

								<template v-if="back.data.status.phase === 'Completed'">

									<hr>
									<span>Pod</span>
									{{ back.data.status.pod }}

									<hr>
									<span>Name</span>
									{{ back.data.status.name }}

									<hr>
									<span>WAL File Name</span>
									{{ back.data.status.walFileName }}

									</div>
									<div class="col">

									<hr>
									<span>Start Time</span>
									{{ back.data.status.startTime }}

									<hr>
									<span>Finish Time</span>
									{{ back.data.status.finishTime }}
									
									<hr>
									<span>Hostname</span>
									{{ back.data.status.hostname }}

									<hr>
									<span>Data Directory</span>
									{{ back.data.status.dataDir }}

									</div>
									<div class="col">
									
									<hr>
									<span>Start Lsn</span>
									{{ back.data.status.startLsn }}

									<hr>
									<span>Finish Lsn</span>
									{{ back.data.status.finishLsn }}

									<hr>
									<span>System Identifier</span>
									{{ back.data.status.systemIdentifier }}

									<hr>
									<span>Compressed size</span>
									{{ back.data.status.compressedSize | formatBytes }}
								</template>
							</div>
						</div>

						<hr>
						<h4 class="basic">Backup Configuration</h4>

						<div class="row">
							<div class="col">
								<hr>
								<span>Type</span>
								{{ back.data.status.backupConfig.storage.type }}
							</div>
							<div class="col">	
								<hr>
								<span>Compression Method</span>
								{{ back.data.status.backupConfig.compressionMethod }}
							</div>
						</div>

						<hr/>

						<ul class="params">
							<li v-for="(item, index) in back.data.status.backupConfig.storage[back.data.status.backupConfig.storage.type]">
								<template v-if="index == 'credentials'">
									<strong>{{ index }}:</strong><br/>
										<ul class="params">
											<li>
												<strong>accessKey:</strong>
												<ul class="params">
													<li><strong>name:</strong> {{ item.accessKey.name }} </li>
													<li><strong>key:</strong> {{ item.accessKey.key }} </li>
												</ul>
											</li>
										</ul>
										<ul class="params">
											<li>
												<strong>secretKey:</strong>
												<ul class="params">
													<li><strong>name:</strong> {{ item.secretKey.name }} </li>
													<li><strong>key:</strong> {{ item.secretKey.key }} </li>
												</ul>
											</li>
										</ul>
								</template>
								<template v-else>
									<strong>{{ index }}:</strong> {{ item }}<br/>
								</template>
							</li>
						</ul>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			
		}
	},
	computed: {

		backups () {
			return store.state.backups
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	}
})
