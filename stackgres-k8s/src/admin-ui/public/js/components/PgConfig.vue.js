var PgConfig = Vue.component("PostgresConfig", {
	template: `
		<div id="pg-config">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z" class="a"></path></svg>
						<router-link :to="'/admin/configurations/postgres/'+currentNamespace" title="Postgres">SGPostgresConfigList</router-link>
					</li>
					<li v-if="typeof $route.params.name !== 'undefined'">
						{{ $route.params.name }}
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/02-configuration-tuning/02-postgres-configuration/" target="_blank" title="SGPostgresConfig Documentation">SGPostgresConfig Documentation</a>
					<div>
						<router-link v-if="iCan('create','sgpgconfigs',$route.params.namespace)" :to="'/admin/crd/create/pgconfig/'+$route.params.namespace" class="add">Add New</router-link>
					</div>
				</div>	
			</header>

			<div class="content">
				<table id="postgres" class="configurations pgConfig">
					<thead class="sort">
						<th @click="sort('data.metadata.name')" class="sorted desc name">
							<span>
								Name
								<span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'metadata.name')"></span>
							</span>
						</th>
						<th @click="sort('data.spec.postgresVersion')" class="desc postgresVersion">
							<span>
								PG
								<span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'spec.postgresVersion')"></span>
							</span>
						</th>
						<th class="config">
							<span>
								Parameters
								<span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'spec.postgresql.conf')"></span>
							</span>
						</th>
						<th class="actions"></th>
					</thead>
					<tbody>
						<tr class="no-results">
							<td :colspan="4" v-if="iCan('create','sgpgconfigs',$route.params.namespace)">
								No configurations have been found, would you like to <router-link :to="'/admin/crd/create/pgconfig/'+$route.params.namespace" title="Add New Postgres Configuration">create a new one?</router-link>
							</td>
							<td v-else colspan="4">
								No configurations have been found. You don't have enough permissions to create a new one
							</td>
						</tr>
						<template v-for="conf in config" v-if="(conf.data.metadata.namespace == currentNamespace)">
							<tr class="base" :class="[ $route.params.name == conf.name ? 'open' : '', 'sgpgconfig-'+conf.data.metadata.namespace+'-'+conf.name ]" :data-name="conf.name">
								<td class="hasTooltip configName">
									<span>{{ conf.name }}</span>
								</td>
								<td class="pgVersion">
									{{ conf.data.spec.postgresVersion }}
								</td>
								<td class="parameters">
									<ul class="yaml">
										<template v-for="param in conf.data.status['postgresql.conf']">
											<li>
												<strong class="label">{{ param.parameter }}:</strong> {{ param.value }}
											</li>
										</template>
									</ul>
								</td>
								<td class="actions">
									<router-link v-if="iCan('patch','sgpgconfigs',$route.params.namespace)" :to="'/admin/crd/edit/pgconfig/'+currentNamespace+'/'+conf.name" title="Edit Configuration">
										<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 14 14"><path d="M90,135.721v2.246a.345.345,0,0,0,.345.345h2.246a.691.691,0,0,0,.489-.2l8.042-8.041a.346.346,0,0,0,0-.489l-2.39-2.389a.345.345,0,0,0-.489,0L90.2,135.232A.691.691,0,0,0,90,135.721Zm13.772-8.265a.774.774,0,0,0,0-1.095h0l-1.82-1.82a.774.774,0,0,0-1.095,0h0l-1.175,1.176a.349.349,0,0,0,0,.495l2.421,2.421a.351.351,0,0,0,.5,0Z" transform="translate(-90 -124.313)"/></svg>
									</router-link>
									<a v-if="iCan('create','sgpgconfigs',$route.params.namespace)" v-on:click="cloneCRD('SGPostgresConfig', currentNamespace, conf.name)" class="cloneCRD" title="Clone Configuration"><svg xmlns="http://www.w3.org/2000/svg" width="13.9" height="16" viewBox="0 0 20 20"><g><path fill="#00ADB5" d="M2.5,20c-0.5,0-1-0.4-1-1V5c0-0.5,0.4-1,1-1c0.6,0,1,0.4,1,1v12.4c0,0.3,0.3,0.6,0.6,0.6h9.4c0.5,0,1,0.4,1,1c0,0.5-0.4,1-1,1H2.5z"/><path fill="#00ADB5" d="M6.5,16c-0.5,0-0.9-0.4-0.9-0.9V0.9C5.6,0.4,6,0,6.5,0h11.1c0.5,0,0.9,0.4,0.9,0.9v14.1c0,0.5-0.4,0.9-0.9,0.9H6.5z M8,1.8c-0.3,0-0.6,0.3-0.6,0.6v11.2c0,0.3,0.3,0.6,0.6,0.6h8.1c0.3,0,0.6-0.3,0.6-0.6V2.4c0-0.3-0.3-0.6-0.6-0.6H8z"/><path fill="#00ADB5" d="M14.1,5.3H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,4.9,14.6,5.3,14.1,5.3z"/><path fill="#00ADB5" d="M14.1,8.8H10C9.5,8.8,9.1,8.4,9.1,8v0c0-0.5,0.4-0.9,0.9-0.9h4.1C14.6,7.1,15,7.5,15,8v0C15,8.4,14.6,8.8,14.1,8.8z"/><path fill="#00ADB5" d="M14.1,12.4H10c-0.5,0-0.9-0.4-0.9-0.9v0c0-0.5,0.4-0.9,0.9-0.9h4.1c0.5,0,0.9,0.4,0.9,0.9v0C15,12,14.6,12.4,14.1,12.4z"/></g></svg></a>									
									<a v-if="iCan('delete','sgpgconfigs',$route.params.namespace)" v-on:click="deleteCRD('sgpgconfig',currentNamespace, conf.name)" class="delete" title="Delete Configuration">
										<svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
									</a>
								</td>
							</tr>
							<tr :style="$route.params.name == conf.name ? 'display: table-row' : ''" :class="$route.params.name == conf.name ? 'open details pgConfig' : 'details pgConfig'">
								<td colspan="4">
									<div class="configurationDetails">
										<span class="title">Configuration Details</span>	
										<table>
											<tbody>
												<tr>
													<td class="label">Postgres Version <span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'spec.postgresVersion')"></span></td>
													<td>{{ conf.data.spec.postgresVersion }}</td>
												</tr>
												<template v-if="conf.data.status.clusters.length">
													<tr>
														<td class="label">Used on</td>
														<td class="usedOn">
															<ul>
																<li v-for="c in conf.data.status.clusters">
																	{{ c }}
																	<router-link :to="'/admin/cluster/status/'+currentNamespace+'/'+c" title="Cluster Details">
																		<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
																	</router-link>
																</li>
															</ul>
														</td>
													</tr>
												</template>
											</tbody>
										</table>
									</div>
									<div class="paramDetails" v-if="conf.data.status['postgresql.conf'].length">
										<template v-if="conf.data.status.defaultParameters.length != conf.data.status['postgresql.conf'].length">
											<span class="title">
												Parameters
												<span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'status.postgresql.conf')"></span>
											</span>
											<table>
												<tbody>
													<tr v-for="param in conf.data.status['postgresql.conf']" v-if="!conf.data.status.defaultParameters.includes(param.parameter)">
														<td class="label">
															{{ param.parameter }}
														</td>
														<td class="paramValue">
															{{ param.value }}
															<a v-if="typeof param.documentationLink !== 'undefined'" :href="param.documentationLink" target="_blank">
																<svg xmlns="http://www.w3.org/2000/svg" width="14.999" height="14.999" viewBox="0 0 14.999 14.999"><g transform="translate(4.772 3.02)"><path d="M10.271,6.274A1.006,1.006,0,0,1,9.162,5.266a1.236,1.236,0,0,1,1.263-1.2,1,1,0,0,1,1.12,1.006A1.227,1.227,0,0,1,10.271,6.274Z" transform="translate(-7.191 -4.062)" fill="#d3d3d6"/><path d="M9.635,13.986a2.8,2.8,0,0,1-.624-.067,1.807,1.807,0,0,1-.784-.382,1.548,1.548,0,0,1-.45-.681,2,2,0,0,1-.1-.634,3.539,3.539,0,0,1,.077-.636l.016-.081c.076-.307.382-1.486.382-1.486A.573.573,0,0,0,8.178,9.6a.4.4,0,0,0-.365-.223H6.837A.423.423,0,0,1,6.7,9.344a.261.261,0,0,1-.1-.06.252.252,0,0,1-.059-.094,2.271,2.271,0,0,1,.123-.857l.02-.088a.753.753,0,0,1,.046-.163.277.277,0,0,1,.214-.16h3.083a.319.319,0,0,1,.256.127.288.288,0,0,1,.053.252l-.784,3.351a1.41,1.41,0,0,0-.043.265c0,.361.188.538.576.538a1.469,1.469,0,0,0,.467-.1l.131-.043a.9.9,0,0,1,.166-.021c.145.019.2.052.23.1.051.091.232.726.271.877a.639.639,0,0,1,.028.18.312.312,0,0,1-.185.23,3.627,3.627,0,0,1-.356.106,6.275,6.275,0,0,1-.624.145A3.656,3.656,0,0,1,9.635,13.986Z" transform="translate(-6.534 -5.027)" fill="#d3d3d6"/></g><path d="M7.67.035a7.5,7.5,0,1,0,7.5,7.5A7.5,7.5,0,0,0,7.67.035Zm0,13.511a6.012,6.012,0,1,1,6.012-6.012A6.019,6.019,0,0,1,7.67,13.546Z" transform="translate(-0.17 -0.035)" fill="#d3d3d6"/></svg>
															</a>
															<template v-else>
																<svg class="grayscale" xmlns="http://www.w3.org/2000/svg" width="14.999" height="14.999" viewBox="0 0 14.999 14.999"><g transform="translate(4.772 3.02)"><path d="M10.271,6.274A1.006,1.006,0,0,1,9.162,5.266a1.236,1.236,0,0,1,1.263-1.2,1,1,0,0,1,1.12,1.006A1.227,1.227,0,0,1,10.271,6.274Z" transform="translate(-7.191 -4.062)" fill="#d3d3d6"/><path d="M9.635,13.986a2.8,2.8,0,0,1-.624-.067,1.807,1.807,0,0,1-.784-.382,1.548,1.548,0,0,1-.45-.681,2,2,0,0,1-.1-.634,3.539,3.539,0,0,1,.077-.636l.016-.081c.076-.307.382-1.486.382-1.486A.573.573,0,0,0,8.178,9.6a.4.4,0,0,0-.365-.223H6.837A.423.423,0,0,1,6.7,9.344a.261.261,0,0,1-.1-.06.252.252,0,0,1-.059-.094,2.271,2.271,0,0,1,.123-.857l.02-.088a.753.753,0,0,1,.046-.163.277.277,0,0,1,.214-.16h3.083a.319.319,0,0,1,.256.127.288.288,0,0,1,.053.252l-.784,3.351a1.41,1.41,0,0,0-.043.265c0,.361.188.538.576.538a1.469,1.469,0,0,0,.467-.1l.131-.043a.9.9,0,0,1,.166-.021c.145.019.2.052.23.1.051.091.232.726.271.877a.639.639,0,0,1,.028.18.312.312,0,0,1-.185.23,3.627,3.627,0,0,1-.356.106,6.275,6.275,0,0,1-.624.145A3.656,3.656,0,0,1,9.635,13.986Z" transform="translate(-6.534 -5.027)" fill="#d3d3d6"/></g><path d="M7.67.035a7.5,7.5,0,1,0,7.5,7.5A7.5,7.5,0,0,0,7.67.035Zm0,13.511a6.012,6.012,0,1,1,6.012-6.012A6.019,6.019,0,0,1,7.67,13.546Z" transform="translate(-0.17 -0.035)" fill="#d3d3d6"/></svg>
															</template>
														</td>
													</tr>
												</tbody>
											</table>
										</template>

										<template v-if="conf.data.status.defaultParameters.length">
											<span class="title">
												Default Parameters
												<span class="helpTooltip" @mouseover="helpTooltip( 'SGPostgresConfig', 'status.defaultParameters')"></span>
											</span>	
											<table class="defaultParams">
												<tbody>
													<tr v-for="param in conf.data.status['postgresql.conf']" v-if="conf.data.status.defaultParameters.includes(param.parameter)">
														<td class="label">
															{{ param.parameter }}
														</td>
														<td class="paramValue">
															{{ param.value }}
															<a v-if="typeof param.documentationLink !== 'undefined'" :href="param.documentationLink" target="_blank">
																<svg xmlns="http://www.w3.org/2000/svg" width="14.999" height="14.999" viewBox="0 0 14.999 14.999"><g transform="translate(4.772 3.02)"><path d="M10.271,6.274A1.006,1.006,0,0,1,9.162,5.266a1.236,1.236,0,0,1,1.263-1.2,1,1,0,0,1,1.12,1.006A1.227,1.227,0,0,1,10.271,6.274Z" transform="translate(-7.191 -4.062)" fill="#d3d3d6"/><path d="M9.635,13.986a2.8,2.8,0,0,1-.624-.067,1.807,1.807,0,0,1-.784-.382,1.548,1.548,0,0,1-.45-.681,2,2,0,0,1-.1-.634,3.539,3.539,0,0,1,.077-.636l.016-.081c.076-.307.382-1.486.382-1.486A.573.573,0,0,0,8.178,9.6a.4.4,0,0,0-.365-.223H6.837A.423.423,0,0,1,6.7,9.344a.261.261,0,0,1-.1-.06.252.252,0,0,1-.059-.094,2.271,2.271,0,0,1,.123-.857l.02-.088a.753.753,0,0,1,.046-.163.277.277,0,0,1,.214-.16h3.083a.319.319,0,0,1,.256.127.288.288,0,0,1,.053.252l-.784,3.351a1.41,1.41,0,0,0-.043.265c0,.361.188.538.576.538a1.469,1.469,0,0,0,.467-.1l.131-.043a.9.9,0,0,1,.166-.021c.145.019.2.052.23.1.051.091.232.726.271.877a.639.639,0,0,1,.028.18.312.312,0,0,1-.185.23,3.627,3.627,0,0,1-.356.106,6.275,6.275,0,0,1-.624.145A3.656,3.656,0,0,1,9.635,13.986Z" transform="translate(-6.534 -5.027)" fill="#d3d3d6"/></g><path d="M7.67.035a7.5,7.5,0,1,0,7.5,7.5A7.5,7.5,0,0,0,7.67.035Zm0,13.511a6.012,6.012,0,1,1,6.012-6.012A6.019,6.019,0,0,1,7.67,13.546Z" transform="translate(-0.17 -0.035)" fill="#d3d3d6"/></svg>
															</a>
															<template v-else>
																<svg class="grayscale" xmlns="http://www.w3.org/2000/svg" width="14.999" height="14.999" viewBox="0 0 14.999 14.999"><g transform="translate(4.772 3.02)"><path d="M10.271,6.274A1.006,1.006,0,0,1,9.162,5.266a1.236,1.236,0,0,1,1.263-1.2,1,1,0,0,1,1.12,1.006A1.227,1.227,0,0,1,10.271,6.274Z" transform="translate(-7.191 -4.062)" fill="#d3d3d6"/><path d="M9.635,13.986a2.8,2.8,0,0,1-.624-.067,1.807,1.807,0,0,1-.784-.382,1.548,1.548,0,0,1-.45-.681,2,2,0,0,1-.1-.634,3.539,3.539,0,0,1,.077-.636l.016-.081c.076-.307.382-1.486.382-1.486A.573.573,0,0,0,8.178,9.6a.4.4,0,0,0-.365-.223H6.837A.423.423,0,0,1,6.7,9.344a.261.261,0,0,1-.1-.06.252.252,0,0,1-.059-.094,2.271,2.271,0,0,1,.123-.857l.02-.088a.753.753,0,0,1,.046-.163.277.277,0,0,1,.214-.16h3.083a.319.319,0,0,1,.256.127.288.288,0,0,1,.053.252l-.784,3.351a1.41,1.41,0,0,0-.043.265c0,.361.188.538.576.538a1.469,1.469,0,0,0,.467-.1l.131-.043a.9.9,0,0,1,.166-.021c.145.019.2.052.23.1.051.091.232.726.271.877a.639.639,0,0,1,.028.18.312.312,0,0,1-.185.23,3.627,3.627,0,0,1-.356.106,6.275,6.275,0,0,1-.624.145A3.656,3.656,0,0,1,9.635,13.986Z" transform="translate(-6.534 -5.027)" fill="#d3d3d6"/></g><path d="M7.67.035a7.5,7.5,0,1,0,7.5,7.5A7.5,7.5,0,0,0,7.67.035Zm0,13.511a6.012,6.012,0,1,1,6.012-6.012A6.019,6.019,0,0,1,7.67,13.546Z" transform="translate(-0.17 -0.035)" fill="#d3d3d6"/></svg>
															</template>
														</td>
													</tr>
												</tbody>
											</table>
										</template>
									</div>
								</td>
							</tr>
						</template>
					</tbody>
				</table>
			</div>
			<div id="nameTooltip">
				<div class="info"></div>
			</div>
		</div>`,
	data: function() {

		return {
			currentSort: 'data.metadata.name',
			currentSortDir: 'desc',
		}
	},
	computed: {

		config () {
			return sortTable( store.state.pgConfig, this.currentSort, this.currentSortDir )
		},

		currentNamespace () { 
			return store.state.currentNamespace
		},

	},
	mounted: function() {


		$('tr.toggle').click(function() {
			$(this).toggleClass("open");
			$('tr.toggle').not(this).removeClass("open");
		});
	}
	
})
 