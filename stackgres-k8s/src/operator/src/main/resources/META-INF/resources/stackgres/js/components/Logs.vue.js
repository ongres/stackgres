var Logs = Vue.component("sg-logs", {
	template: `
		<div id="sg-logs">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>
						Distributed Logs
					</li>
					<li>
						{{ $route.params.name }}
					</li>
				</ul>

				<div class="actions">
					<router-link :to="'/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link> <a v-on:click="deleteCRD('sgcluster', currentNamespace, currentCluster.name, '/overview/'+currentNamespace)" :class="'/overview/'+currentNamespace">Delete Cluster</a>
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="currentCluster.hasBackups">
						<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
					</li>
					<li>
						<router-link :to="'/cluster/logs/'+$route.params.namespace+'/'+$route.params.name" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="currentCluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>

			<div class="content">
				<div id="log">
					<div class="toolbar">
						<input id="keyword" v-model="keyword" @keyup="filterTable" class="search" placeholder="Search Backup...">

						<div class="filter">
							<span class="toggle">FILTER</span>
						</div>

						<div class=calendar>
							<ul>
								<li><span class="shortcut">1 m</span></li>
								<li><span class="shortcut">30 m</span></li>
								<li><span class="shortcut">3 h</span></li>
								<li><span class="shortcut">1 d</span></li>
							</ul>
						</div>

						<div class=visibleColumns>
							<span class="toggle">VISIBLE COLUMNS</span>
						</div>
					</div>

					<table class="logs">
						<thead>
							<th class="timestamp logTime">Log Time</th>
							<th class="logType center">Type</th>
							<th class="podName">Pod</th>
							<th class="role center">Role</th>
							<th class="userName">User</th>
							<th class="databaseName">Database</th>
							<th class="processId">Process ID</th>
							<th class="connectionFrom">Connection From</th>
							<th class="errorSeverity center">Error Severity</th>
							<th class="applicationName">Application</th>
							<th class="messsage">Message</th>
							<th class="actions"></th>
						</thead>
						<tbody>
							<template v-for="log in logs">
								<tr v-if="log.logType === 'pg'" :class="($route.params.name == log.name) ? 'open' : ''">
									<td class="timestamp logTime">
										<span class='date'>
											{{ log.logTime | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ log.logTime | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ log.logTime | formatTimestamp('ms') }}
										</span>
										Z
									</td>
									<td class="logType label postgres center">
										<span>Postgres</span>
									</td>
									<td class="podName">{{ log.podName }}</td>
									<td class="role label center" :class="log.role">
										<span v-if="log.role == 'pr'">Primary</span>
										<span v-else="log.role == 're'">Replica</span>
									</td>
									<td class="userName">{{ log.userName }}</td>
									<td class="databaseName">{{ log.databaseName }}</td>
									<td class="processId">{{ log.processId }}</td>
									<td class="connectionFrom">{{ log.connectionFrom }}</td>
									<td class="errorSeverity label center" :class="log.errorSeverity">{{ log.errorSeverity }}</td>
									<td class="applicationName">{{ log.applicationName }}</td>
									<td class="messsage">{{ log.messsage }}</td>
								</tr>
								<tr v-else>
									<td class="timestamp logTime">
										<span class='date'>
											{{ log.logTime | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ log.logTime | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ log.logTime | formatTimestamp('ms') }}
										</span>
										Z
									</td>
									<td class="logType label patroni center">
										<span>Patroni</span>
									</td>
									<td class="podName">{{ log.podName }}</td>
									<td class="role label center" :class="log.role">
										<span v-if="log.role == 'pr'">Primary</span>
										<span v-else="log.role == 're'">Replica</span>
									</td>
									<td class="userName"></td>
									<td class="databaseName"></td>
									<td class="processId"></td>
									<td class="connectionFrom"></td>
									<td class="errorSeverity label center" :class="log.errorSeverity">{{ log.errorSeverity }}</td>
									<td class="applicationName"></td>
									<td class="messsage">{{ log.message }}</td>
								</tr>
							</template>
						</tbody>
					</table>

					
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.status.process.timing.stored',
			currentSortDir: 'desc',
			clusterName: '',
			keyword: '',
			isPermanent: [],
			phase: [],
			postgresVersion: [],
			tested: [],
		}
	},
	computed: {

		backups () {
			//return store.state.backups
			return sortTable( store.state.backups, this.currentSort, this.currentSortDir )
		},

		currentNamespace () {
			return store.state.currentNamespace
		},

		allClusters () {
			return store.state.clusters
		},

		currentCluster () {
			return store.state.currentCluster
		},

		isCluster() {
			return  vm.$route.params.cluster !== undefined
        },
        
        logs() {

			if(!store.state.logs.length) {

				/* Mock Data */
				/* axios
				.get('js/data/logs')
				.then( function(response){
					store.commit('setLogs', response.data)
				}).catch(function(err) {
					console.log(err);
				}); */

				/* API Data */
				/* axios
				.get(apiURL+'sgcluster/logs/'+store.state.currentNamespace+'/'+store.state.currentCluster.name)
				.then( function(response){
		
					store.commit('setLogs', response.data)
				  
				}).catch(function(err) {
				  console.log(err);
				}); */

			}
			//vm.fetchAPI('logs');
			return store.state.logs

        }

	},
	mounted: function() {

		axios
		.get(apiURL+'sgcluster/logs/'+store.state.currentNamespace+'/'+store.state.currentCluster.name+'?records=10')
		.then( function(response){
			store.commit('setLogs', response.data)
			console.log(response.data);
		}).catch(function(err) {
			store.commit('setLogs', [])
		});


	},
	methods: {

		filterTable: function() {

			let bk = this;

			$("table tr.base").each(function () {

				let show = true;
				let r = $(this);
				let checkFilters = ['isPermanent', 'phase', 'postgresVersion']; // 'tested' is out for now

				// Filter by Keyword
				if(bk.keyword.length && (r.text().toLowerCase().indexOf(bk.keyword.toLowerCase()) === -1) )
					show = false;

				checkFilters.forEach(function(f){

					if(bk[f].length){
						let hasClass = 0;

						bk[f].forEach(function(c){
							if(r.children('.'+c).length)
								hasClass++;
						});

						if(!hasClass)
							show = false;
					}
					
				})

				/* //Filter by isPermanent
				if(bk.managedLifecycle.length && (!r.children(".managedLifecycle."+bk.managedLifecycle).length))
					show = false;

				//Filter by phase
				if(bk.phase.length && (!r.children(".phase."+bk.phase).length))
					show = false;

				//Filter by postgresVersion
				if(bk.postgresVersion.length){

					let hasClass = 0;
					
					bk.postgresVersion.forEach(function(item){
						if(r.children('.'+item).length)
							hasClass++;
					});

					if(hasClass < 1)
						show = false;

				}

				//Filter by tested
				if(bk.tested.length && (!r.children(".tested."+bk.tested).length))
					show = false; */

				//Filter by clusterName
				if(bk.clusterName.length && (!r.children(".clusterName."+bk.clusterName).length))
					show = false;

				if(!show)
					r.addClass("not-found");
				else
					r.removeClass("not-found");
				
				if(!$("tr.base:not(.not-found)").length)
					$("tr.no-results").show();
				else
					$("tr.no-results").hide();

			});
			
		}
	}
})
