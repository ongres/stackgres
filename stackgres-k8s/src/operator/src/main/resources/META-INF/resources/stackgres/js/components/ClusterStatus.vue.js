var ClusterStatus = Vue.component("cluster-status", {
	template: `
		<div id="cluster-status">
			<header>
				<h2 class="title">Cluster Status</h2>
				<h3 class="subtitle">{{ $route.params.name }}</h3>
			</header>

			<div class="content">
				<div class="table" v-if="allDataReady">
					<div class="head row">
						<div class="col text">
							<h4>Pod Name</h4>
						</div>

						<div class="col status">
							<h4>Status</h4>
						</div>

						<!--<div class="col text">
							<h4>View Report</h4>
						</div>-->

						<div class="col">
							<h4>Containers</h4>
						</div>
					</div>
					<div v-for="pod in pods.data.pods" class="row">
						<div class="col text">
							{{ pod.name }}
						</div>
						<div :class="'col status '+pod.status.toLowerCase()">
							<span>{{ pod.status.charAt(0) }}</span> {{ pod.role }}
						</div>
						<!--<div class="col link">
							{{ pod.ip }}:{{ pod.port }}
						</div>-->
						<div class="col">
							{{ pod.containersReady }} / {{ pod.containers }}
						</div>
					</div>
				</div>
			</div>
			<ul class="status-legend">
				STATUS LEGEND:

				<li class="status running">
					<span>R</span> Running Pod
				</li>

				<li class="status pending">
					<span>P</span> Pending Pod
				</li>

				<li class="status failed">
					<span>F</span> Failed Pod
				</li>
			</ul>

			<div class="form">
				<button @click="deleteCluster" class="border">Delete Cluster</button>
			</div>
		</div>`,
	data: function() {
		return {
	      dataReady: [ false, false ],
	      allDataReady: false,
	      polling: null
	    }
	},
	methods: {
		
		fetchAPI: function() {
			vc = this;

			/*store.commit('setCurrentPods', vm.$route.params.name);
			console.log("Current pods: "+store.state.currentPods)*/

			/* Clusters Data */
		    axios
		    .get(apiURL+'cluster/status/'+vm.$route.params.namespace+'/'+vm.$route.params.name,
		    	{ headers: {
		            'content-type': 'application/json'
		          }
		        }
	      	)
	      	.then( function(response){

	        	store.commit('setCurrentCluster', { 
	              	name: vm.$route.params.name,
	              	data: response.data
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
			e.preventDefault();

			let confirmDelete = confirm("DELETE CLUSTER\nAre you sure you want to delete this item?")

			if(confirmDelete) {

				const cl = {
					name: vm.$route.params.name,
					namespace: vm.$route.params.namespace
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
					notify('Cluster <strong>"'+vm.$route.params.name+'"</strong> deleted successfully', 'message');
					$('#'+cl.name+'-'+cl.namespace).addClass("deleted");
					router.push('/overview/'+store.state.currentNamespace);
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	

	},
	mounted: function() {

		var count = 0;

		if (store.state.currentPods.length === 0) {
			this.fetchAPI();
			this.dataReady = false;
		}

	    this.polling = setInterval( function(){
	    	//count++;
	      	this.fetchAPI();

	      	//console.log("Interval run #"+count);

	    }.bind(this), 5000);

	    //$('.clu .'+vm.$route.params.name).addClass("active");
	    
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
