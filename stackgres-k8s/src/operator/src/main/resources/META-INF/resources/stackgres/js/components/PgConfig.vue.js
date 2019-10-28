var PgConfig = Vue.component("pg-config", {
	template: `
		<div id="pg-config">
			<header>
				<h2 class="title">POSTGRESQL CONFIGURATIONS</h2>
				<h3 class="subtitle">K8S Cluster: {{ serverIP }}</h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" class="box" v-bind:class="{'show':($route.params.name == conf.name)}">
						<h4>{{ conf.name }}</h4>
						<span>Configuration Namespace</span>
						{{ conf.data.metadata.namespace }}
						<hr>
						<span>PostgreSQL Version</span>
						{{ conf.data.spec.pgVersion }}
						<hr>
						<span>Params</span>
						<ul class="params">
							<li v-for="(item, index) in conf.data.spec.postgresqlConf">
								<strong>{{ index }}:</strong> {{ item }}<br/>
							</li>
						</ul>
					</div>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			config: []		
		}
	},
	/*created: function() {

		let vm = this;
		vm.config = pgConf;

	},*/
	created: function() {

		let vm = this;
		
		/*if (typeof(this.$route.params.name) !== 'undefined'){ // If filtered by name
			var name = this.$route.params.name;

			console.log("Change to: "+name);

			for(let i=0; i < pgConf.length; i++ ){

				if(pgConf[i].data.metadata.name == name) {
					var config = { 
						"name": name,
					    "data": pgConf[i].data
					};

					vm.config.push(config);
					break;					
				}
			}

			vm.show = 'show';

			console.log(vm.config);			
		} else { // If listing every conf*/
			
			vm.config = pgConf;

		//}

	},
	/*beforeRouteUpdate (to, from, next) {

		let vm = this;

		if (to.params.name !== 'undefined'){ // If filtered by name
			var name = this.$route.params.name;

			console.log("Change to: "+name);

			for(let i=0; i < pgConf.length; i++ ){

				if(pgConf[i].data.metadata.name == name) {
					var config = { 
						"name": name,
					    "data": pgConf[i].data
					};

					vm.config.push(config);
					break;					
				}
			}

			vm.show = 'show';

		} else { // If listing every conf
			let vm = this;
			vm.config = pgConf;
			vm.show = '';

			console.log("Enter BEFORE");
		}
		
		next();
	}*/
})
