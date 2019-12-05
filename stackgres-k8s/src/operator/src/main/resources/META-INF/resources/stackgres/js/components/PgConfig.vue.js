var PgConfig = Vue.component("pg-config", {
	template: `
		<div id="pg-config">
			<header>
				<h2 class="title">POSTGRESQL CONFIGURATIONS</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" class="box" v-bind:class="{'show':($route.params.name == conf.name)}" v-if="conf.data.metadata.namespace == currentNamespace">
						<h4>{{ conf.name }}</h4>
						<span>Configuration Namespace</span>
						{{ conf.data.metadata.namespace }}
						<hr>
						<span>PostgreSQL Version</span>
						{{ conf.data.spec.pg_version }}
						<hr>
						<span>Params</span>
						<ul class="params">
							<li v-for="(item, index) in conf.data.spec['postgresql.conf']">
								<strong>{{ index }}:</strong> {{ item }}<br/>
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

		config () {
			return store.state.pgConfig
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	}
})
