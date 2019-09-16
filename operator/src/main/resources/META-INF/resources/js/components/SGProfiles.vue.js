var SGProfiles = Vue.component("sg-profile", {
	template: `
		<div id="sg-profile">
			<header>
				<h2 class="title">PostgreSQL instance profiles</h2>
				<h3 class="subtitle">K8S Cluster: 255.255.255.255</h3>
			</header>

			<div class="content">
				<div class="profiles boxes">
					<div v-for="prof in profiles" class="box" v-bind:class="{'show':($route.params.name == prof.name)}">
						<h4>{{ prof.name }}</h4>
						<span>Namespace</span>
						{{ prof.data.metadata.namespace }}
						<hr>
						<span>RAM</span>
						{{ prof.data.spec.memory }}
						<hr>
						<span>CPU</span>
						{{ prof.data.spec.cpu }}
					</div>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			profiles: []
		}
	},
	created: function() {

		let vm = this;

		vm.profiles = profiles;

		/*profiles.forEach(function(item, index){

			var config = { 
				"name": item.metadata.name,
			    "data": item 
			};

			vm.config.push(config);
		});


		console.log(vm.config);*/

	},
	/*created: function() {

		let vm = this;
		
		if (typeof(this.$route.params.name) !== 'undefined'){ // If filtered by name
			var name = this.$route.params.name;

			for(let i=0; i < pgconf.length; i++ ){

				if(pgconf[i].metadata.name == name) {
					var config = { 
						"name": name,
					    "data": pgconf[i]
					};

					vm.config.push(config);
					break;					
				}
			}			
		} else { // If listing every conf
			pgconf.forEach(function(item, index){

				var config = { 
					"name": item.metadata.name,
				    "data": item 
				};

				vm.config.push(config);
			});
		}

	},
	beforeRouteUpdate (to, from, next) {

		if((to.params.name) !== 'undefined'){
			var config = { 
				"name": to.params.name,
			    "data": pgconf[to.params.name] 
			};


			this.config.push(config);
			console.log("pushed");
			console.log(this.config);
		}
		else {
			pgconf.forEach(function(item, index){

				var config = { 
					"name": item.metadata.name,
				    "data": item 
				};

				this.config.push(config);
			});
		}
		
		next();
	}*/
})
