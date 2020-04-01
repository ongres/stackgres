var ClusterOverview = Vue.component("cluster-overview", {
	template: `
		<div id="cluster-overview">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						StackGres Clusters
					</li>
				</ul>
				<router-link to="/crd/create/cluster/" class="add">Add New</router-link>
			</header>

			<div class="content">
				<table class="clusterOverview">
					<thead>
						<th>
							<span>StackGres Cluster</span>
						</th>

						<th>
							<span>Instances</span>
						</th>

						<th>
							<span>CPU</span>
						</th>

						<th>
							<span>Memory</span>
						</th>

						<th>
							<span>Disk</span>
						</th>

						<th>
							<span>Health</span>
						</th>
						
						<th class="actions"></th>
					</thead>
					<tbody>
						<tr class="no-results">
							<td colspan="7">
								No clusters have been found, would you like to <router-link to="/crd/create/cluster/" title="Add New Cluster">create a new one?</router-link>
							</td>
						</tr>
						<template v-for="cluster in clusters">
							<tr v-if="cluster.data.metadata.namespace == currentNamespace" :class="'cluster-'+cluster.data.metadata.namespace+'-'+cluster.name" >
								<td class="clusterName">
									<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
										{{ cluster.name }}
									</router-link>
								</td>
								<td class="instances">
									<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
										{{ cluster.data.spec.instances }}
									</router-link>
								</td>
								
								<template v-for="profile in profiles">
									<template v-if="(profile.data.metadata.namespace == currentNamespace) && (cluster.data.spec.sgInstanceProfile == profile.name)">
										<td class="cpu">
											<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
												{{ profile.data.spec.cpu }}
											</router-link>
										</td>
										<td class="ram">
											<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
												{{ profile.data.spec.memory }}
											</router-link>
										</td>
									</template>
								</template>

								<td class="volumeSize">
									<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
										{{ cluster.data.spec.pods.persistentVolume.size }}
									</router-link>
								</td>
								<td class="health">
									<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu" class="no-color">
										{{ cluster.data.podsReady }} / {{ cluster.data.spec.instances }}
									</router-link>
								</td>
								<td class="actions">
									<router-link :to="'/cluster/status/'+currentNamespace+'/'+cluster.name" title="Cluster Status" data-active=".set.clu"><svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg></router-link>
									<router-link :to="'/crd/edit/cluster/'+currentNamespace+'/'+cluster.name" title="Edit Cluster" data-active=".set.clu"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg></router-link>
									<a @click="deleteCRD('cluster', currentNamespace, cluster.name)" title="Delete Cluster"><svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg></a>
								</td>
							</tr>
						</template>
					</tbody>
				</table>
			</div>
		</div>`,
	data: function() {
		
		return {
	    }
	},
	computed: {
		clusters () {
			return store.state.clusters
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		profiles () {
			return store.state.profiles
		}
	},
	methods: {
		deleteCluster: function(clusterName) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const cl = {
					name: clusterName,
					namespace: store.state.currentNamespace
				}
	
				const res = axios
				.delete(
					apiURL+'sgcluster/', 
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
					notify('Cluster <strong>'+clusterName+'</strong> deleted successfully', 'message', 'cluster');
					vm.fetchAPI();
					//router.push('/overview/'+store.state.currentNamespace);                        
				})
				.catch(function (error) {
					console.log(error);
					notify(error.response.data,'error','cluster');
				});
			}

		}	
	}
})
