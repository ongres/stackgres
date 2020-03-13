var Backups = Vue.component("sg-backup", {
	template: `
		<div id="sg-backup">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
						<template v-if="$route.params.name !== undefined">
							{{ $route.params.name }}
						</template>
						<template v-else>
							Cluster Backups
						</template>
					</li>
				</ul>
				<router-link :to="'/crd/create/backup/'+currentNamespace" class="btn">Create New Backup</router-link>
			</header>

			<div class="content">
				<div id="backups">
					<div class="toolbar">
						<input id="keyword" v-model="keyword" @keyup="filterTable" class="search" placeholder="Search Backup...">

						<div class="filter">
							<span class="toggle">FILTER</span>

							<ul class="options">
								<li>
									<span>Permanent</span>
									<label for="isPermanent"><input v-model="isPermanent" type="checkbox" id="isPermanent" name="isPermanent" value="true" @change="filterTable"/>YES</label>
									<label for="notPermanent"><input v-model="isPermanent" type="checkbox" id="notPermanent" name="isPermanent" value="false"  @change="filterTable"/>NO</label>
								</li>

								<li>
									<span>Phase</span>
									<label for="isCompleted"><input v-model="phase" type="checkbox" id="isCompleted" name="phase" value="Completed" @change="filterTable"/>Completed</label>
									<label for="notCompleted"><input v-model="phase" type="checkbox" id="notCompleted" name="phase" value="Pending" @change="filterTable"/>Pending</label>
								</li>

								<li>
									<span>Postgres Version</span>
									<label for="pg11"><input v-model="pgVersion" type="checkbox" id="pg11" name="pg11" value="pg11" @change="filterTable" />11</label>
									<label for="pg12"><input v-model="pgVersion" type="checkbox" id="pg12" name="pg12" value="pg12" @change="filterTable" />12</label>
								</li>
								
								<li>
									<span>Tested</span>
									<label for="isTested"><input v-model="tested" type="checkbox" id="isTested" name="tested" value="true" @change="filterTable" />YES</label>
									<label for="notTested"><input v-model="tested" type="checkbox" id="notTested" name="tested" value="false" @change="filterTable" />NO</label>
								</li>

								<li>
									<span>Cluster</span>
									<select v-model="clusterName" @change="filterTable">
										<option disabled value="">Select Cluster...</option>
										<option value="">All Clusters</option>
										<template v-for="cluster in allClusters">
											<option v-if="cluster.data.metadata.namespace == currentNamespace">{{ cluster.data.metadata.name }}</option>
										</template>
									</select>
								</li>
							</ul>
						</div>
					</div>
					<table>
						<thead class="sort">
							<th @click="sort('data.status.time')" class="sorted desc timestamp">
								<span>Timestamp</span>
							</th>
							<th @click="sort('data.spec.isPermanent')" class="icon desc isPermanent">
								<span>Permanent</span>
							</th>
							<th @click="sort('data.status.uncompressedSize')" class="desc phase">
								<span>Phase</span>
							</th>
							<th @click="sort('data.status.uncompressedSize')" class="desc size">
								<span>Size</span>
							</th>
							<th @click="sort('data.status.pgVersion')" class="desc pgVersion">
								<span>PG</span>
							</th>
							<th @click="sort('data.status.tested')" class="icon desc tested">
								<span>Tested</span>
							</th>
							<th @click="sort('data.metadata.name')" class="desc name">
								<span>Name</span>
							</th>
							<th @click="sort('data.spec.cluster')" class="desc clusterName">
								<span>Cluster Name</span>
							</th>
							<th class="actions"></th>
							<!--<th class="details"></th>-->
						</thead>
						<tbody>
							<template v-for="back in backups"  v-if="(back.data.metadata.namespace == currentNamespace)">
								<tr class="base" :class="back.data.status.phase">
										<td class="timestamp">{{ back.data.status.time }}</td>
										<td class="isPermanent icon" :class="[(back.data.spec.isPermanent) ? 'true' : 'false']"></td>
										<td class="phase" :class="back.data.status.phase">{{ back.data.status.phase }}</td>
										<td class="size">
											<template v-if="back.data.status.phase === 'Completed'">
												{{ back.data.status.uncompressedSize | formatBytes }}
											</template>
										</td>
										<td class="pgVersion" :class="[(back.data.status.phase === 'Completed') ? 'pg'+(back.data.status.pgVersion.substr(0,2)) : '']">
											<template v-if="back.data.status.phase === 'Completed'">
												{{ back.data.status.pgVersion | prefix }}
											</template>											
										</td>
										<td class="tested icon" :class="[(back.data.status.tested) ? 'true' : 'false']"></td>
										<td class="name">{{ back.data.metadata.name }}</td>
										<td class="clusterName" :class="back.data.spec.cluster">{{ back.data.spec.cluster }}</td>
									<td class="actions">
										<router-link :to="'/crd/edit/backup/'+$route.params.namespace+'/'+back.data.spec.cluster+'/'+back.name">Edit</router-link> <a v-on:click="deleteBackup(back.name, back.data.metadata.namespace)" class="delete">Delete</a>
									</td>
								</tr>
								<tr class="details" v-if="back.data.status.phase === 'Completed'">
									<td colspan="8">
										<h4 class="basic">Backup Details</h4>

										<hr>
										<span>UID</span>
										{{ back.data.metadata.uid }}

										<hr>
										<span>Pod</span>
										{{ back.data.status.pod }}

										<hr>
										<span>Name</span>
										{{ back.data.status.name }}

										<hr>
										<span>WAL File Name</span>
										{{ back.data.status.walFileName }}

										<hr>
										<span>Start Time</span>
										{{ back.data.status.startTime }}

										<hr>
										<span>Finish Time</span>
										{{ back.data.status.finishTime }}
										
										<hr>
										<span>Hostname</span>
										{{ back.data.status.hostname }}

										<hr>
										<span>Data Directory</span>
										{{ back.data.status.dataDir }}
										
										<hr>
										<span>Start Lsn</span>
										{{ back.data.status.startLsn }}

										<hr>
										<span>Finish Lsn</span>
										{{ back.data.status.finishLsn }}

										<hr>
										<span>System Identifier</span>
										{{ back.data.status.systemIdentifier }}

										<hr>
										<span>Compressed size</span>
										{{ back.data.status.compressedSize | formatBytes }}

										<hr>
										<h4 class="basic">Backup Configuration</h4>

										<hr>
										<span>Type</span>
										{{ back.data.status.backupConfig.storage.type }}

										<hr>
										<span>Compression Method</span>
										{{ back.data.status.backupConfig.compressionMethod }}
									</td>
								</tr>
							</template>
						</tbody>
					</table>
				</div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.status.time',
			currentSortDir: 'desc',
			clusterName: '',
			keyword: '',
			isPermanent: [],
			phase: [],
			pgVersion: [],
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
		}

	},
	methods: {
		
		sort: function(s) {
			
			//if s == current sort, reverse
			if(s === this.currentSort) {
			  this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
			}
			this.currentSort = s;

		},

		deleteBackup: function(backupName, backupNamespace) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const backup = {
					name: backupName,
					namespace: backupNamespace
				}
	
				const res = axios
				.delete(
					apiURL+'backup/', 
					{
						data: {
							"metadata": {
								"name": backup.name,
								"namespace": backup.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Backup <strong>'+vm.$route.params.name+'</strong> deleted successfully', 'message');
					vm.fetchAPI();
					router.push('/backups/'+store.state.currentNamespace);                        
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		},

		filterTable: function() {

			let bk = this;

			$("table tr.base").each(function () {

				let show = true;
				let r = $(this);
				let checkFilters = ['isPermanent', 'phase', 'pgVersion', 'tested'];

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
				if(bk.isPermanent.length && (!r.children(".isPermanent."+bk.isPermanent).length))
					show = false;

				//Filter by phase
				if(bk.phase.length && (!r.children(".phase."+bk.phase).length))
					show = false;

				//Filter by pgVersion
				if(bk.pgVersion.length){

					let hasClass = 0;
					
					bk.pgVersion.forEach(function(item){
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

			});
			
		}
	}
})
