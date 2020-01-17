var Backups = Vue.component("sg-backup", {
	template: `
		<div id="sg-backup">
			<header>
				<h2 class="title">PostgreSQL backups</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="backups boxes">
					<div v-for="back in backups" class="box" v-bind:class="{'show':($route.params.name == back.name)}" v-if="back.data.metadata.namespace == currentNamespace">
						<h4>{{ back.name }}</h4>
						<span>Namespace</span>
						{{ back.data.metadata.namespace }}
						<hr>
						<span>Cluster</span>
						{{ back.data.spec.cluster }}
						<hr>
						<span>Permanent</span>
						{{ back.data.spec.isPermanent }}
						<div v-if="!back.data.status || back.data.status.phase === 'Pending'">
						<hr>
						<span>Phase</span>
						Pending
						</div>
						<div v-else-if="back.data.status.phase === 'Failed'">
						<hr>
						<span>Phase</span>
						Failed
						<hr>
						<span>Reason</span>
						{{ back.data.status.failureReason }}
						</div>
						<div v-else-if="back.data.status.phase === 'Completed'">
						<hr>
						<span>Phase</span>
						Completed
						<hr>
						<span>Timestamp</span>
						{{ back.data.status.time }}
						<hr>
						<span>Compressed size</span>
						{{ back.data.status.compressedSize }}
						<hr>
						<span>Uncompressed size</span>
						{{ back.data.status.uncompressedSize }}
						<hr>
						<span>PostgreSQL version</span>
						{{ back.data.status.pgVersion }}
						<hr>
						<div v-if="back.data.status.tested">
						<span>Tested</span>
						true
						</div>
						<div v-else-if="!back.data.status.tested">
						<span>Tested</span>
						false
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

		backups () {
			return store.state.backups
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	}
})
