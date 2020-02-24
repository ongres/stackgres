var PoolConfig = Vue.component("pool-config", {
	template: `
		<div id="pool-config">
			<header>
				<h2 class="title">POSTGRESQL CONNECTION POOLING</h2>
				<h3 class="subtitle">Namespace: {{ currentNamespace }} </h3>
			</header>

			<div class="content">
				<div class="boxes">
					<div v-for="conf in config" v-bind:id="conf.name+'-'+conf.data.metadata.namespace" class="box config" v-bind:class="{'show':($route.params.name == conf.name)}" v-if="conf.data.metadata.namespace == currentNamespace">
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
						<div class="form">
							<router-link :to="'/crd/edit/poolconfig/'+$route.params.namespace+'/'+conf.name" class="btn">Edit Configuration</router-link> 
							<button @click="deleteConfig(conf.name, conf.data.metadata.namespace)" class="border">Delete Configuration</button>
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
			return store.state.poolConfig
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

	},
	methods: {
		deleteConfig: function(configName, configNamespace) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {

				const config = {
					name: configName,
					namespace: configNamespace
				}

				const res = axios
				.delete(
					apiURL+'connpoolconfig/', 
					{
						data: {
							"metadata": {
								"name": config.name,
								"namespace": config.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Configuration <strong>"'+configName+'"</strong> deleted successfully', 'message');
					$('#'+configName+'-'+configNamespace).addClass("deleted");
					//router.push('/overview/'+store.state.currentNamespace);
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	
	}
})
