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
						<input id="keyword" @keyup="filterText" class="search" placeholder="Search Backup...">

						<div class="filter">
							<span class="toggle">FILTER</span>

							<div class="options">
								<label for="permanent" data-filter="type">
									Permanent
										<input type="radio" name="type-true" value="true" />
										<input type="radio" name="type-true" value="false" />
								</label>
							</div>
						</div>
					</div>
					<table>
						<thead class="sort">
							<th @click="sort('data.status.time')" class="sorted desc" data-col="timestamp">
								<span>Timestamp</span>
							</th>
							<th @click="sort('data.spec.isPermanent')" class="icon desc" data-col="permanent">
								<span>Permanent</span>
							</th>
							<th @click="sort('data.status.uncompressedSize')" class="desc" data-col="phase">
								<span>Phase</span>
							</th>
							<th @click="sort('data.status.uncompressedSize')" class="desc" data-col="size">
								<span>Size</span>
							</th>
							<th @click="sort('data.status.pgVersion')" class="desc" data-col="pgVersion">
								<span>PG</span>
							</th>
							<th @click="sort('data.status.tested')" class="icon desc" data-col="tested">
								<span>Tested</span>
							</th>
							<th @click="sort('data.metadata.name')" class="desc" data-col="name">
								<span>Name</span>
							</th>
							<th @click="sort('data.spec.cluster')" class="desc" data-col="clusterName">
								<span>Cluster Name</span>
							</th>
							<th class="actions"></th>
							<!--<th class="details"></th>-->
						</thead>
						<tbody>
							<template v-for="back in backups"  v-if="back.data.metadata.namespace == currentNamespace">
								<tr class="base" :class="back.data.status.phase">
									<td data-col="timestamp">{{ back.data.status.time }}</td>
									<td data-col="permanent" class="icon">
										<template v-if="back.data.spec.isPermanent">
											✔
										</template>
										<template v-else>
											×
										</template>
									</td>
									<td data-col="phase">{{ back.data.status.phase }}</td>
									<td data-col="size">
										<template v-if="back.data.status.phase === 'Completed'">
											{{ back.data.status.uncompressedSize | formatBytes }}
										</template>
									</td>
									<td data-col="pgversion">
										<template v-if="back.data.status.phase === 'Completed'">
											{{ back.data.status.pgVersion | prefix }}
										</template>											
									</td>
									<td data-col="tested" class="icon">
										<template v-if="back.data.status.tested">
											✔
										</template>
										<template v-else>
											×
										</template>
									</td>
									<td data-col="name">{{ back.data.metadata.name }}</td>
									<td data-col="clusterName">{{ back.data.spec.cluster }}</td>
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
  			currentSortDir: 'desc'
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

		filterText: function() {
			var search = $("#keyword").val();
			console.log(search);

			if(search.length) {
				$("table tr").each(function () {

					if ( ($(this).text().toLowerCase().indexOf(search.toLowerCase()) !== -1) )
						$(this).removeClass("not-found");
					else
						$(this).addClass("not-found");

				});
			}
		}
	}
})
