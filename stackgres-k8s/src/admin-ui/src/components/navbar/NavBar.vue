<template>
  <!-- Vue reactivity hack -->
	<aside id="nav" class="disabled">
    	<div id="topMenu" v-if="$route.params.hasOwnProperty('namespace')" @click="toggleViewMode()">
			<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16"><path fill="#FFF" opacity=".75" d="M0 16h24v-2.7H0V16zm0-6.7h24V6.7H0v2.6zM0 0v2.7h24V0H0z"/></svg>
		</div>
		<div id="logo" :class="!$route.params.hasOwnProperty('namespace') && 'hiddenMenu'">
			<router-link to="/" title="Global Dashboard">
				<svg xmlns="http://www.w3.org/2000/svg" width="29.997" height="25.348"><path d="M0 14.125l6.78 7.769h16.438l6.779-7.769-6.779-7.769H6.78z" fill="#42a8c8"/><path fill="#426d88" d="M6.78 21.894h16.443v3.455H6.78z"/><path d="M6.78 25.348L0 17.574v-3.45l6.78 7.77z" fill="#428bb4"/><path d="M23.218 25.348l6.779-7.769v-3.454l-6.779 7.769z" fill="#16657c"/><g><path d="M28.213 12.882c0-2.882-5.92-5.219-13.215-5.219s-13.21 2.336-13.21 5.219 5.915 5.219 13.21 5.219 13.215-2.337 13.215-5.219z" fill="#39b54a"/><path d="M28.213 12.882c0 2.882-5.92 5.219-13.215 5.219s-13.21-2.336-13.21-5.219v2.873c.91 2.533 6.525 5.219 13.21 5.219s12.3-2.687 13.215-5.219v-2.873z" fill="#009245"/></g><g><path d="M.678 8.302l14.323 8.3 14.323-8.3-14.323-8.3z" fill="#f2c63f"/><path d="M.678 8.302v3.235l14.323 8.3v-3.235z" fill="#f2b136"/><path d="M29.324 8.302L15 16.602v3.235l14.324-8.3z" fill="#f2a130"/></g><g><path d="M3.846 10.368l22.307-3.242-5.657-6.5z" fill="#ff7124"/><path d="M3.846 10.368l22.307-3.242v2.928L3.846 13.295z" fill="#d93d1b"/></g></svg>
				<h1>
					Stack<strong>Gres</strong>
					<span id="sgVersion">v{{ sgVersion }}</span>
				</h1>
			</router-link>
		</div>

		<div class="right">
			<div id="reload" @click="fetchAPI()" v-if="loggedIn">
				<svg xmlns="http://www.w3.org/2000/svg" width="20.001" height="20" viewBox="0 0 20.001 20"><g transform="translate(0 0)"><path d="M1.053,11.154A1.062,1.062,0,0,1,0,10.089,9.989,9.989,0,0,1,16.677,2.567l.484-.484a.486.486,0,0,1,.2-.121.541.541,0,0,1,.663.343l1.318,3.748a.522.522,0,0,1,.007.327.5.5,0,0,1-.627.323L18.7,6.7l-3.743-1.32a.531.531,0,0,1-.206-.13.52.52,0,0,1-.016-.733l.464-.465A7.9,7.9,0,0,0,2.092,10.1a1.04,1.04,0,0,1-1.039,1.057Z"/><path d="M18.947,8.844A1.063,1.063,0,0,1,20,9.91,9.989,9.989,0,0,1,3.323,17.434l-.484.484a.476.476,0,0,1-.2.121.541.541,0,0,1-.663-.343L.659,13.948a.522.522,0,0,1-.007-.327.5.5,0,0,1,.627-.323l.022.008,3.743,1.32a.531.531,0,0,1,.206.13.52.52,0,0,1,.016.733l-.464.465A7.9,7.9,0,0,0,17.908,9.9a1.04,1.04,0,0,1,1.039-1.057Z"/></g></svg>
			</div>

			<div id="tzToggle" class="hoverPointer" :class="timezone" @click="toggleTimezone()" :title="'Switch timezone to ' + ((timezone == 'local') ? 'UTC' : Intl.DateTimeFormat().resolvedOptions().timeZone)">
				<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10,0C4.5,0,0,4.5,0,10s4.5,10,10,10s10-4.5,10-10S15.5,0,10,0z M18.1,9.1h-4.2c-0.2-2.6-1-5-2.4-7.2C15,2.6,17.8,5.5,18.1,9.1z M7.9,10.9h4.3c-0.2,2.2-0.9,4.3-2.1,6.2C8.8,15.2,8.1,13.1,7.9,10.9z M7.9,9.1C8.1,6.9,8.8,4.8,10,2.9c1.2,1.8,1.9,4,2.1,6.2H7.9z M8.5,1.9C7.1,4.1,6.3,6.5,6.1,9.1H1.9C2.2,5.5,5,2.6,8.5,1.9z M1.9,10.9h4.2c0.2,2.6,1,5,2.4,7.2C5,17.4,2.2,14.5,1.9,10.9z M11.5,18.1c1.4-2.1,2.2-4.6,2.4-7.2h4.2C17.8,14.5,15,17.4,11.5,18.1z"/></svg>
			</div>

			<button
				type="button"
				id="darkmode"
				@click="toggleTheme()"
				class="plain"
			>
				<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g transform="translate(-90 -152)"><rect width="2" height="2" rx="1" transform="translate(103 152) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(103 174) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(114 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(92 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(111.778 155.636) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(95.05 172.364) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(93.636 154.222) rotate(45)"/><rect width="2" height="2" rx="1" transform="translate(110.364 170.95) rotate(45)"/><path d="M102,156a8,8,0,1,0,8,8A8,8,0,0,0,102,156Zm-5.336,8A5.343,5.343,0,0,1,102,158.664v10.672A5.343,5.343,0,0,1,96.664,164Z" transform="translate(0 0)"/></g></svg>
			</button>

			<NotificationsArea v-if="loggedIn"></NotificationsArea>

			<div 
				v-if="loggedIn && havePermissionsTo.get.users"
				id="usersManagement"
			>
				<router-link
					:to="'/manage/users'"
					title="User Management"
				>
					<svg xmlns="http://www.w3.org/2000/svg" width="19" height="15.909"><g><path d="M13.364 15.909a1 1 0 0 1-1-1v-1.545a2.093 2.093 0 0 0-2.091-2.091H4.091A2.09 2.09 0 0 0 2 13.364v1.545a1 1 0 0 1-2 0v-1.545a4.09 4.09 0 0 1 4.091-4.091h6.182a4.1 4.1 0 0 1 4.091 4.091v1.545a1 1 0 0 1-1 1M7.182 0a4.091 4.091 0 1 1-4.091 4.091A4.1 4.1 0 0 1 7.182 0m0 6.182a2.091 2.091 0 1 0-2.091-2.091 2.093 2.093 0 0 0 2.091 2.091M18 15.909a1 1 0 0 1-1-1v-1.546a2.09 2.09 0 0 0-1.568-2.022 1 1 0 1 1 .5-1.936A4.09 4.09 0 0 1 19 13.363v1.546a1 1 0 0 1-1 1"/><path d="M12.591 8.089a1 1 0 0 1-.247-1.969 2.091 2.091 0 0 0 0-4.051 1 1 0 1 1 .5-1.937 4.091 4.091 0 0 1 0 7.926 1 1 0 0 1-.253.031"/></g></svg>
				</router-link>
			</div>

			<div
				v-if="loggedIn && sgConfigName"
				id="operatorSettings"
			>
				<router-link :to="'/sgconfig/' + sgConfigName" title="Operator Settings">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M13.193 10A3.193 3.193 0 1010 13.2a3.2 3.2 0 003.193-3.2zm-1.809 0A1.384 1.384 0 1110 8.614 1.386 1.386 0 0111.384 10z"/><path class="a" d="M16.961 12.835a.443.443 0 01.44-.246 2.6 2.6 0 000-5.2h-.136a.4.4 0 01-.318-.157.988.988 0 00-.055-.164.427.427 0 01.122-.486A2.6 2.6 0 1013.3 2.937a.414.414 0 01-.287.116.4.4 0 01-.292-.12.455.455 0 01-.123-.357 2.591 2.591 0 00-.762-1.84 2.659 2.659 0 00-3.675 0 2.6 2.6 0 00-.76 1.84v.137a.406.406 0 01-.158.318 1.078 1.078 0 00-.163.055.41.41 0 01-.465-.1l-.076-.077a2.5 2.5 0 00-1.853-.729 2.576 2.576 0 00-1.822.8 2.632 2.632 0 00.1 3.71.434.434 0 01.058.5.423.423 0 01-.422.265 2.6 2.6 0 000 5.2h.133a.41.41 0 01.285.117.43.43 0 01-.035.629l-.079.079v.005A2.61 2.61 0 003 17.135a2.479 2.479 0 001.853.728 2.614 2.614 0 001.847-.827.429.429 0 01.5-.057.419.419 0 01.264.42 2.6 2.6 0 105.2 0v-.132a.414.414 0 01.116-.284.421.421 0 01.3-.126.356.356 0 01.278.113l.1.1a2.731 2.731 0 001.852.728 2.6 2.6 0 002.55-2.65 2.611 2.611 0 00-.825-1.857.4.4 0 01-.081-.444zm-6.2 4.422v.143a.691.691 0 01-.69.691.718.718 0 01-.692-.788 2.289 2.289 0 00-1.457-2.095 2.274 2.274 0 00-.919-.2 2.427 2.427 0 00-1.7.728.7.7 0 01-.5.213.652.652 0 01-.482-.194.676.676 0 01-.208-.477.749.749 0 01.217-.53l.064-.064a2.323 2.323 0 00-1.654-3.938H2.6a.692.692 0 01-.489-1.18.755.755 0 01.587-.2A2.286 2.286 0 004.788 7.9a2.306 2.306 0 00-.467-2.556l-.069-.069a.693.693 0 01.478-1.191.655.655 0 01.5.213l.069.071a2.257 2.257 0 002.334.536.92.92 0 00.27-.071 2.312 2.312 0 001.4-2.121v-.134a.687.687 0 01.2-.489.705.705 0 01.977 0 .751.751 0 01.2.571 2.3 2.3 0 00.705 1.64 2.331 2.331 0 001.649.665 2.369 2.369 0 001.652-.713.691.691 0 011.181.488.753.753 0 01-.259.547 2.253 2.253 0 00-.538 2.334.932.932 0 00.072.274 2.313 2.313 0 002.119 1.4h.139a.691.691 0 01.69.692.717.717 0 01-.768.691 2.312 2.312 0 00-2.113 1.395 2.345 2.345 0 00.533 2.619.693.693 0 01-.45 1.192.749.749 0 01-.506-.19l-.1-.1a2.4 2.4 0 00-1.653-.654 2.325 2.325 0 00-2.283 2.312zM5.5 4.177z"/></svg>
				</router-link>
			</div>

			<div id="logout" v-if="loggedIn">
				<a @click="logout(authType)">Logout <svg xmlns="http://www.w3.org/2000/svg" width="10.546" height="10.5" viewBox="0 0 10.546 10.5"><g transform="translate(-30 -181.75)"><path d="M33.92,192h-2.1a1.538,1.538,0,0,1-1.571-1.5v-7a1.538,1.538,0,0,1,1.571-1.5h2.1a.5.5,0,1,1,0,1h-2.1a.515.515,0,0,0-.527.5v7a.515.515,0,0,0,.527.5h2.1a.5.5,0,1,1,0,1Z" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/><path d="M42.157,192.074l1.965-1.965a.525.525,0,0,0,0-.741L42.157,187.4a.524.524,0,0,0-.741.74l1.072,1.071h-3.7a.524.524,0,1,0,0,1.048h3.7l-1.072,1.071a.524.524,0,0,0,.741.74Z" transform="translate(-4.026 -2.739)" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/></g></svg></a>
			</div>

			<div id="delete" class="hasTooltip hideOnClick">
				<div class="tooltip">
					<h3>This action is permanent</h3>
					<p>
						Are you sure you want to delete the {{ deleteItem.kind }} <strong>{{ deleteItem.name }}</strong>?<br/>
						Keep in mind that this action will permanently delete your {{ deleteItem.kind }}.
					</p>
					<p>
						Please type the exact name of the {{ deleteItem.kind }} to confirm.
					</p>
					<form @submit="confirmDelete(confirmDeleteName)">
						<input id="deleteName" v-model="confirmDeleteName" :placeholder="deleteItem.kind+' name'" autocomplete="off">
						<span class="warning" style="display:none">The {{ deleteItem.kind }} name does not match the name of the element requested to be deleted.</span>
						<a class="confirmDelete" @click="confirmDelete(confirmDeleteName)">DELETE ITEM</a> <a class="cancelDelete" @click="cancelDelete()">CANCEL</a>
					</form>
				</div>
			</div>

			<div id="signup" :class="( (authType == 'OIDC') && 'textCenter' )">
				<form id="login" class="form noSubmit">
					<div class="header">
						<h2>Welcome to StackGres!</h2>
					</div>

					<p>To continue, please Log in.</p>

					<template v-if="(authType == 'JWT')">
						<label for="loginUser">
							Username <span class="req">*</span>
						</label>
						<input v-model="loginUser" placeholder="username">

						<label for="loginPassword">
							Password <span class="req">*</span>
						</label>
						<input v-model="loginPassword" placeholder="password" :type="loginPasswordType">

						<a @click="showPassword()" id="showPassword">
							<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
						</a>

						<span class="warning" style="display:none">
							Wrong username or password. Please try again!
						</span>

						<hr/>
						<button @click="login('JWT')">Login</button>
					</template>
					<template v-else-if="(authType == 'OIDC')">
						<hr/>
						<button @click="login('OIDC')">Login using OIDC</button>
					</template>
				</form>
			</div>

			<div id="clone" :class="clone.hasOwnProperty('name') ? 'show' : ''">
				<form class="form">
					<div class="header" v-if="clone.hasOwnProperty('kind')">
						<h2>Clone {{ (clone.kind == 'SGDistributedLogs') ? clone.kind : clone.kind.slice(0, -1) }}</h2>
					</div>
					<label for="cloneNamespace">Namespace <span class="req">*</span></label>
					<select @change="setCloneNamespace" id="cloneNamespace">
						<option v-for="namespace in namespaces">{{ namespace }}</option>
					</select>

					<label for="cloneName">Name <span class="req">*</span></label>
					<input @keyup="setCloneName" id="cloneName" autocomplete="off">

					<span class="warning" v-if="clone.hasOwnProperty('kind') && nameCollision">
						There's already a <strong>{{ (clone.kind == 'SGDistributedLogs') ? clone.kind : clone.kind.slice(0, -1) }}</strong> with the same name on the specified namespace. Please specify a different name or choose another namespace
					</span>

					<span class="warning" v-if="clone.kind == 'SGClusters'">
						This action will create a new cluster with the same configuration as the source cluster. Please note that:
						<ul>
							<li>The cluster will be created as soon as this configuration is copied</li>
							<li>Every configuration associated to this cluster, must already exist on the target namespace</li>
							<li>No source data is copied whatsoever</li>
						</ul>
					</span>

					<span v-if="missingCRDs.length" class="warning alert">
						Unable to clone cluster configuration. The following dependencies do not exist on the target namespace:
						<ul>
							<li v-for="crd in missingCRDs">
								<strong>{{ crd.kind }}</strong>: <router-link :to="'/' + currentNamespace + '/' + ( (crd.kind == 'SGPoolingConfig') ? 'sgpoolconfig' : ( (crd.kind == 'SGPostgresConfig') ? 'sgpgconfig' : crd.kind.toLowerCase() ) )  + '/' + crd.name">{{ crd.name }}</router-link>
							</li>
						</ul>
						Please clone or create the dependencies manually on the target namespace and try again.
					</span>

					<a class="btn" @click="cloneCRD" :disabled="nameCollision">CLONE</a> <a class="btn border" @click="cancelClone">CANCEL</a>
				</form>
			</div>

			<div id="restartCluster" v-if="restartCluster.hasOwnProperty('name')">
				<h3 class="title">Restart Cluster</h3>
				<p>
					This operation will create a new <strong>RESTART sgdbop</strong> with the name <strong>{{ restartCluster.restartName }}</strong> and the cluster <strong>{{ restartCluster.name }}</strong> will be restarted as soon as posible.
				</p><br/>
				
				<p><strong>Are you sure you want to proceed?</strong></p><br/>

				<a class="btn" @click="executeClusterRestart()">Restart</a> <a class="btn border" @click="setRestartCluster()">Cancel</a>
			</div>
		</div>

		<div id="helpTooltip" class="hideOnClick"><vue-markdown :source=tooltipsText :breaks=false></vue-markdown></div>
		
		<NotFound v-if="loggedIn && notFound"></NotFound>
	</aside>
</template>

<script>
	import store from '../../store'
	import router from '../../router'
	import sgApi from '../../api/sgApi'
	import { mixin } from '../mixins/mixin'

	/* Child Components */
	import NotificationsArea from './NotificationsArea.vue'
	import NotFound from '../NotFound.vue'


    export default {
        name: 'NavBar',

		mixins: [mixin],

		components: {
			NotificationsArea,
			NotFound
		},

		data: function() {
			return {
				initialized: false,
				polling: '',
				sgVersion: '',
				loginUser: '',
				loginPassword: '',
				loginPasswordType: 'password',
			}
		},

		computed: {

			namespaces () {
				return store.state.Allnamespaces
			},

			currentNamespace () {
				return store.state.currentNamespace;
			},

			currentCluster () {
				return store.state.currentCluster;
			},

			currentPods () {
				return store.state.currentPods;
			},

			deleteItem () {
				return store.state.deleteItem;
			},

			theme () {
				return store.state.theme
			},

			timezone () {
				return store.state.timezone
			},

			clone () {
				return store.state.cloneCRD
			},

			tooltipsText () {
				return store.state.tooltipsText
			},

			notFound() {
				return store.state.notFound
			},

			authType() {
				return store.state.authType
			},

			nameCollision() {
				if(store.state.cloneCRD.hasOwnProperty('kind')) {
					let kind = ( 
						(store.state.cloneCRD.kind == 'SGPostgresConfigs') ? 
							'sgpgconfigs' : 
							(  (store.state.cloneCRD.kind == 'SGPoolingConfigs') ? 
								'sgpoolconfigs' : 
								store.state.cloneCRD.kind.toLowerCase() 
							) 
						);
					let collision = store.state[kind].find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
					return (typeof collision != 'undefined')
				} else {
					return false
				}
			},

			missingCRDs() {
				const vc = this;
				let missingCRDs = [];

				if(typeof store.state.cloneCRD.data != 'undefined') {
					let cloneCRD = store.state.cloneCRD.data;
					let cloneKind = store.state.cloneCRD.kind;
					let targetNamespace = cloneCRD.metadata.namespace;


					if (cloneKind == 'SGClusters') {

						let profile = store.state.sginstanceprofiles.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.sgInstanceProfile))
						if (typeof profile == 'undefined')
							missingCRDs.push({kind: 'SGInstanceProfile', name: cloneCRD.spec.sgInstanceProfile})

						let pgconfig = store.state.sgpgconfigs.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.sgPostgresConfig))
						if (typeof pgconfig == 'undefined')
							missingCRDs.push({kind: 'SGPostgresConfig', name: cloneCRD.spec.configurations.sgPostgresConfig})

						let poolconfig = store.state.sgpoolconfigs.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.sgPoolingConfig))
						if (typeof poolconfig == 'undefined')
							missingCRDs.push({kind: 'SGPoolingConfig', name: cloneCRD.spec.configurations.sgPoolingConfig})

						if ( this.hasProp(cloneCRD, 'spec.configurations.backups.sgObjectStorage') ) {
							let objectStorage = store.state.sgobjectstorages.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.backups.sgObjectStorage))
							if (typeof objectStorage == 'undefined')
								missingCRDs.push({kind: 'SGObjectStorage', name: cloneCRD.spec.configurations.backup.sgObjectStorage})
						}
					}
				}

				return missingCRDs
			},

			restartCluster() {
				return store.state.restartCluster
			},

			sgConfigName() {
				return store.state.sgconfigs.length ? store.state.sgconfigs[0].metadata.name : null
			}

		},

		methods: {

			login(authType = 'JWT') {

				const vc = this;

				if(authType == 'JWT') {
					sgApi
					.create('login', {
						username: this.loginUser,
						password: this.loginPassword
					})
					.then( function(response){
						store.commit('setLoginToken', response.data.access_token);
						$('#signup').fadeOut();
						document.cookie = "sgToken="+response.data.access_token+"; Path=/; SameSite=Strict;";
						vc.fetchAPI();
					}
					).catch(function(err) {
						$('#login .warning').fadeIn();
					});
				} else if (authType == 'OIDC') {
					document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';

					console.log('Using OIDC Auth');
					window.location.replace( window.location.origin + '/stackgres/auth/external?redirectTo=' + window.location.href);
				}

			},

			logout(authType = 'JWT') {

				if(authType == 'JWT') {
					document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';
				} else if (authType == 'OIDC') {
					sgApi
					.get('logout')
					.then(function(response) {
						console.log('Logged out from OIDC');
					})
					.catch(function(error) {
						console.log(error);
					});
				}
				
				store.commit('setLoginToken');
				store.commit('flushPermissions');
				$('#signup').addClass('login').fadeIn();
				router.push('/');
			},

			showPassword: function() {

				if(this.loginPasswordType == 'text') {
					this.loginPasswordType = 'password';
					$('#showPassword').removeClass('active');
				} else {
					this.loginPasswordType = 'text';
					$('#showPassword').addClass('active');
				}
			},

			setCloneName: function() {
				store.commit('setCloneName', $('#cloneName').val());
			},

			setCloneNamespace: function() {
				store.commit('setCloneNamespace', $('#cloneNamespace').val());
			},

			cancelClone: function() {
				store.commit('setCloneCRD', {});
				$('#cloneNamespace').val(this.$route.params.namespace)
			},

			cloneCRD: function() {
				
				const vc = this
				let cloneCRD = store.state.cloneCRD.data;
				let cloneKind = store.state.cloneCRD.kind;

				if(cloneKind == 'SGPoolingConfigs')
					cloneKind = 'sgpoolconfigs'
				else if (cloneKind == 'SGPostgresConfigs')
					cloneKind = 'sgpgconfigs'
				else if (cloneKind == 'SGClusters') {
					
					if( vc.hasProp(cloneCRD.spec, 'distributedLogs.sgDistributedLogs') && !cloneCRD.spec.distributedLogs.sgDistributedLogs.includes('.') ) {
						cloneCRD.spec.distributedLogs.sgDistributedLogs = vc.$route.params.namespace + '.' + cloneCRD.spec.distributedLogs.sgDistributedLogs;
					}

					if( vc.hasProp(cloneCRD, 'spec.initialData.scripts') ) {
						cloneCRD.spec.initialData.scripts.forEach( s => {
							if(vc.hasProp(s, 'scriptFrom.configMapKeyRef')) {
								delete s.scriptFrom.configMapScript
							}
						});
					}

				}

				if (!vc.missingCRDs.length) {
					sgApi
					.create(cloneKind.toLowerCase(), cloneCRD)
					.then(function (response) {
						vc.notify('Resource <strong>"'+store.state.cloneCRD.data.metadata.name+'"</strong> cloned successfully', 'message', store.state.cloneCRD.kind.toLowerCase());
						vc.fetchAPI(cloneKind.toLowerCase());
						vc.cancelClone();
					})
					.catch(function (error) {
						console.log(error.response);
						vc.cancelClone();

						if(typeof error.response != 'undefined') {
							vc.notify(error.response.data,'error',cloneKind.toLowerCase());
						}

					});
				}
			},

			flushToken: function() {
				document.cookie = 'sgToken=; SameSite=Strict;';
				store.commit('setLoginToken','401 Authentication Error');
				console.log('Flushed');
			},
			cancelDelete: function(){
				$("#delete").removeClass("active");
				$("#delete .warning").hide();
				this.confirmDeleteName = '';
				store.commit('setConfirmDeleteName', '');
			},

			confirmDelete: function( confirmName ) {

				const vc = this;
				const item = store.state.deleteItem;

				if(confirmName == item.name) {
					$("#delete .warning").fadeOut();

					sgApi
					.delete(item.kind, {
						data: {
							"metadata": {
								"name": item.name,
								"namespace": item.namespace
							}
						}
					})
					.then(function (response) {

						store.commit("setDeleteItem", {
							kind: '',
							namespace: '',
							name: '',
							redirect: ''
						});

						vc.notify('Resource <strong>"'+item.name+'"</strong> deleted successfully', 'message', item.kind);

						if( (typeof item.redirect !== 'undefined') && item.redirect.length)
							router.push(item.redirect);

						$("#delete").removeClass("active");
						vc.confirmDeleteName = '';
					})
					.catch(function (error) {
						console.log(error);
						if(typeof error.response != 'undefined') {
							vc.notify(error.response.data,'error',item.kind);
						}
						
						vc.checkAuthError(error)
					});
				} else {
					$("#delete .warning").fadeIn();
				}

			},

			toggleTimezone() {
				store.commit('toggleTimezone')
				this.notify('You have set <strong class="upper">' + ( (store.state.timezone == 'utc') ? 'UTC' : Intl.DateTimeFormat().resolvedOptions().timeZone ) + '</strong> as the default timezone.', 'message')
			},

			executeClusterRestart() {
				const vc = this;

				let dbOp = {
					metadata: {
						name: store.state.restartCluster.restartName,
						namespace: store.state.restartCluster.namespace
					},
					spec: {
						sgCluster: store.state.restartCluster.name,
						op: 'restart',
						restart: {
							method: 'InPlace',
							onlyPendingRestart: false
						},
					}
				}

				sgApi
				.create('sgdbops', dbOp)
				.then(function (response) {
					vc.setRestartCluster();
					vc.notify('Restart operation created successfully', 'message', 'sgdbops');
					vc.fetchAPI('sgdbops');
				})
				.catch(function (error) {
					vc.setRestartCluster();
					console.log(error.response);
					vc.notify(error.response.data,'error','sgdbops');
				});
			},

			toggleViewMode() {
				store.commit('toggleView');
			},

			toggleTheme() {
				store.commit('setTheme');
			}

		},

		mounted: function() {
			let vc = this;

			fetch('/admin/info/sg-info.json')
			.then(response => response.json())
			.then(data =>
				vc.sgVersion = data.version
			);

			vc.fetchAPI();

			if( !vc.initialized && (store.state.interval > 0) ) {
				vc.polling = setInterval( function(){
					if(store.state.loginToken.length > 0)
						vc.fetchAPI();
				}.bind(this), store.state.interval * 30000);
				
				vc.initialized = true;
			}
	
			$(document).click(function(event) { 
				var $target = $(event.target);
				
				//Cancel Delete when clicked outside of Delete popup
				if( $('#delete.active').length && ( (!$target.is('a.deleteCRD')) && (!$target.closest('#delete').length) )) {
					vc.cancelDelete();
				}
			})
		},

	}
</script>


<style scoped>
	#tzToggle {
		position: relative;
	}

	#tzToggle.utc:after {
		content: "UTC";
		color: #6f7078;
		font-weight: bold;
		position: absolute;
		font-size: 7px;
		bottom: -3px;
		left: 50%;
		background: var(--bgColor);
		padding: 1px;
	}

	#restartCluster {
		padding: 20px;
		line-height: 1.5;
	}

	#restartCluster h3 {
		border-bottom: 1px solid var(--borderColor);
		padding-bottom: 5px;
		margin-bottom: 25px;
		font-size: 14px;
	}

	#login {
		margin-bottom: 0;
	}

	#login .warning {
		display: block;
	}
</style>
