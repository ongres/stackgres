var PoolConfig = Vue.component("pool-config", {
	template: `
		<div id="pool-config">
			<header>
				<h2 class="title">POSTGRESQL CONNECTION POOLING</h2>
				<h3 class="subtitle">K8S Cluster: 255.255.255.255</h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" class="box" v-bind:class="{'show':($route.params.name == conf.name)}">
						<h4>{{ conf.name }}</h4>
						<span>Namespace</span>
						<strong>{{ conf.data.metadata.namespace }}</strong>
						<hr>
						<span>Params</span>
						<ul class="params">
							<li v-for="(item, index) in conf.data.spec.pgbouncerConf">
								<strong>{{ index }}:</strong> {{ item }}<br/>
							</li>
						</ul>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			config: [],
		}
	},
	created: function() {

		let vm = this;
		
		/*if (typeof(this.$route.params.name) !== 'undefined'){ // If filtered by name
			var name = this.$route.params.name;

			for(let i=0; i < poolConf.length; i++ ){

				if(poolConf[i].data.metadata.name == name) {
					var config = { 
						"name": name,
					    "data": poolConf[i].data
					};

					vm.config.push(config);
					break;					
				}
			}

			vm.show = 'show';

			console.log(vm.config);			
		} else { // If listing every conf
			let vm = this;*/
			vm.config = poolConf;
			
		//}

	},
	/*beforeRouteUpdate (to, from, next) {

		if (to.params.name !== 'undefined'){ // If filtered by name
			var name = this.$route.params.name;

			for(let i=0; i < poolConf.length; i++ ){

				if(poolConf[i].data.metadata.name == name) {
					var config = { 
						"name": name,
					    "data": poolConf[i].data
					};

					vm.config.push(config);
					break;					
				}
			}

			vm.show = 'show';

		} else { // If listing every conf
			let vm = this;
			vm.config = poolConf;
			vm.show = '';
		}
		
		next();
	}*/
})
