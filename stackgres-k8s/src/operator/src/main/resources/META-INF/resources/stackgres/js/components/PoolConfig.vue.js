var PoolConfig = Vue.component("pool-config", {
	template: `
		<div id="pool-config">
			<header>
				<h2 class="title">POSTGRESQL CONNECTION POOLING</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" class="box" v-bind:class="{'show':($route.params.name == conf.name)}" v-if="conf.data.metadata.namespace == currentNamespace">
						<h4>{{ conf.name }}</h4>
						<span>Namespace</span>
						<strong>{{ conf.data.metadata.namespace }}</strong>
						<hr>
						<span>Params</span>
						<ul class="params">
							<li v-for="(item, index) in conf.data.spec['pgbouncer.ini']">
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
			return store.state.poolConfig
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	}
})
