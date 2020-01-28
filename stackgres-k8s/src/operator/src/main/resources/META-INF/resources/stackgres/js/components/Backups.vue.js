var Backups = Vue.component("sg-backup", {
	template: `
		<div id="sg-backup">
			<header>
				<h2 class="title">PostgreSQL Backups</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
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
										{{ back.data.status.uncompressedSize }}
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
						<h4>Backup Details</h4>

						<hr>
						<span>UID</span>
						{{ back.data.metadata.uid }}

						<hr>
						<span>Cluster</span>
						{{ back.data.spec.cluster }}

						<template v-if="back.data.status.phase === 'Failed'">
							<hr>
							<span>Phase</span>
							Failed
							<hr>
							<span>Reason</span>
							{{ back.data.status.failureReason }}
						</template>
						<template v-else-if="back.data.status.phase === 'Completed'">
							<hr>
							<span>Compressed size</span>
							{{ back.data.status.compressedSize }}
						</template>
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
