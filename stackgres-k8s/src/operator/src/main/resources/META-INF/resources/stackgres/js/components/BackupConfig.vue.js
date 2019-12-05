var BackupConfig = Vue.component("backup-config", {
	template: `
		<div id="bk-config">
			<header>
				<h2 class="title">POSTGRESQL BACKUPS</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" class="box" v-bind:class="{'show':($route.params.name == conf.name)}" v-if="conf.data.metadata.namespace == currentNamespace">
						<h4>{{ conf.name }}</h4>

						<div class="row">
							<div class="col">
								<span>General Configuration</span>
								<ul class="params">
									<li v-for="(item, index) in conf.data.spec" v-if="index !== 'storage'">
										<template v-if="index == 'tarSizeThreshold'">
											<strong>{{ index }}:</strong> {{ (conf.data.spec.tarSizeThreshold / 1e+9).toFixed(2) }}Gi<br/>
										</template>
										<template v-else-if="index == 'fullSchedule'">
											<strong>{{ index }}:</strong> {{ item | prettyCRON }}<br/>
										</template>
										<template v-else>
											<strong>{{ index }}:</strong> {{ item }}<br/>
										</template>
									</li>
								</ul>
							</div>
							<div class="col right">
								<span>Storage</span>
								<span>Type: {{ conf.data.spec.storage.type }}</span>
								<ul class="params">
									<li v-for="(item, index) in conf.data.spec.storage[conf.data.spec.storage.type]">
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
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			
		}
	},
	computed: {

		config () {
			return store.state.backupConfig
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	}
})
