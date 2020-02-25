var ClusterInfo = Vue.component("cluster-info", {
	template: `
		<div id="cluster-info">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					{{ $route.params.namespace }}
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
					{{ $route.params.name }}
				</li>
			</ul>

			<ul class="tabs">
				<li>
					<router-link :to="'/information/'+$route.params.namespace+'/'+$route.params.name" title="Information" class="info">Information</router-link>
				</li>

				<li>
					<router-link :to="'/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
				</li>

				<li>
					<router-link id="grafana-btn" :to="'/grafana/'+$route.params.name" title="Grafana Dashboard" class="grafana" style="display:none;">Monitoring</router-link>
				</li>
			</ul>
		</header>

			<div class="content">
				<div class="table">
					<div class="head row">
						<div class="col">
							<h4>CPU</h4>
						</div>

						<div class="col">
							<h4>Memory</h4>
						</div>

						<div class="col">
							<h4>Disk</h4>
						</div>

						<div class="col">
							<h4>Health</h4>
						</div>
					</div>
					<div class="row" v-if="allDataReady">
						<div class="col">
							{{ cluster.data.cpuRequested }} (avg. load {{ cluster.data.averageLoad1m }})
						</div>
						<div class="col">
							{{ cluster.data.memoryRequested }}
						</div>
						<div class="col">
							{{ cluster.data.diskUsed }} / {{ cluster.data.diskFound }}
						</div>
						<div class="col">
							{{ pods.data.podsReady }} / {{ pods.data.pods.length }}
						</div>
					</div>
				</div>

				<div class="form">
					<router-link :to="'/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name" class="btn">Edit Cluster</router-link> <button v-on:click="deleteCluster" class="border">Delete Cluster</button>
				</div>
			</div>
		</div>`,
	data: function() {
		return {
	      dataReady: [ false, false ],
	      allDataReady: false,
		  polling: null,
		  name: '',
		  namespace: ''
	    }
	},
	methods: {
		
		fetchAPI: function() {
			vc = this;

			/*store.commit('setCurrentCluster', vm.$route.params.name);
			console.log("Current cluster: "+store.state.currentCluster)*/

			/* Clusters Data */
		    axios
		    .get(apiURL+'cluster/status/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
		    	{ headers: {
		            'content-type': 'application/json'
		          }
		        }
	      	)
	      	.then( function(response){

				const c = store.state.clusters.find(function(e){
					return e.name == vm.$route.params.name
				});

	        	store.commit('setCurrentCluster', { 
	              	name: vm.$route.params.name,
					data: response.data,
					spec: c.data.spec,
					metadata: c.data.metadata
					
              	});

	        	vc.dataReady[0] = true;
	        	vc.allDataReady = vc.dataReady[0] && vc.dataReady[1];
	      	});

			/* Pods Data */
		    axios
		    .get(apiURL+'cluster/pods/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
		    	{ headers: {
		            'content-type': 'application/json'
		          }
		        }
	      	)
	      	.then( function(response){

	        	store.commit('setCurrentPods', { 
	              	name: vm.$route.params.name,
	              	data: response.data
              	});

	        	vc.dataReady[1] = true;
	        	vc.allDataReady = vc.dataReady[0] && vc.dataReady[1];
	      	});
		},

		deleteCluster: function(e) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const cl = {
					name: this.name,
					namespace: this.namespace
				}
	
				const res = axios
				.delete(
					apiURL+'cluster/', 
					{
						data: {
							"metadata": {
								"name": cl.name,
								"namespace": cl.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Cluster <strong>'+vm.$route.params.name+'</strong> deleted successfully', 'message');
					vm.fetchAPI();
					router.push('/overview/'+store.state.currentNamespace);                        
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	

	},
	created: function() {

		if ( (store.state.currentCluster.length > 0) && (store.state.currentCluster.name == vm.$route.params.name) ) {
			this.dataReady = true;
		}

		this.name = vm.$route.params.name;
		this.namespace = vm.$route.params.namespace;
		
	},
	mounted: function() {

		var count = 0;

		this.fetchAPI();
	    
	    this.polling = setInterval( function(){
	    	//count++;
	      	this.fetchAPI();

	      	//console.log("Interval run #"+count);

	    }.bind(this), 5000);
		
		$(".set.clu").addClass("active");
	},
	computed: {

		cluster () {
			//console.log(store.state.currentCluster);
			return store.state.currentCluster
		},
		pods () {
			//console.log(store.state.currentPods);
			return store.state.currentPods
		}
	},
	beforeDestroy () {
		clearInterval(this.polling);
		//console.log('Interval cleared');
	} 
})