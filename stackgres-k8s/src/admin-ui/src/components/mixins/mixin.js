import store from '../../store'

export const mixin = {

    data: function(){
      return {
        confirmDeleteName: ''
      }
    },
    computed: {
  
      loggedIn () {
              if (typeof store.state.loginToken !== 'undefined')
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
  
          currentComponent() {
              return this.$route.name
          }
  
    },
    methods: {
  
      setContentTooltip( el ) {
        $('#contentTooltip .info .content').html($(el).html());
        $('#contentTooltip').addClass('show');
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
  
      iCan( action, kind, namespace = '' ) {
        
        if(namespace.length) {
          var iCan = false;
          
          store.state.permissions.allowed.namespaced.forEach(function( ns ){
            if( (ns.namespace == namespace) && ns.resources.hasOwnProperty(kind) && (ns.resources[kind].includes(action)) ) {
              iCan = true;
              return false
            }
          });
          
          return iCan
        } else {
          return !store.state.permissions.forbidden.includes(kind)
        }
  
      },
  
      iCant ( ) {
        return "<p>You don't have enough permissions to access this resource</p>"
      },
  
      parseParams: function(params) {
        params = params.replace(/=/g, ':</strong> ');
        params = '<li><strong class="label">'+params+'</li>';
        params = params.replace(/(?:\r\n|\r|\n)/g, '</li><li><strong class="label">')
        
        return params;
      },
  
      sort: function(s) {
              
              //if s == current sort, reverse
              if(s === this.currentSort) {
                this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
              }
              this.currentSort = s;
  
          },
  
      /*cancelDelete: function(){
        $("#delete").removeClass("active");
        $("#delete .warning").hide();
        this.confirmDeleteName = '';
        store.commit('setConfirmDeleteName', '');
      },*/
      
      deleteCRD: function( kind, namespace, name, redirect ) {
  
        //console.log("Open delete");
        //$('#delete input').val('');
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
  
          confirmDelete: function( confirmName ) {
  
        const vc = this;
        const item = store.state.deleteItem;
  
              if(confirmName == item.name) { 
                  $("#delete .warning").fadeOut();
  
                  const res = axios
                  .delete(
                    process.env.VUE_APP_API_URL + '//' + item.kind, 
              {
                data: {
                  "metadata": {
                    "name": item.name,
                    "namespace": item.namespace
                  }
                }
              }
                  )
                  .then(function (response) {
                      notify('<span class="capitalize">'+item.kind+'</span> <strong>'+item.name+'</strong> deleted successfully', 'message', item.kind);
            
            $('.'+item.kind+'-'+item.namespace+'-'+item.name).addClass("hide");
            //$('.'+item.kind+'-'+item.namespace+'-'+item.name+'.hide').remove();
            vm.fetchAPI(item.kind);
  
                      if( (typeof item.redirect !== 'undefined') && item.redirect.length)
              router.push(item.redirect);
            
            store.commit("setDeleteItem", {
              kind: '',
              namespace: '',
              name: '',
              redirect: ''
            });
  
            //$("#delete").removeClass("active");
            $("#delete").removeClass("active");
            vc.confirmDeleteName = '';
                  })
                  .catch(function (error) {
                    console.log(error);
            notify(error.response.data,'error',item.kind);
            checkAuthError(error)
                  });
              } else {
                  $("#delete .warning").fadeIn();
              }
              
      },
  
      loadTooltips: function( kind, lang = 'EN' ) {
  
        fetch('/admin/info/sg-tooltips.json')
        .then(response => response.json())
        .then(data => {
  
          var tooltips = data.components.schemas;
          cleanupTooltips(tooltips)
          store.commit('setTooltips', tooltips)
        });
  
      },
  
      showTooltip: function( kind, field ) {
  
        const label = $("[for='"+field+"']").first().text();
        const crd = store.state.tooltips[kind];
  
        $("#help .title").html(label);
  
        let param = crd;
  
        if(field == 'spec.postgresql.conf') {
          param =  crd.spec['postgresql.conf']
        } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
          param =  crd.spec.pgBouncer['pgbouncer.ini']
        } else {
          params = field.split('.');
          params.forEach(function(item, index){
            param = param[item]
          })
        }
  
        store.commit("setTooltipsText", param.description);
  
      },
  
      cloneCRD: function(kind, namespace, name) {
  
        var crd = {};
  
        switch(kind) {
          case 'SGCluster':
            crd = JSON.parse(JSON.stringify(store.state.clusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGBackupConfig':
            crd = JSON.parse(JSON.stringify(store.state.backupConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
  
          case 'SGBackup':
            crd = JSON.parse(JSON.stringify(store.state.backups.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGInstanceProfile':
            crd = JSON.parse(JSON.stringify(store.state.profiles.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPoolingConfig':
            crd = JSON.parse(JSON.stringify(store.state.poolConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPostgresConfig':
            crd = JSON.parse(JSON.stringify(store.state.pgConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGDistributedLogs':
            crd = JSON.parse(JSON.stringify(store.state.logsClusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
        }
        
        if(typeof crd !== 'undefined') {
          crd.kind = kind;
          if($('#cloneName').val() !== crd.data.metadata.name)
            crd.data.metadata.name = 'copy-of-'+crd.data.metadata.name;
  
          store.commit('setCloneCRD', crd);
          
          $('#cloneName').val(crd.data.metadata.name);
          $('#cloneNamespace').val(crd.data.metadata.namespace);
          $("#notifications.hasTooltip.active").removeClass("active");
          $("#notifications.hasTooltip .message.show").removeClass("show");
          $('#clone').fadeIn().addClass('show');
        }
      }
    },
  
    beforeCreate: function() {
      store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.');
    }
}