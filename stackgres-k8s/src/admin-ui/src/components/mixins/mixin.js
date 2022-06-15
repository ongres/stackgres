import store from '../../store'
import sgApi from '../../api/sgApi'
import router from '../../router'
import VueMarkdown from 'vue-markdown'
import moment from 'moment'

export const mixin = {

    data: function(){
      return {
        confirmDeleteName: '',
        pagination: {
          amount: 0,
          rows: 999,
          start: 0,
          end: 999,
          current: this.$route.query.hasOwnProperty('page') ? parseInt(this.$route.query.page) : 1,
          singleLoaded: false
        }
      }
    },
    components: {
      VueMarkdown
    },
    computed: {
  
      loggedIn () {
        if (store.state.hasOwnProperty('loginToken'))
          return store.state.loginToken.length > 0
        else
          return false
      },

      notFound () {
        return store.state.notFound
      },

      isReady () {
        return store.state.ready
      },

      tooltips () {
        return store.state.tooltips
      },

      iCanLoad() {
        const vc = this;

        let kind = (vc.$route.meta.componentName + 's').toLowerCase();

        return ( vc.loggedIn && vc.isReady && !vc.notFound && 
          ( 
            ( vc.$route.name.startsWith('Create') && vc.iCan('create', kind, vc.$route.params.namespace) ) || 
            ( vc.$route.name.startsWith('Edit') && vc.iCan('patch', kind, vc.$route.params.namespace) ) ||
            ( ( !vc.$route.name.startsWith('Edit') && !vc.$route.name.startsWith('Create') ) && vc.iCan('list', kind, vc.$route.params.namespace) )
          )
        )
      }
  
    },
    methods: {
      
      cancel: function() {
        const vc = this
        
        if(window.history.length > 2)
          vc.$router.go(-1)
        else
          vc.$router.push('/' + vc.$route.params.namespace )
      },

      checkAuthError: function(error) {
        let kind = '';

        if(error.response) {
          if(error.response.status == 401 ) {
            document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';
            store.commit('setLoginToken');
            window.location.replace('/admin/')
            process.exit(1);
          } else if(error.response.status == 403 ) {
            
            // Little hack to store the right plural kind to validate RBAC permissions
            if(error.response.config.url.includes('sgcluster/stats'))
              kind = 'stats'
            else
              kind = error.response.config.url.replace('/stackgres/','')+'s';
            
            store.commit('setNoPermissions', kind);
          }
        }
      },      

      fetchAPI  (kind = '') {

        const vc = this

        let loginToken = ("; "+document.cookie).split("; sgToken=").pop().split(";").shift();
  
        if(store.state.loginToken.search('Authentication Error') !== -1) {
          clearInterval(this.pooling);
        } else if (!loginToken.length) {
          if(!store.state.loginToken.length) {
            $('#signup').addClass('login').fadeIn();
            return false;
          }
        } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
          $('#signup').hide();
          store.commit('setLoginToken', loginToken);
        }
  
        $('#reload').addClass('active');
  
        // Read and set user permissions first
        sgApi
        .get('can_i')
        .then( function(response) {
          store.commit('setPermissions', response.data);
        })
        .then( function() {

          if ( vc.iCan('list', 'namespaces') && ( !kind.length || (kind == 'namespaces') ) ) {
            /* Namespaces Data */
            sgApi
            .get('namespaces')
            .then( function(response){
    
              if(vc.$route.params.hasOwnProperty('namespace') && !response.data.includes(vc.$route.params.namespace)) {
                router.push('/')
                vc.notify('The namespace you were browsing has been deleted from the server')
              }
              store.commit('addNamespaces', response.data);
  
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'sgclusters') && ( !kind.length || (kind == 'sgclusters') ) ){
            /* Clusters Data */
            sgApi
            .get('sgclusters')
            .then( function(response){
  
              vc.lookupCRDs('sgclusters', response.data);
    
              response.data.forEach( function(item, index) {
  
                var cluster = {
                  name: item.metadata.name,
                  data: item,
                  hasBackups: false,
                  status: {}
                };
                
                if(!store.state.namespaces.includes(item.metadata.namespace))
                  store.commit('updateNamespaces', item.metadata.namespace);
  
                store.commit('updateClusters', cluster);
  
                sgApi
                .getResourceDetails('sgclusters', cluster.data.metadata.namespace, cluster.data.metadata.name, 'stats')
                .then( function(resp) {
                  store.commit('updateClusterStats', {
                    name: cluster.data.metadata.name,
                    namespace: cluster.data.metadata.namespace,
                    stats: resp.data
                  })
                }).catch(function(err) {
                  console.log(err);
                });
  
                // Set as current cluster if no other cluster has already been set
                if(!store.state.currentCluster)              
                  store.commit('setCurrentCluster', cluster);
  
              });
              
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
    
          }
    
          if ( vc.iCan('list', 'sgbackups') && ( !kind.length || (kind == 'sgbackups') )) {
            
            /* Backups */
            sgApi
            .get('sgbackups')
            .then( function(response) {
  
              vc.lookupCRDs('sgbackups', response.data);
    
                var start, finish, duration;
    
                response.data.forEach( function(item, index) {
                  
                  if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                    store.commit('updateNamespaces', item.metadata.namespace);
    
                  if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                    if( item.status.process.status === 'Completed' ) {
                      start = moment(item.status.process.timing.start);
                      finish = moment(item.status.process.timing.stored);
                      duration = new Date(moment.duration(finish.diff(start))).toISOString();
                    } else {
                      duration = '';
                    }
                    
                  }
  
                  if(!index)
                    store.commit('flushResource', 'sgbackups')
                    
                  store.commit('updateBackups', { 
                    name: item.metadata.name,
                    data: item,
                    duration: duration,
                    show: true
                  });
    
                });
    
                store.state.sgclusters.forEach(function(cluster, index){
                  let backups = store.state.sgbackups.find(b => ( (cluster.name == b.data.spec.sgCluster) && (cluster.data.metadata.namespace == b.data.metadata.namespace) ) );
          
                  if ( typeof backups !== "undefined" )
                    cluster.hasBackups = true; // Enable/Disable Backups button
    
                });
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'sgpgconfigs') && (!kind.length || (kind == 'sgpgconfigs') ) ){
    
            /* PostgreSQL Config */
            sgApi
            .get('sgpgconfigs')
            .then( function(response) {
  
              vc.lookupCRDs('sgpgconfigs', response.data);
    
              response.data.forEach( function(item, index) {
                  
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushResource', 'sgpgconfigs')
                  
                store.commit('updatePGConfig', { 
                  name: item.metadata.name,
                  data: item
                });
  
              });
  
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('get', 'sgpoolconfigs') && ( !kind.length || (kind == 'sgpoolconfig') ) ){
    
            /* Connection Pooling Config */
            sgApi
            .get('sgpoolconfigs')
            .then( function(response) {
  
              vc.lookupCRDs('sgpoolconfigs', response.data);
    
              response.data.forEach( function(item, index) {
                  
                  if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                    store.commit('updateNamespaces', item.metadata.namespace);
                  
                  if(!index)
                    store.commit('flushResource', 'sgpoolconfigs')
                    
                  store.commit('updatePoolConfig', { 
                    name: item.metadata.name,
                    data: item
                  });
    
                });
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'sgbackupconfigs') && ( !kind.length || (kind == 'sgbackupconfigs')) ) {
    
            /* Backup Config */
            sgApi
            .get('sgbackupconfigs')
            .then( function(response) {
  
              vc.lookupCRDs('sgbackupconfigs', response.data);
    
              response.data.forEach( function(item, index) {
                
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushResource', 'sgbackupconfigs')
                  
                store.commit('updateBackupConfig', { 
                  name: item.metadata.name,
                  data: item
                });
  
              });
        
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'sginstanceprofiles') && (!kind.length || (kind == 'sginstanceprofiles') ) ) {
    
            /* Profiles */
            sgApi
            .get('sginstanceprofiles')
            .then( function(response) {
  
              vc.lookupCRDs('sginstanceprofiles', response.data);
    
              response.data.forEach( function(item, index) {
                  
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushResource', 'sginstanceprofiles')
  
                store.commit('updateProfiles', { 
                  name: item.metadata.name,
                  data: item
                });
    
              });
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'storageclasses') && ( !kind.length || (kind == 'storageclasses') )) {
            /* Storage Classes Data */
            sgApi
            .get('storageclasses')
            .then( function(response){
    
              store.commit('addStorageClasses', response.data);
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
    
          if ( vc.iCan('list', 'sgdistributedlogs') && ( !kind.length || (kind == 'sgdistributedlogs') ) ){
            /* Distributed Logs Data */
            sgApi
            .get('sgdistributedlogs')
            .then( function(response){
  
              vc.lookupCRDs('sgdistributedlogs', response.data);
    
              var logs = [];
    
              response.data.forEach(function(item, index){
                logs.push({
                  name: item.metadata.name,
                  data: item
                })
              })
  
              store.commit('addLogsClusters', logs);
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }
  
          if ( vc.iCan('list', 'sgdbops') && ( !kind.length || (kind == 'sgdbops') ) ){
            /* DbOps Data */
            sgApi
            .get('sgdbops')
            .then( function(response){
  
              vc.lookupCRDs('sgdbops', response.data);
    
              var dbOps = [];
    
              response.data.forEach(function(item, index){
                dbOps.push({
                  name: item.metadata.name,
                  data: item
                })
              })
  
              store.commit('addDbOps', dbOps);
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }

          if ( vc.iCan('list', 'sgobjectstorages') && ( !kind.length || (kind == 'sgobjectstorages') ) ){
            /* Distributed Logs Data */
            sgApi
            .get('sgobjectstorages')
            .then( function(response){
  
              vc.lookupCRDs('sgobjectstorages', response.data);
    
              var sgobjectstorages = [];
    
              response.data.forEach(function(item, index){
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushResource', 'sgobjectstorages')
  
                store.commit('updateObjectStorages', { 
                  name: item.metadata.name,
                  data: item
                });
              })
  
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          }

        })
        .catch(function(err) {
          console.log(err);
          vc.checkAuthError(err);
        });

        if(!Object.keys(store.state.tooltips).length && !store.state.tooltips.hasOwnProperty('error')) {
          fetch('/admin/info/sg-tooltips.json')
          .then(response => response.json())
          .then(data => {
            var tooltips = data.components.schemas;
            vc.cleanupTooltips(tooltips)
            store.commit('setTooltips', tooltips)
          })
          .catch((error) => {
            console.log(error);
            
            $('body').addClass('noTooltips')
            
            vc.notify({
              title: 'INFO', 
              detail: 'Information tooltips could not be loaded properly, help texts won\'t be available. <br/><br/>This does not affect the usability of the web console. You can always refer to the <a href="https://stackgres.io/doc" target="_blank">official StackGres Documentation</a> for more information.',
              type: 'https://stackgres.io/doc'
            }, 'error')

            store.commit('setTooltips', {error: 'Information tooltips could not be loaded'})
          });
        }

        if(!Object.keys(store.state.postgresVersions).length) {

          let pgFlavors = {'vanilla': {}, 'babelfish': {}};

          Object.keys(pgFlavors).forEach(function(flavor) {
            /* Postgres versions */
            sgApi
            .getPostgresVersions(flavor)
            .then( function(response){

              let postgresVersions = {};

              response.data.postgresql.forEach(function(version) {
                let major = version.split('.')[0];
                
                if(!postgresVersions.hasOwnProperty(major)) {
                  postgresVersions[major] = [];
                }
                
                postgresVersions[major].push(version);
              })
  
              pgFlavors[flavor] = postgresVersions;
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          });

          store.commit('setPostgresVersions', pgFlavors);

        }

        if(!store.state.applications.length) {

          /* Get SG Applications */
          sgApi
          .get('applications')
          .then( function(response) {

            store.commit('setApplications', response.data.applications);
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });

        }

        if(!store.state.ready)
          store.commit('setReady',true)
  
        setTimeout(function(){
          $("#reload").removeClass("active");
          this.init = true;
        }, 2000);

      },
  
      setContentTooltip( el = '', warning = false ) {
        const tooltip = `<div class="contentTooltip show">
          <div class="close"></div>
          <div class="info` + (warning ? ' warning' : '') + `">
            <span class="close">CLOSE</span>
            <div class="content">` + $(el).html() + `</div>
          </div>
        </div>`;

        $('#main').append(tooltip);
      },
  
      helpTooltip(kind, field) {
        const crd = store.state.tooltips[kind];
        let param = crd;
  
        if(field == 'spec.postgresql.conf') {
          param =  crd.spec['postgresql.conf']
        } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
          param =  crd.spec.pgBouncer['pgbouncer.ini']
        } else {
          params = field.split('.');
          params.forEach(function(item, index){
            if( !index ) // First level
              param = param[item]
            else if (param.type == 'object')
              param = param.properties[item]
            else if (param.type == 'array')
              param = param.items.properties[item]
          })
        }
        
        store.commit('setTooltipsText', param.description)
        $('#helpTooltip').show()
      },
  
      hasProp (obj, propertyPath) {
        if(!propertyPath)
            return false;
  
        var properties = propertyPath.split('.');
  
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
  
            if(!obj || !obj.hasOwnProperty(prop)){
                return false;
            } else {
                obj = obj[prop];
            }
        }
  
        return true;
      },
  
      iCan( action = 'any', kind, namespace = '' ) {
        const vc = this;
          
          if(namespace.length) { // If filtered by namespace
  
            let permissions = store.state.permissions.allowed.namespaced.find(p => (p.namespace == namespace));
            return (
              (typeof permissions != 'undefined') &&
                (
                  ( (action == 'any') && permissions.resources[kind].length) ||
                  ( (action != 'any') && permissions.resources[kind].includes(action) 
                )
              )
            )

          } else if( !['namespaces', 'storageclasses'].includes(kind) && (action != 'any') ) { // For CRDs when no namespace indicated
            
            return (store.state.permissions.allowed.namespaced.filter(n => 
              (n.resources[kind].includes(action)) ).length == store.state.permissions.allowed.namespaced.length
            );

          } else if(['namespaces', 'storageclasses'].includes(kind)) {

            return store.state.permissions.allowed.unnamespaced[kind].includes(action)
          
          } else {
            
            return !store.state.permissions.forbidden.includes(kind)
            
          }
  
      },
  
      iCant ( ) {
        return "<p>You don't have enough permissions to access this resource</p>"
      },
  
      sort: function(param, type = 'alphabetical') {
              
        if(param === this.currentSort.param) {
          this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
        }
        this.currentSort.param = param;
        this.currentSort.type = type;

      },
  
      deleteCRD: function( kind, namespace, name, redirect ) {
  
        this.confirmDeleteName = '';
        $("#delete").addClass("active");
        $(".filter > .open").removeClass("open");
  
        store.commit("setDeleteItem", {
          kind: kind,
          namespace: namespace,
          name: name,
          redirect: redirect
        });
  
      },

      notify (message, type = 'message', crd = 'general') {

        let msgExists = store.state.notifications.messages.find( m =>
          m.show && (m.kind == crd) && (m.level == type) && (m.message.content == message)
        );

        // Show only if there are no active notifications with the exact same info
        if(typeof msgExists == 'undefined') {

          let now = new Date();
          let msg = {
            kind: crd,
            level: type,
            message: {
              content: message.hasOwnProperty('title') ? message.title : message,
              ...(message.hasOwnProperty('detail') && {details: message.detail}),
              ...(message.hasOwnProperty('type') && {link: message.type}),
            },
            timestamp: now,
            show: true
          };

          $('#nav .active:not(#notifications').removeClass('active');
          $('.form .alert').removeClass('alert');

          store.commit('addNotification', msg);

          if(message.hasOwnProperty('fields')) {
            message.fields.forEach( function(item, index) {
              $("[data-field='"+item+"']").addClass("notValid");
            });
          }
        }
        
      },
  
      showTooltip: function( kind, field, customTooltip = '' ) {
  
        const label = $("[for='"+field+"']").first().text();
        $("#help .title").html(label);

        // If it's a custom tooltip
        if(customTooltip.length) {
          
          store.commit("setTooltipsText", customTooltip);

        } else { // Get tooltips from CRDs
  
          const crd = store.state.tooltips[kind];
          let param = crd;
    
          if(field == 'spec.postgresql.conf') {
            param =  crd.spec['postgresql.conf']
          } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
            param =  crd.spec.pgBouncer['pgbouncer.ini']
          } else {
            let params = field.split('.');
            params.forEach(function(item, index){
              param = param[item]
            })
          }
    
          store.commit("setTooltipsText", param.description);
        }
  
      },
  
      cloneCRD: function(kind, namespace, name) {
  
        var crd = {};
  
        switch(kind) {
          case 'SGClusters':
            crd = JSON.parse(JSON.stringify(store.state.sgclusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            
            if(this.hasProp(crd, 'data.spec.initialData.restore')) {
              delete crd.data.spec.initialData.restore

              if(!Object.keys(crd.data.spec.initialData).length)
                delete crd.data.spec.initialData
            }

            break;
          
          case 'SGBackupConfigs':
            crd = JSON.parse(JSON.stringify(store.state.sgbackupconfigs.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
  
          case 'SGBackups':
            crd = JSON.parse(JSON.stringify(store.state.sgbackups.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGInstanceProfiles':
            crd = JSON.parse(JSON.stringify(store.state.sginstanceprofiles.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPoolingConfigs':
            crd = JSON.parse(JSON.stringify(store.state.sgpoolconfigs.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPostgresConfigs':
            crd = JSON.parse(JSON.stringify(store.state.sgpgconfigs.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGDistributedLogs':
            crd = JSON.parse(JSON.stringify(store.state.sgdistributedlogs.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
        }
        
        if(typeof crd !== 'undefined') {
          crd.kind = kind;
          crd.data.metadata.name = crd.name = 'copy-of-'+crd.data.metadata.name;
          store.commit('setCloneCRD', crd);

          $('#cloneName').val(crd.data.metadata.name);
          $('#cloneNamespace').val(crd.data.metadata.namespace);
        }
      },

      cleanupTooltips( obj ) {
        const vc = this;

        Object.keys(obj).forEach(function(key){
          if(obj[key].hasOwnProperty('type')) {
            if((obj[key].type != 'array') && obj[key].hasOwnProperty('properties')){
              var desc = ''
              
              if(obj[key].hasOwnProperty('description'))
                desc = obj[key].description
        
              obj[key] = obj[key].properties
              obj[key].description = desc
              vc.cleanupTooltips(obj[key])
              
            } else if ( (obj[key].type == 'array') && obj[key].items.hasOwnProperty('properties')){
              Object.keys(obj[key].items.properties).forEach(function(k){
                obj[key][k] = obj[key].items.properties[k]
              })      
            } 
          }
        })
      },

      hasParams( obj, params) {
        var valid = true;
      
        for ( var i=0; i < params.length; i++){
          if( !obj || !obj.hasOwnProperty(params[i]) ) {
            return valid = false
          }
          else
            obj = obj[params[i]]
        }
      
        return valid
      },      

      // Sort tables by an specific parameter
      sortTable( table, param, direction, type = 'alphabetical' ) {
        const vc = this;

        var backupFixedParams = ['data.spec.subjectToRetentionPolicy','data.metadata.name','data.spec.sgCluster','data.status.process.status'];
        
        table.sort((a,b) => {
          let modifier = 1;
          
          if(direction === 'desc') modifier = -1;

          // If sorting backups first validate its state
          if(a.data.hasOwnProperty('status') && a.hasOwnProperty('duration')) {
            // If record is not sortable by the provided param
            if(a.data.status !== null) {
              if( (a.data.status.process.status == 'Failed') && !backupFixedParams.includes(param)){
                return 1
              } else if ((a.data.status.process.status == 'Running') && !backupFixedParams.includes(param)){
                return -1
              }
            }
          } 

          if (param == 'data.status.conditions') { // If dbOps sorted by Status

            if(a.data.hasOwnProperty('status')) {
              a = a.data.status.conditions.find(c => (c.status === 'True') )
              a = a.type
            } else {
              a = '-'
            }

            if(b.data.hasOwnProperty('status')) {
              b = b.data.status.conditions.find(c => (c.status === 'True') )
              b = b.type
            } else {
              b = '-'
            }

          } else if (param == 'data.spec.runAt' ) { // If dbOps sorted by runAt
            
            a = a.data.spec.hasOwnProperty('runAt') ? a.data.spec.runAt : (vc.hasProp(a, 'data.status.opStarted') ? a.data.status.opStarted : '-');
            b = b.data.spec.hasOwnProperty('runAt') ? b.data.spec.runAt : (vc.hasProp(b, 'data.status.opStarted') ? b.data.status.opStarted : '-');

          } else if (param == 'data.status.elapsed' ) { // If dbOps sorted by elapsed
            
            if( a.data.hasOwnProperty('status') ) {
              let lastStatus = a.data.status.conditions.find(c => (c.status === 'True') )
              let begin = moment(a.data.status.opStarted)
              let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
              a = moment.duration(finish.diff(begin));
            } else {
              a = -1
            }

            if( b.data.hasOwnProperty('status') ) {
              let lastStatus = b.data.status.conditions.find(c => (c.status === 'True') )
              let begin = moment(b.data.status.opStarted)
              let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
              b = moment.duration(finish.diff(begin));
            } else {
              b = -1
            }

          } else { // Every other param

            if(vc.hasParams( a, param.split(".")))
              a = eval("a."+param);
            else
              a = '';

            if(vc.hasParams( b, param.split(".")))
              b = eval("b."+param);
            else
              b = '';

          }

          switch(type) {

            case 'timestamp':

              if(moment(a).isValid && moment(b).isValid) {

                if(moment(a).isBefore(moment(b)))
                  return -1 * modifier;
              
                if(moment(a).isAfter(moment(b)))
                  return 1 * modifier;  

              } else if (!moment(a).isValid && moment(b).isValid) {
                return -1 * modifier;
              } else if (moment(a).isValid && !moment(b).isValid) {
                return 1 * modifier;
              }
              
              break;

            case 'memory':
              
              if( vc.getBytes(a) < vc.getBytes(b) )
                return -1 * modifier;
              
              if( vc.getBytes(a) > vc.getBytes(b) )
                return 1 * modifier;
              
              break;
            
            default: // alphabetical, duration, cpu

              if(type == 'cpu') {
                a = a.includes('m') ? (parseFloat(a.replace('m','')/1000)) : a;
                b = b.includes('m') ? (parseFloat(b.replace('m','')/1000)) : b;
              }
                          
              if( a < b )
                return -1 * modifier;
              
              if( a > b )
                return 1 * modifier;
              
                break;
            
          }
          
          return 0;
        });

        return table;

      },

      getBytes (text) {
        var powers = {'Ki': 1, 'Mi': 2, 'Gi': 3, 'Ti': 4, 'Pi': 5};
        var regex = /(\d+(?:\.\d+)?)\s?(Ki|Mi|Gi|Ti|Pi)?b?/i;
        var res = regex.exec(text);
        
        return ( (res !== null) && (typeof res[2] !== 'undefined') ) ? (res[1] * Math.pow(1024, powers[res[2]]) ) : text;
      },
      
      hasProp(obj, propertyPath){
        if(!propertyPath)
            return false;
      
        var properties = propertyPath.split('.');
      
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
      
            if(!obj || !obj.hasOwnProperty(prop)){
                return false;
            } else {
                obj = obj[prop];
            }
        }
      
        return true;
      },

      goTo(path) {
        var $target = $(event.target);

        if( !$target.closest('td.actions').length) {
        
          if(window.location.href.endsWith(path)) // Already in resource, go to parent page
            router.push(path.substring(0, path.lastIndexOf("/") + 1))
          else
            router.push(path)

        }
      },

      getTooltip(field) {
        const vc = this;

        if(Object.keys(vc.tooltips).length) {
          if(field == 'sgpostgresconfig.spec.postgresql.conf') {
            return vc.tooltips.sgpostgresconfig.spec['postgresql.conf'].description
          } else if (field == 'sgpoolingconfig.spec.pgBouncer.pgbouncer.ini') {
            return vc.tooltips.sgpoolingconfig.spec.pgBouncer['pgbouncer.ini'].description
          } else if (field == 'sginstanceprofile.spec.hugePages.hugepages-1Gi') {
            return vc.tooltips.sginstanceprofile.spec.hugePages['hugepages-1Gi'].description
          } else if (field == 'sginstanceprofile.spec.hugePages.hugepages-2Mi') {
            return vc.tooltips.sginstanceprofile.spec.hugePages['hugepages-2Mi'].description
          } else if (vc.hasProp(vc.tooltips, field)) {
            let params = field.split('.');
            let tooltipText = vc.tooltips;

            params.forEach(function(item, index){
              tooltipText = tooltipText[item]
            })
            return tooltipText.description
          } else {
            return 'Information not available'
          }
        } else {
          return 'Information not available'
        }
        
      },

      tzCrontab( baseCrontab, toLocal = true ) {

        if( !!moment().utcOffset() && (store.state.timezone == 'local') ) {    

          let crontab = baseCrontab.split(' ');
          
          const isParsable = function(n) {
            try {
              var t = parseInt(n);
              return Number.isInteger(t)
            } catch(err) {
                return false
            }
          }

          if(isParsable(crontab[1])) {
            
            let tzOffset = (moment().utcOffset() / 60);      
            let dom = crontab[2];
            let dow = crontab[4];
            let modifier = 0;

            crontab[1] = parseInt( crontab[1] ) + ( tzOffset * ( toLocal ? 1 : -1 ) ); // Set opposite offset if converting to UTC

            if(!crontab[1].isInteger) {
              
              // Fix minutes offset if in timezone with 30/45min offsets
              if (isParsable(crontab[0])) {
                let minOffset = crontab[1] % 1;

                if(crontab[0].includes('-')) {
                  crontab[0] = (parseInt(crontab[0].split('-')[0]) + (minOffset * 60)) + '-' + (parseInt(crontab[0].split('-')[1]) + (minOffset * 60) );
                } else if(crontab[0].includes('/')) {
                  crontab[0] = (parseInt(crontab[0].split('/')[0]) + (minOffset * 60)) + '/' + (parseInt(crontab[0].split('/')[1]) + (minOffset * 60) );
                } else  {
                  crontab[0] = (parseInt(crontab[0] + (minOffset * 60)) );
                }
              }

              crontab[1] = parseInt( crontab[1] )
            }

            // Fix hour offset on 24h edges
            if(crontab[1] < 0) {
              modifier = -1
              crontab[1] = crontab[1] + 24
            } else if(crontab[1] >= 24) {
              modifier = 1
              crontab[1] = crontab[1] - 24
            }
            
            if(dom.includes('-')) {
              crontab[2] = (parseInt(dom.split('-')[0]) + modifier) + '-' + (parseInt(dom.split('-')[1]) + modifier )
            } else if (dom.includes('/')) {
              crontab[2] = (parseInt(dom.split('/')[0]) + modifier) + '/' + (parseInt(dom.split('/')[1]) + modifier )
            } else if (isParsable(dom)) {
              crontab[2] = (parseInt(dom) + modifier)
      
              // Fix DOM offset on month edges
              if(crontab[2] < 1) {

                var month = crontab[3];
                // Offset month 
                if(isParsable(month) && parseInt(month) > 1) { 
                  crontab[3] = (parseInt(crontab[3]) - 1)
                } else { // Jan > Dec
                  crontab[3] = 12
                }
      
                // Offset day of month
                if( [1,3,5,7,8,10,12].includes(crontab[3]) ) { // Jan, Mar, May, Jul, Aug, Oct, Dec
                  crontab[2] = 31
                } else if ([4,6,9,11].includes(crontab[3])) { // Apr, Jun, Sept, Nov
                  crontab[2] = 30
                } else if (crontab[3] == 2) { // Feb
                  crontab[2] = 28
                }
      
              } else if (
                (crontab[2] > 31) || 
                ((crontab[2] > 30) && (['4','6','9','11'].includes(crontab[3]))) ||
                ((crontab[2] > 28) && (crontab[3] == '2')) ) {
                
                  // Offset month 
                  if(parseInt(crontab[3]) < 12) { 
                    crontab[3] = (parseInt(crontab[3]) + 1);
                  } else { // Dec > Jan
                    crontab[3] = 1
                  }
                  
                  crontab[2] = 1;
      
              }
      
            }
      
            if(!isParsable(dow) && dow.includes('-')) {
              crontab[4] = (parseInt(dow.split('-')[0]) + modifier) + '-' + (parseInt(dow.split('-')[1]) + modifier )
            } else if (dow.includes('/')) {
              crontab[4] = (parseInt(dow.split('/')[0]) + modifier) + '/' + (parseInt(dow.split('/')[1]) + modifier )
            } else if (dow !== '*') {
              crontab[4] = (parseInt(dow) + modifier)
            }
              
            baseCrontab = crontab.join(' ')
          }
      
        } 

        return baseCrontab
      },

      showTzOffset() {
        if( !!moment().utcOffset() && (store.state.timezone == 'local') ) {
          var offset = new Date().getTimezoneOffset(), o = Math.abs(offset);
          return (offset < 0 ? "+" : "-") + ("00" + Math.floor(o / 60)).slice(-2) + ":" + ("00" + (o % 60)).slice(-2);
        } else {
          return '+00:00'
        }
      },

      getIsoDuration(duration) {
        let d = (duration.split('P').pop().split('D')[0] != '0') ? duration.split('P').pop().split('D')[0] : ''
        let h = (duration.split('T').pop().split('H')[0] != '0') ? duration.split('T').pop().split('H')[0] : ''
        let m = (duration.split('H').pop().split('M')[0] != '0') ? duration.split('H').pop().split('M')[0] : ''
        let s = (duration.split('M').pop().split('S')[0] != '0') ? duration.split('M').pop().split('S')[0] : ''

        return (
            (d.length ? (d + ' day' + ( (d != '1') ? 's' : '' ) ) : '') +
            (h.length ? ( (d.length ? ', ' : '') + (h + ' hour' + ( (h != '1') ? 's' : '' ) ) ) : '') +
            (m.length ? ( ( (d.length || h.length) ? ', ' : '') + (m + ' minute' + ( (m != '1') ? 's' : '' ) ) ) : '') +
            (s.length ? ( ( (d.length || h.length || m.length) ? ', ' : '') + (s + ' second' + ( (s != '1') ? 's' : '' ) ) ) : '')
        )
      },

      lookupCRDs(kind, crds) {
        const vc = this;

       store.state[kind].forEach(function(item, index) {

          let foundItem = crds.find(e => (e.metadata.name == item.data.metadata.name) && (e.metadata.namespace == item.data.metadata.namespace))

          if(typeof foundItem == 'undefined') {

            store.commit('removeResource', {
              kind: kind,
              name: item.data.metadata.name,
              namespace: item.data.metadata.namespace,
            })

            if(vc.$route.params.hasOwnProperty('name') && (item.data.metadata.name == vc.$route.params.name)) {

              switch(kind) {
          
                case 'clusters':
                  router.push('/' + item.data.metadata.namespace + '/sgclusters')
                  break;
                
                default:
                  router.push('/' + item.data.metadata.namespace + '/' + kind);
                  break;
              }

              vc.notify('The resource you were browsing has been deleted from the server')
            } else if ((kind == 'backups') && vc.$route.params.hasOwnProperty('backupname') && (item.data.metadata.name == vc.$route.params.backupname) ) {
              
              if(vc.$route.params.hasOwnProperty('name')) {
                router.push('/' + item.data.metadata.namespace + '/sgcluster/' + vc.$route.params.name + '/sgbackups')
              } else {
                router.push('/' + item.data.metadata.namespace + '/sgbackups')
              }
              vc.notify('The resource you were browsing has been deleted from the server')

            }  

          }

        })
      },

      // Pagination handle
      pageChange(pInfo){
        const vc = this;
        
        vc.pagination.start = pInfo.pageSize * (pInfo.pageNumber-1);
        vc.pagination.end = vc.pagination.start + pInfo.pageSize;
        
      },

      setRestartCluster(namespace = '', name = '') {
        store.commit('setRestartCluster', {
          namespace: namespace, 
          name: name,
          restartName: 'op' + this.getDateString()
        })
      },

      checkRequired() {
        const vc = this;
        let isValid = true;
        
        $('[required]').each(function() {
          if ( !$(this).val() || ($(this).is(':checkbox') && !$(this).is(':checked')) ) {
            $(this).addClass("notValid");

            if($(this).is(':checkbox')) {
              $(this).parent('label').addClass('notValid');
            }
            isValid = false;
          } else if ($(this).hasClass('error')) {
            $(this).removeClass('notValid');
          }
        });

        if(!isValid && ($('#notifications .message.show.title').text != 'Please fill every mandatory field in the form') ) {
          setTimeout(function() {
            vc.notify('Please fill every mandatory field in the form', 'message', 'general');
          }, 100);

          vc.checkValidSteps(vc._data, 'submit')
        }

        return isValid
      },

      splitUppercase(text) {
        return text.split(/(?=[A-Z])/).join(' ')
      },
      
      getDateString() {
        let fullDate = new Date();
        let timeString = '';
        let dayString = '';
        let localeMonth = fullDate.getMonth()+1;
        
        if (store.state.timezone == 'local') {
          timeString = fullDate.toLocaleTimeString([], { hour12: false });
          dayString = fullDate.getFullYear() + '-' + localeMonth.toString().padStart(2, '0') + '-' + fullDate.getDate().toString().padStart(2, '0');
        } else {
          timeString = fullDate.getUTCHours() + ':' + fullDate.getUTCMinutes() + ':' + fullDate.getUTCSeconds()
          dayString = fullDate.toISOString().slice(0, fullDate.toISOString().indexOf('T'));
        }
          
        return dayString + '-' + timeString.replaceAll(':','-')
      }, 
            
      checkValidSteps(data, source) {
        if( (source == 'submit') || ( (source == 'steps') && (data.errorStep.length)) ) {
          $('fieldset[data-fieldset]').each(function() {
            let fieldset = $(this);
            let fieldsetAttr = fieldset.attr('data-fieldset'); 
            let notValidFields = fieldset.find('.notValid');
  
          
            if(notValidFields.length) {
              if(!data.errorStep.includes(fieldsetAttr))
                data.errorStep.push(fieldsetAttr);
            } else {
              if(data.errorStep.includes(fieldsetAttr)) {
                for( var i = 0; i < data.errorStep.length; i++) {
                  if (data.errorStep[i] === fieldsetAttr) { 
                    data.errorStep.splice(i, 1); 
                    break;
                  }
                }
              }
            }
          })
        }
      },

      isEnabled(spec, reversed = false) {
        if((spec && !reversed) || (!spec && reversed))
          return 'Enabled'
        else
          return 'Disabled'
      }
            
    },
  
    beforeCreate: function() {
      store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.');
    },

    mounted: function() {
      const vc = this;

      $(window).on('resize', function() {
        vc.pagination.rows = parseInt(($(window).innerHeight() - 480)/40)
        vc.pagination.rows = (vc.pagination.rows <= 0) ? 1 : vc.pagination.rows
        vc.pageChange({
          pageNumber: 1,
          pageSize: vc.pagination.rows
        })
      });
      $(window).resize()

      // Setup currentPath for sidebar use
      store.commit('setCurrentPath', {
        namespace: vc.$route.params.hasOwnProperty('namespace') ? vc.$route.params.namespace : '',
        name: vc.$route.params.hasOwnProperty('name') ? vc.$route.params.name : '',
        component: vc.$route.name.length ? vc.$route.name : ''
      });

      // Allow API fetching from child browser tabs
      window.fetchParentAPI = function(kind) {
        vc.fetchAPI(kind);
      }

    },

    updated: function(){
      const vc = this

      // Little hack to set container height
      vc.$nextTick(function () {
        if( (typeof vc.$refs.page != 'undefined') && $('.v-pagination').length && !$('.content').hasClass('withPagination'))
          $('.content').addClass('withPagination')
        else if (!$('.v-pagination').length)
          $('.content').removeClass('withPagination')
      })
    }
}