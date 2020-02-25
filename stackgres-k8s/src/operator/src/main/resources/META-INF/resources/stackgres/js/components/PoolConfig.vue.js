var PoolConfig = Vue.component("pool-config", {
	template: `
		<div id="pool-config">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						{{ $route.params.namespace }}
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
						<template v-if="$route.params.name !== undefined">
							{{ $route.params.name }}
						</template>
						<template v-else>
							connection pooling configurations
						</template>
					</li>
				</ul>
				<router-link to="/crd/create/poolconfig/" class="btn">Create New Configuration</router-link>
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
					
					notify('Configuration <strong>"'+configName+'"</strong> deleted successfully', 'message');
					$('#'+configName+'-'+configNamespace).addClass("deleted");
					
					vm.fetchAPI();
					router.push('/configurations/connectionpooling/'+store.state.currentNamespace);
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	
	}
})
