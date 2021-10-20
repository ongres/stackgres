<template>
	<aside id="nav" class="disabled">
		<div id="topMenu" v-if="!$route.name.includes('GlobalDashboard')" @click="toggleViewMode()">
			<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 16"><path fill="#FFF" opacity=".75" d="M0 16h24v-2.7H0V16zm0-6.7h24V6.7H0v2.6zM0 0v2.7h24V0H0z"/></svg>
		</div>
		<div id="logo" :class="$route.name.includes('GlobalDashboard') && 'hiddenMenu'">
			<router-link to="/" title="Global Dashboard">
				<svg xmlns="http://www.w3.org/2000/svg" width="29.997" height="25.348"><path d="M0 14.125l6.78 7.769h16.438l6.779-7.769-6.779-7.769H6.78z" fill="#42a8c8"/><path fill="#426d88" d="M6.78 21.894h16.443v3.455H6.78z"/><path d="M6.78 25.348L0 17.574v-3.45l6.78 7.77z" fill="#428bb4"/><path d="M23.218 25.348l6.779-7.769v-3.454l-6.779 7.769z" fill="#16657c"/><g><path d="M28.213 12.882c0-2.882-5.92-5.219-13.215-5.219s-13.21 2.336-13.21 5.219 5.915 5.219 13.21 5.219 13.215-2.337 13.215-5.219z" fill="#39b54a"/><path d="M28.213 12.882c0 2.882-5.92 5.219-13.215 5.219s-13.21-2.336-13.21-5.219v2.873c.91 2.533 6.525 5.219 13.21 5.219s12.3-2.687 13.215-5.219v-2.873z" fill="#009245"/></g><g><path d="M.678 8.302l14.323 8.3 14.323-8.3-14.323-8.3z" fill="#f2c63f"/><path d="M.678 8.302v3.235l14.323 8.3v-3.235z" fill="#f2b136"/><path d="M29.324 8.302L15 16.602v3.235l14.324-8.3z" fill="#f2a130"/></g><g><path d="M3.846 10.368l22.307-3.242-5.657-6.5z" fill="#ff7124"/><path d="M3.846 10.368l22.307-3.242v2.928L3.846 13.295z" fill="#d93d1b"/></g></svg>
				<h1>
					Stack<strong>Gres</strong>
					<span id="sgVersion">v{{ sgVersion }}</span>
				</h1>
			</router-link>
		</div>

		<div class="right">
			<div id="tzToggle" class="cursor" :class="timezone" @click="toggleTimezone()" :title="'Switch timezone to ' + ((timezone == 'local') ? 'UTC' : Intl.DateTimeFormat().resolvedOptions().timeZone)">
				<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 465.2 465.2"><g xmlns="http://www.w3.org/2000/svg"><path class="rotate" d="M279.591 423.714a192.461 192.461 0 01-11.629 2.52c-10.148 1.887-16.857 11.647-14.98 21.804a18.651 18.651 0 007.618 11.876 18.64 18.64 0 0014.175 3.099 233.175 233.175 0 0013.854-3.008c10.021-2.494 16.126-12.646 13.626-22.662-2.494-10.025-12.637-16.125-22.664-13.629zM417.887 173.047a18.644 18.644 0 006.97 9.398c4.684 3.299 10.813 4.409 16.662 2.475 9.806-3.256 15.119-13.83 11.875-23.631a232.327 232.327 0 00-4.865-13.314c-3.836-9.59-14.714-14.259-24.309-10.423-9.585 3.834-14.256 14.715-10.417 24.308a194.816 194.816 0 014.084 11.187zM340.36 397.013a195.86 195.86 0 01-10.134 6.261c-8.949 5.162-12.014 16.601-6.854 25.546a18.664 18.664 0 005.416 5.942c5.769 4.059 13.604 4.667 20.127.909a233.049 233.049 0 0012.062-7.452c8.614-5.691 10.985-17.294 5.291-25.912-5.693-8.621-17.291-10.989-25.908-5.294zM465.022 225.279c-.407-10.322-9.101-18.356-19.426-17.953-10.312.407-18.352 9.104-17.947 19.422.155 3.945.195 7.949.104 11.89-.145 6.473 3.021 12.243 7.941 15.711a18.647 18.647 0 0010.345 3.401c10.322.229 18.876-7.958 19.105-18.285.103-4.709.064-9.48-.122-14.186zM414.835 347.816c-8.277-6.21-19.987-4.524-26.186 3.738a195.193 195.193 0 01-7.434 9.298c-6.69 7.86-5.745 19.666 2.115 26.361.448.38.901.729 1.371 1.057 7.814 5.509 18.674 4.243 24.992-3.171a232.358 232.358 0 008.874-11.102c6.2-8.262 4.522-19.98-3.732-26.181zM442.325 280.213c-9.855-3.09-20.35 2.396-23.438 12.251a198.06 198.06 0 01-3.906 11.253c-3.105 8.156-.13 17.13 6.69 21.939a18.635 18.635 0 004.126 2.19c9.649 3.682 20.454-1.159 24.132-10.812a240.351 240.351 0 004.646-13.382c3.085-9.857-2.397-20.349-12.25-23.439zM197.999 426.402a193.1 193.1 0 01-47.968-15.244c-.18-.094-.341-.201-.53-.287a204.256 204.256 0 01-10.63-5.382c-.012-.014-.034-.023-.053-.031a199.491 199.491 0 01-18.606-11.628C32.24 331.86 11.088 209.872 73.062 121.901c13.476-19.122 29.784-35.075 47.965-47.719.224-.156.448-.311.67-.468 64.067-44.144 151.06-47.119 219.089-1.757l-14.611 21.111c-4.062 5.876-1.563 10.158 5.548 9.518l63.467-5.682c7.12-.64 11.378-6.799 9.463-13.675L387.61 21.823c-1.908-6.884-6.793-7.708-10.859-1.833l-14.645 21.161C312.182 7.638 252.303-5.141 192.87 5.165a235.263 235.263 0 00-17.709 3.78c-.045.008-.081.013-.117.021-.225.055-.453.128-.672.189-51.25 13.161-95.965 43.052-127.872 85.7-.269.319-.546.631-.8.978a220.276 220.276 0 00-3.145 4.353 229.217 229.217 0 00-4.938 7.308c-.199.296-.351.597-.525.896C10.762 149.191-1.938 196.361.24 244.383c.005.158-.004.317 0 .479a227.87 227.87 0 001.088 14.129c.027.302.094.588.145.89a230.909 230.909 0 001.998 14.145c8.344 48.138 31.052 91.455 65.079 125.16.079.079.161.165.241.247.028.031.059.047.086.076a235.637 235.637 0 0029.793 24.898c28.02 19.744 59.221 32.795 92.729 38.808 10.167 1.827 19.879-4.941 21.703-15.103 1.823-10.169-4.939-19.889-15.103-21.71z"/><path d="M221.124 83.198c-8.363 0-15.137 6.78-15.137 15.131v150.747l137.87 71.271a15.042 15.042 0 006.933 1.69c5.476 0 10.765-2.982 13.454-8.185 3.835-7.426.933-16.549-6.493-20.384L236.244 230.65V98.329c-.001-8.351-6.767-15.131-15.12-15.131z"/></g></svg>
			</div>

			<div id="darkmode" :class="(theme == 'dark') ? 'active' : ''">
				<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g transform="translate(-90 -152)"><rect width="2" height="2" rx="1" transform="translate(103 152) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(103 174) rotate(90)"/><rect width="2" height="2" rx="1" transform="translate(114 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(92 165) rotate(180)"/><rect width="2" height="2" rx="1" transform="translate(111.778 155.636) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(95.05 172.364) rotate(135)"/><rect width="2" height="2" rx="1" transform="translate(93.636 154.222) rotate(45)"/><rect width="2" height="2" rx="1" transform="translate(110.364 170.95) rotate(45)"/><path d="M102,156a8,8,0,1,0,8,8A8,8,0,0,0,102,156Zm-5.336,8A5.343,5.343,0,0,1,102,158.664v10.672A5.343,5.343,0,0,1,96.664,164Z" transform="translate(0 0)"/></g></svg>
			</div>

			<div id="notifications" class="hasTooltip">
				<a href="javascript:void(0)" title="Notifications">
					<span class="loader"></span>
					<span class="count zero">0</span>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M11.877 16.75a.918.918 0 00-.225-.026 1.013 1.013 0 00-.984.783.812.812 0 01-.158.35.6.6 0 01-.467.179h-.1a.579.579 0 01-.469-.156.881.881 0 01-.174-.38 1.008 1.008 0 00-1.988.213.95.95 0 00.022.2A2.518 2.518 0 009.81 20h.155c.055 0 .105.006.148.006a2.529 2.529 0 002.523-2.07.955.955 0 00-.13-.744 1 1 0 00-.629-.442zm6.108-2.332a6.6 6.6 0 00-.479-.527l-.01-.01c-.757-.77-1.696-1.723-1.696-5.181 0-5.52-2.942-6.791-4.207-7.08l-.019-.005h-.01v-.071a1.563 1.563 0 00-3.126 0v.066h-.006l-.019.005C7.148 1.907 4.2 3.18 4.2 8.7c0 3.45-.935 4.405-1.687 5.173a6.06 6.06 0 00-.5.545 1.283 1.283 0 00-.257.968 1.307 1.307 0 00.521.87 1.365 1.365 0 00.81.264h13.852a1.352 1.352 0 00.941-.409 1.291 1.291 0 00.105-1.689zm-2.572.137H4.568a6.173 6.173 0 00.9-1.475 10.979 10.979 0 00.728-4.394 6.858 6.858 0 01.977-3.973 2.783 2.783 0 011.679-1.174 2.63 2.63 0 001.179-.612 2.153 2.153 0 001.09.609A2.768 2.768 0 0112.8 4.7a6.877 6.877 0 01.983 3.986 10.966 10.966 0 00.737 4.409 6.18 6.18 0 00.893 1.46z"/></svg>
				</a>

				<div class="tooltip">
					<span>Notifications</span>
					<p class="zero message">There are no new notifications.</p>
				</div>
			</div>

			<div id="reload" @click="fetchAPI()">
				<svg xmlns="http://www.w3.org/2000/svg" width="20.001" height="20" viewBox="0 0 20.001 20"><g transform="translate(0 0)"><path d="M1.053,11.154A1.062,1.062,0,0,1,0,10.089,9.989,9.989,0,0,1,16.677,2.567l.484-.484a.486.486,0,0,1,.2-.121.541.541,0,0,1,.663.343l1.318,3.748a.522.522,0,0,1,.007.327.5.5,0,0,1-.627.323L18.7,6.7l-3.743-1.32a.531.531,0,0,1-.206-.13.52.52,0,0,1-.016-.733l.464-.465A7.9,7.9,0,0,0,2.092,10.1a1.04,1.04,0,0,1-1.039,1.057Z"/><path d="M18.947,8.844A1.063,1.063,0,0,1,20,9.91,9.989,9.989,0,0,1,3.323,17.434l-.484.484a.476.476,0,0,1-.2.121.541.541,0,0,1-.663-.343L.659,13.948a.522.522,0,0,1-.007-.327.5.5,0,0,1,.627-.323l.022.008,3.743,1.32a.531.531,0,0,1,.206.13.52.52,0,0,1,.016.733l-.464.465A7.9,7.9,0,0,0,17.908,9.9a1.04,1.04,0,0,1,1.039-1.057Z"/></g></svg>
			</div>

			<div id="logout" v-if="loggedIn">
				<a @click="logout()">Logout <svg xmlns="http://www.w3.org/2000/svg" width="10.546" height="10.5" viewBox="0 0 10.546 10.5"><g transform="translate(-30 -181.75)"><path d="M33.92,192h-2.1a1.538,1.538,0,0,1-1.571-1.5v-7a1.538,1.538,0,0,1,1.571-1.5h2.1a.5.5,0,1,1,0,1h-2.1a.515.515,0,0,0-.527.5v7a.515.515,0,0,0,.527.5h2.1a.5.5,0,1,1,0,1Z" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/><path d="M42.157,192.074l1.965-1.965a.525.525,0,0,0,0-.741L42.157,187.4a.524.524,0,0,0-.741.74l1.072,1.071h-3.7a.524.524,0,1,0,0,1.048h3.7l-1.072,1.071a.524.524,0,0,0,.741.74Z" transform="translate(-4.026 -2.739)" fill="#00adb5" stroke="#00adb5" stroke-width="0.5"/></g></svg></a>
			</div>

			<div id="delete" class="hasTooltip">
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
						<a @click="confirmDelete(confirmDeleteName)">DELETE ITEM</a> <a @click="cancelDelete()">CANCEL</a>
					</form>

				</div>
			</div>

			<div id="signup">
				<form id="login" class="form noSubmit">
					<div class="header">
						<h2>Welcome to StackGres!</h2>
					</div>
					<p>To continue, please Log in.</p>

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
					<button @click="login">Login</button>
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
					This operation will create a new <strong>RESTART sgdbop</strong> with the name <strong>restart-{{ restartsCount + 1 }}</strong> and the cluster <strong>{{ restartCluster.name }}</strong> will be restarted as soon as posible.
				</p><br/>
				
				<p><strong>Are you sure you want to proceed?</strong></p><br/>

				<a class="btn" @click="executeClusterRestart()">Restart</a> <a class="btn border" @click="setRestartCluster()">Cancel</a>
			</div>

			<!--<div id="settings">
				<a href="javascript:void(0)" title="Settings" class="nav-item">
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M13.193 10A3.193 3.193 0 1010 13.2a3.2 3.2 0 003.193-3.2zm-1.809 0A1.384 1.384 0 1110 8.614 1.386 1.386 0 0111.384 10z"/><path class="a" d="M16.961 12.835a.443.443 0 01.44-.246 2.6 2.6 0 000-5.2h-.136a.4.4 0 01-.318-.157.988.988 0 00-.055-.164.427.427 0 01.122-.486A2.6 2.6 0 1013.3 2.937a.414.414 0 01-.287.116.4.4 0 01-.292-.12.455.455 0 01-.123-.357 2.591 2.591 0 00-.762-1.84 2.659 2.659 0 00-3.675 0 2.6 2.6 0 00-.76 1.84v.137a.406.406 0 01-.158.318 1.078 1.078 0 00-.163.055.41.41 0 01-.465-.1l-.076-.077a2.5 2.5 0 00-1.853-.729 2.576 2.576 0 00-1.822.8 2.632 2.632 0 00.1 3.71.434.434 0 01.058.5.423.423 0 01-.422.265 2.6 2.6 0 000 5.2h.133a.41.41 0 01.285.117.43.43 0 01-.035.629l-.079.079v.005A2.61 2.61 0 003 17.135a2.479 2.479 0 001.853.728 2.614 2.614 0 001.847-.827.429.429 0 01.5-.057.419.419 0 01.264.42 2.6 2.6 0 105.2 0v-.132a.414.414 0 01.116-.284.421.421 0 01.3-.126.356.356 0 01.278.113l.1.1a2.731 2.731 0 001.852.728 2.6 2.6 0 002.55-2.65 2.611 2.611 0 00-.825-1.857.4.4 0 01-.081-.444zm-6.2 4.422v.143a.691.691 0 01-.69.691.718.718 0 01-.692-.788 2.289 2.289 0 00-1.457-2.095 2.274 2.274 0 00-.919-.2 2.427 2.427 0 00-1.7.728.7.7 0 01-.5.213.652.652 0 01-.482-.194.676.676 0 01-.208-.477.749.749 0 01.217-.53l.064-.064a2.323 2.323 0 00-1.654-3.938H2.6a.692.692 0 01-.489-1.18.755.755 0 01.587-.2A2.286 2.286 0 004.788 7.9a2.306 2.306 0 00-.467-2.556l-.069-.069a.693.693 0 01.478-1.191.655.655 0 01.5.213l.069.071a2.257 2.257 0 002.334.536.92.92 0 00.27-.071 2.312 2.312 0 001.4-2.121v-.134a.687.687 0 01.2-.489.705.705 0 01.977 0 .751.751 0 01.2.571 2.3 2.3 0 00.705 1.64 2.331 2.331 0 001.649.665 2.369 2.369 0 001.652-.713.691.691 0 011.181.488.753.753 0 01-.259.547 2.253 2.253 0 00-.538 2.334.932.932 0 00.072.274 2.313 2.313 0 002.119 1.4h.139a.691.691 0 01.69.692.717.717 0 01-.768.691 2.312 2.312 0 00-2.113 1.395 2.345 2.345 0 00.533 2.619.693.693 0 01-.45 1.192.749.749 0 01-.506-.19l-.1-.1a2.4 2.4 0 00-1.653-.654 2.325 2.325 0 00-2.283 2.312zM5.5 4.177z"/></svg>
				</a>
			</div>-->
		</div>

		<div id="helpTooltip" class="hideOnClick"><vue-markdown :source=tooltipsText :breaks=false></vue-markdown></div>
		<div id="notFound" v-if="loggedIn && notFound">
            <h1>Not Found</h1>
            <p>
                The resource you're looking for doesn't exist,<br/>
                confirm your URL is correct and try again
            </p>
            <br/>
            <router-link to="/" class="btn">Go to default Dashboard</router-link>
        </div>
	</aside>
</template>

<script>
	import store from '../store'
	import router from '../router'
	import axios from 'axios'
	import { mixin } from './mixins/mixin'


    export default {
        name: 'NavBar',

		mixins: [mixin],

		data: function() {
			return {
				pooling: '',
				sgVersion: '',
				loginUser: '',
				loginPassword: '',
				loginPasswordType: 'password',
			}
		},

		computed: {

			namespaces () {
				return store.state.namespaces
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

			loggedIn () {
				if (typeof store.state.loginToken !== 'undefined')
					return store.state.loginToken.length > 0
				else
					return false
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

			nameCollision() {
				let collision = {};

				switch(store.state.cloneCRD.kind) {
					case 'SGClusters':
						collision = store.state.clusters.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGBackupConfigs':
						collision = store.state.backupConfig.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGBackups':
						collision = store.state.backups.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGInstanceProfiles':
						collision = store.state.profiles.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGPoolingConfigs':
						collision = store.state.poolConfig.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGPostgresConfigs':
						collision = store.state.pgConfig.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;

					case 'SGDistributedLogs':
						collision = store.state.logsClusters.find(c => ( (store.state.cloneCRD.data.metadata.namespace == c.data.metadata.namespace) && (store.state.cloneCRD.data.metadata.name == c.name) ))
						break;
				}

				return (typeof collision != 'undefined')
			},

			missingCRDs() {
				let missingCRDs = [];

				if(typeof store.state.cloneCRD.data != 'undefined') {
					let cloneCRD = store.state.cloneCRD.data;
					let cloneKind = store.state.cloneCRD.kind;
					let targetNamespace = cloneCRD.metadata.namespace;


					if (cloneKind == 'SGClusters') {

						let profile = store.state.profiles.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.sgInstanceProfile))
						if (typeof profile == 'undefined')
							missingCRDs.push({kind: 'SGInstanceProfile', name: cloneCRD.spec.sgInstanceProfile})

						let pgconfig = store.state.pgConfig.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.sgPostgresConfig))
						if (typeof pgconfig == 'undefined')
							missingCRDs.push({kind: 'SGPostgresConfig', name: cloneCRD.spec.configurations.sgPostgresConfig})

						let poolconfig = store.state.poolConfig.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.sgPoolingConfig))
						if (typeof poolconfig == 'undefined')
							missingCRDs.push({kind: 'SGPoolingConfig', name: cloneCRD.spec.configurations.sgPoolingConfig})

						if (cloneCRD.spec.configurations.hasOwnProperty('sgBackupConfig')) {
							let backupconfig = store.state.backupConfig.find(p => (p.data.metadata.namespace == targetNamespace) && (p.data.metadata.name == cloneCRD.spec.configurations.sgBackupConfig))
							if (typeof backupconfig == 'undefined')
								missingCRDs.push({kind: 'SGBackupConfig', name: cloneCRD.spec.configurations.sgBackupConfig})
						}
					}
				}

				return missingCRDs
			},

			restartCluster() {
				return store.state.restartCluster
			},

			restartsCount() {
				return store.state.dbOps.filter(op => ( (op.data.spec.op == 'restart') && (op.data.metadata.namespace == store.state.currentNamespace) ) ).length
			}
			
		},

		methods: {

			login: function() {

				const vc = this;

				axios
				.post('/stackgres/auth/login',{
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



			},

			logout: function() {
				document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';
				store.commit('setLoginToken');
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
				else if ( (cloneKind == 'SGClusters') && vc.hasProp(cloneCRD.spec, 'distributedLogs.sgDistributedLogs') && !cloneCRD.spec.distributedLogs.sgDistributedLogs.includes('.') )
					cloneCRD.spec.distributedLogs.sgDistributedLogs = vc.$route.params.namespace + '.' + cloneCRD.spec.distributedLogs.sgDistributedLogs;

				if (!vc.missingCRDs.length) {
					const res = axios
					.post(
						'/stackgres/' + cloneKind.toLowerCase(),
						cloneCRD
					)
					.then(function (response) {
						vc.notify('Resource <strong>"'+store.state.cloneCRD.data.metadata.name+'"</strong> cloned successfully', 'message', store.state.cloneCRD.kind.toLowerCase());
						vc.fetchAPI(cloneKind.toLowerCase());
						vc.cancelClone();
					})
					.catch(function (error) {
						console.log(error.response);
						vc.cancelClone();
						vc.notify(error.response.data,'error',cloneKind.toLowerCase());

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

					const res = axios
					.delete('/stackgres/' + item.kind,
					{
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

						vc.notify('Resource <strong>'+item.name+'</strong> deleted successfully', 'message', item.kind);

						if( (typeof item.redirect !== 'undefined') && item.redirect.length)
							router.push(item.redirect);

						$("#delete").removeClass("active");
						vc.confirmDeleteName = '';
					})
					.catch(function (error) {
						console.log(error);
						vc.notify(error.response.data,'error',item.kind);
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

				let opCount = store.state.dbOps.filter(op => ( (op.data.spec.op == 'restart') && (op.data.metadata.namespace == store.state.currentNamespace) ) ).length + 1;

				let dbOp = {
					metadata: {
						name: 'restart-' + opCount,
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

				axios
				.post(
					'/stackgres/sgdbops', 
					dbOp
				)
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

			vc.pooling = setInterval( function(){
				if(store.state.loginToken.length > 0)
					vc.fetchAPI();
			}.bind(this), 10000);
		}
	}
</script>


<style scoped>
	#notFound {
		width: calc(100vw - 350px);
		margin-left: 350px;
	}

    #notFound h1 {
        font-size: 2rem;
        margin-bottom: 10px;
    }

    #notFound p {
        margin-bottom: 20px;
        font-size: 1rem;
    }

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
</style>
