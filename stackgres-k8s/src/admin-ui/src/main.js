import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import axios from 'axios'

Vue.config.productionTip = false

Vue.use(VueMarkdown);

const vm = new Vue({
  router,
  store,
  render: h => h(App),
  data: {
    active: true,
    ip: '',
    currentCluster: '',
    currentPods: '',
    clustersData: {},
    init: false
    //clusters: []
  },
  methods: {
    

  },
  mounted: function() {

  },

  computed: {

  }
}).$mount('#app')

var apiData = []

var urlParams = new URLSearchParams(window.location.search);

router.beforeEach((to, from, next) => { 

  // If loading CRD from direct URL validate if CRD exists on the API before loading
  if( from.path == '/') {
    const component = to.matched[0].components.default.name;

    if(!checkLogin()) {
      next(); 
      return;
    }

    /* Check if Namespace exist */
    if(to.params.hasOwnProperty('namespace')) {
      
      axios
      .get(process.env.VUE_APP_API_URL + '/namespace')
      .then( function(response){
        store.commit('addNamespaces',response.data)
        if(response.data.includes(to.params.namespace)) {
          store.commit('setCurrentNamespace', to.params.namespace);
        }
        else {
          checkAuthError(error)
          notFound();
        }
      }).catch(function(error) {
        checkAuthError(error)
        notFound()
      });
    }
      
    switch(component) {

      case 'CreateCluster':
      case 'Logs':
      case 'ClusterInfo':
      case 'Grafana':
      case 'ClusterStatus':

        axios
        .get(process.env.VUE_APP_API_URL + '/sgcluster')
        .then( function(response){

          var found = false,
              stats = {};

            if(component == 'ClusterStatus') {
              /* Check for Cluster status */
              axios
              .get(process.env.VUE_APP_API_URL + '/sgcluster/stats/'+to.params.namespace+'/'+to.params.name)
              .then( function(resp){
                stats = resp.data;
              }).catch(function(error) {
                checkAuthError(error)
                notFound()
              });
            } 

          response.data.forEach( function(item, index) {

            var cluster = {
              name: item.metadata.name,
              data: item,
              hasBackups: false,
              status: {},
            };

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) ) {
              cluster.status = stats;
              store.commit('setCurrentCluster', cluster);
              found = true;
            }

            store.commit('updateClusters', cluster);

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
            checkAuthError(error)
          notFound()
        });

        break

      case 'InstanceProfile':
      case 'CreateProfile':
        /* Check if Profile exists */
        axios
        .get(process.env.VUE_APP_API_URL + '/sginstanceprofile')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updateProfiles', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break

      case 'PostgresConfig':
      case 'CreatePgConfig':
        
        /* Check if Postgres Config exists */
        axios
        .get(process.env.VUE_APP_API_URL + '/sgpgconfig')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updatePGConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;

      case 'PoolConfig':
      case 'CreatePoolConfig':

        /* Check if PgBouncer Config exists */
        axios
        .get(process.env.VUE_APP_API_URL + '/sgpoolconfig')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updatePoolConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;
      
      case 'BackupConfig':
      case 'CreateBackupConfig':
        /* Check if BackupConfig Config exists */
        axios
        .get(process.env.VUE_APP_API_URL + '/sgbackupconfig')
        .then( function(response){

          var found = false

          response.data.forEach( function(item, index) {
              
            store.commit('updateBackupConfig', { 
              name: item.metadata.name,
              data: item
            }); 

            if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) )
              found = true;

          });

          if( to.params.hasOwnProperty('name') && !found)
            notFound()
          else
            next()

        }).catch(function(error) {
          checkAuthError(error)
          notFound()
        });

        break;

      case 'Backups':
      case 'CreateBackup':
        /* If filtered by Cluster, first check if Cluster exists */
        if(to.params.hasOwnProperty('cluster')) {

          axios
          .get(process.env.VUE_APP_API_URL + '/sgcluster')
          .then( function(response){
  
            var found = false
  
            response.data.forEach( function(item, index) {
  
              var cluster = {
                name: item.metadata.name,
                data: item,
                hasBackups: false,
                status: {},
              };
                
              store.commit('updateClusters', cluster);
  
              if( to.params.hasOwnProperty('name') && (to.params.name == item.metadata.name) && (to.params.namespace == item.metadata.namespace) ) {
                store.commit('setCurrentCluster', cluster);
                found = true;
              }
  
            });
  
            if( to.params.hasOwnProperty('name') && !found)
              notFound()
            else
              next()
  
          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });

          axios
          .get(process.env.VUE_APP_API_URL + '/sgbackup')
          .then( function(response){ 
            var found = false,
                duration = '';

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                  if( item.status.process.status === 'Completed' ) {
                    //console.log('setting duration');
                    start = moment(item.status.process.timing.start);
                    finish = moment(item.status.process.timing.stored);
                    duration = new Date(moment.duration(finish.diff(start))).toISOString();
                  } 
                } else {
                  //console.log('duration not set');
                  duration = '';
                }
                
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: duration,
                  show: true
                });

                if( to.params.hasOwnProperty('uid') && (to.params.uid == item.metadata.uid) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('uid') && !found) {
              notFound()
            }
            else {
              next()
            }
          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });

        } else {
          
          axios
          .get(process.env.VUE_APP_API_URL + '/sgbackup')
          .then( function(response){

            var found = false,
                duration = ''

            if(response.data.length) {

              response.data.forEach( function(item, index) {
                  
                if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                  if( item.status.process.status === 'Completed' ) {
                    //console.log('setting duration');
                    start = moment(item.status.process.timing.start);
                    finish = moment(item.status.process.timing.stored);
                    duration = new Date(moment.duration(finish.diff(start))).toISOString();
                  } 
                } else {
                  //console.log('duration not set');
                  duration = '';
                }
                
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: duration,
                  show: true
                });

                if( to.params.hasOwnProperty('uid') && (to.params.uid == item.metadata.uid) && (to.params.namespace == item.metadata.namespace) )
                  found = true;

              });
            }

            if( to.params.hasOwnProperty('uid') && !found) {
              notFound()
            }
            else {
              next()
            }

          }).catch(function(error) {
            checkAuthError(error)
            notFound()
          });
        }
        
        break;

    }

  }
  
  // If entering a Cluster, setup as current
  if ( to.params.cluster === "cluster" ) {

    let cluster = store.state.clusters.find(c => ( (to.params.name == c.name) && (to.params.namespace == c.data.metadata.namespace) ) );
    
    if ( typeof cluster !== "undefined" ) {
      
      store.commit('setCurrentCluster', cluster);
    }  else {
      //alert("Not found");
      //window.location.href = '/admin/not-found.html';
    }

    $('.clu li.current').removeClass('current');
	  $('li.cluster-'+store.state.currentNamespace+'-'+store.state.currentCluster.name).addClass('current');
    
  }

  if (to.matched.some(record => record.meta.conditionalRoute)) { 
      // this route requires condition to be accessed
      // if not, redirect to home page.
      //var nav = document.getElementById("nav"); 

      //console.log(to);

	    //console.log(router.history.current);


      if (store.state.currentCluster == {} && ( from.path.includes("profiles") || from.path.includes("configurations") ) && (to.path != ('/admin/configuration/'+to.params.name)) ) { 
          next({ path: '/admin/'}) 
      } else { 
          next() 
      } 
  } else { 
      next() // make sure to always call next()! 
  } 
    
});

// Set "default" as default landing Namespace
if( (window.location.pathname === '/admin/index.html') && ( (store.state.loginToken.length) || (getCookie('sgToken')) )  ) {
  store.commit('setCurrentNamespace', 'default');
  router.push('/overview/default');
}

// Check URL Params
if( urlParams.has('darkmode') || (getCookie('sgTheme') === 'dark') ) {
  console.log('Switching to darkmode');
  store.commit('setTheme', 'dark');
  $('body').addClass('darkmode');
  $('#darkmode').addClass('active');
}

Vue.filter('prettyCRON', function (value) {
  
  //console.log(value);
  return prettyCron.toString(value)
  
});

Vue.filter('formatBytes',function(a){

  // Bytes Formatter via https://stackoverflow.com/questions/15900485/correct-way-to-convert-size-in-bytes-to-kb-mb-gb-in-javascript
  if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c));return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];

});

Vue.filter('prefix',function(s, l = 2){
  return s.substring(0, l);
});


Vue.filter('formatTimestamp',function(t, part){

    if(part == 'date')
      return t.substr(0, 10);
    else if (part == 'time')
      return t.substring( 11, 19);
    else if (part == 'ms') {
      var ms = '.'+t.substring( 20, t.length - 1);
      
      if (ms == '.Z')
        ms = '.000';
      else if (ms.length == 2)
        ms += '00';
      else if (ms.length == 4)
        ms += '0';
      
      return ms.substring(0,4);
    }
      
});

function notFound() {
  store.commit('notFound',true)
  //console.log('notfound')
  router.push('/not-found.html')
}

function checkLogin() {
  let loginToken = getCookie('sgToken');
  //console.log("TOKEN: "+loginToken)

  if (!loginToken.length) {
    if(!store.state.loginToken.length) {
      $('#signup').addClass('login').fadeIn();
      return false;
    } else {
      return true;
    }
  } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
    $('#signup').hide();
    store.commit('setLoginToken', loginToken);
    return true;
  } else if ( urlParams.has('localAPI') ) {
    $('#signup').hide();
    store.commit('setLoginToken', 'localAPI');
    return true;
  }
}


function checkAuthError(error) {
  if(error.response) {
    if(error.response.status == 401 ) {
      document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
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
  
}

function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}

function formatBytes (a) {
  if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c))+1;return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];
}

function arraysMatch (arr1, arr2) {

  // Check if the arrays are the same length
  if (arr1.length !== arr2.length) return false;

  // Check if all items exist and are in the same order
  for (var i = 0; i < arr1.length; i++) {
    if (arr1[i] !== arr2[i]) return false;
  }

  // Otherwise, return true
  return true;

}

function checkData (newData, currentData) {
  return (JSON.stringify(newData) != JSON.stringify(currentData))
}

/*function checkAPI( response, current ){
  if ( !response.length || !current.length ) 
        return false;

    // compare lengths - can save a lot of time 
    if (reponse.length != current.length)
        return false;

    for (var i = 0, l=response.length; i < l; i++) {
        // Check if we have nested arrays
        if (response[i] instanceof Array && current[i] instanceof Array) {
            // recurse into the nested arrays
            if (! checkAPI(response[i], current[i] ) )
                return false;       
        }           
        else if (response[i] != current[i]) { 
            // Warning - two different object instances will never be equal: {x:20} != {x:20}
            return false;   
        }           
    }       
    return true;
}*/

function trimString(string) {
  return string.trim();
}

function getJSON( value ) {
  //e.preventDefault();
  var text = value.split("\n").map(trimString);
  var json = {};

  text.forEach(function(e, i, a){
    var param = e.split("=").map(trimString);
    json[''+param[0].toString()+''] = param[1];
  });

  // alert(json);
  // console.log(json);
  
  return(json);
}

function showFields( fields ) {
  $(fields).slideDown();
}

function hideFields( fields ) {
  $(fields).slideUp();
}

function discoverText(e) {
  var search = $("#keyword").val();
  console.log(search);

  if (/[a-zA-Z]/.test(search) || e.keyCode == "8" || e.keyCode == "46") {
      
      $("table tr").each(function () {

          if ( ($(this).text().toLowerCase().indexOf(search.toLowerCase()) !== -1) )
              $(this).fadeIn();
          else
              $(this).fadeOut();

      });
  }
}

/* jQuery Init */

$(document).ready(function(){

  $("#namespaces li:first-of-type a").click();

  $(document).on("click", "a.namespace", function(){
    store.commit('setCurrentNamespace',$(this).text());
    $("#backup-btn, #graffana-btn").css("display","none");
  });

  $(document).on("click", ".box h4", function() {
    
    $(this).parents(".box").toggleClass("show");

    // Look for toggle button
    var btn = $(this).parents(".table").find(".details .btn");
    
    if(btn.length) {
      if(btn.text() == 'Details')
        btn.text(' Close ');
      else
        btn.text('Details');  
    }
  
  });

  $(document).on("click", ".details .btn", function(){

    $(this).parents(".box").toggleClass("show");

    if($(this).text() == 'Details')
      $(this).text(' Close ');
    else
      $(this).text('Details');
  
  });

  $(document).on("click", "#main, #side", function(e) {

    if($(this).prop("id") != "notifications") {
      $("#notifications.hasTooltip.active").removeClass("active");
      $("#notifications.hasTooltip .message.show").removeClass("show");
      $("#selected--zg-ul-select.open").removeClass("open");
      $("#be-select.active").removeClass("active");
    }

    if (!$(e.target).parents().addBack().is('.cloneCRD') && $('#clone').hasClass('show'))
      $('#clone.show').fadeOut().removeClass('show');

  });
  
  $("#clone").click(function(e){
    e.stopPropagation();
  });


  $(document).on("click", "#sets .nav-item", function(){
   /*  if(!$(this).parents().hasClass("clu"))
        $('.clu.active').removeClass('active');
    
    if(!($(this).parent().hasClass("active"))) {
      $(".set.active:not(.conf)").removeClass("active");
      $(this).parent("div:not(.conf)").addClass("active");
    } */
    $("#current-namespace").removeClass('open');
    $('#ns-select').slideUp();

    $(".set:not(.active) > ul.show").removeClass("show");
    
  });

  $(document).on("click", ".set .item", function(){    
    $(".set:not(.active) > ul.show").removeClass("show");
  });

  $(document).on("mouseover", ".set:not(.active)", function(){
    let offset = $(this).offset();
    let submenu = $(this).children("ul");
    
    if(window.innerHeight > 700)
      submenu.css("bottom","auto").css("top",offset.top - window.scrollY)
    else
      submenu.css("top", "auto").css("bottom",window.innerHeight - $(this).height() - offset.top)
      
    submenu.addClass("show");
  });

  $(document).on("mouseleave", ".set:not(.active) ul.show", function(){
    $(this).removeClass("show");
  });

  $(document).on("mouseleave", ".set:not(.active)", function(){
    $(this).children("ul.show").removeClass("show");
  });

  $(document).on("click", "#nav:not(.disabled) .top a.nav-item", function(){
    $(".clu a[href$='"+store.state.currentCluster+"']").addClass("router-link-active");
  });

  $(".expand").click(function(){
    $(".set").addClass("active");
    $("#sets").addClass("expanded");
  });

  $(".collapse").click(function(){
    $(".set").removeClass("active");
    $("#sets").removeClass("expanded");
  });

  $("#nav .view").click(function(){
    $("#nav .tooltip.show").prop("class","tooltip").hide();
    $("#nav .top a.nav-item").removeClass("router-link-active");
  });

  $("#nav.disabled .top a.nav-item").click(function(){
      $("#nav .tooltip.show").prop("class","tooltip").hide();
      $(this).siblings(".tooltip").fadeIn().addClass("show");
      $("#nav .top .tooltip").addClass("pos"+($(this).index()+1));
  });

  $(".hasTooltip > a").click(function(){
    if($(this).parent().hasClass("active")) {
      $(this).parent().removeClass("active");
      $(this).parent().find("div.message").removeClass("show");
    }
    else{
      $(".hasTooltip.active").removeClass("show");
      $(this).parent().addClass("active");
      $(this).parent().find("div.message").addClass("show");
    }      
  });

  /* Disable Grafana KEY functions */
  $(".grafana iframe").contents().find("body").keyup( function(e) {
    switch (e.keyCode) {
      case 27: // 'Esc'
        event.returnValue = false;
        event.keyCode = 0;
        alert("ESC");
        break;
    }
  });


  $.fn.ulSelect = function(){
    var ul = $(this);

    if (!ul.hasClass('zg-ul-select'))
      ul.addClass('zg-ul-select');
    
    $('li:first-of-type', this).addClass('active');

    var selected = $('#selected--zg-ul-select');
      
    
    $(document).on('click', '#selected--zg-ul-select', function(){

      $(this).toggleClass('open');
      ul.toggleClass('active');

      var selectedText = $(this).text();
      if (ul.hasClass('active')) {
        selected.addClass('active');
      }
      else {
        //selected.text('').removeClass('active'); 
        $('li.active', ul);
      }
    });

    $(document).on('click', '#be-select li a', function(){
      selected.removeClass('open');
      ul.removeClass('active');
      $(".set.backups.active").removeClass('active');
    });

  }

  // Run
  $('#be-select').ulSelect();

  // Uncheck checkboxes by default
  //$('.switch input[type="checkbox"]').removeAttr('checked');

  $(document).on('click', '.message .remove', function(){
    $(this).parent().detach();
   
    $("#notifications .count").text(parseInt($("#notifications .count").text(),10)-1);
    
    if(parseInt($("#notifications .count").text(),10) == 0)
      $("#notifications .tooltip .zero").show();
  });

  $("form").submit(function(e){
    e.preventDefault(); 
  });

  $(document).on("click", ".sort th span:not(.helpTooltip)", function(){
    $(".sorted").removeClass("sorted");
    $(this).addClass("sorted");
    $(".sort th").toggleClass("desc asc")   
  });

  $(document).mouseup(function(e) {
    var container = $(".filter.open");

    // if the target of the click isn't the container nor a descendant of the container
    if (!container.is(e.target) && container.has(e.target).length === 0) {
      if( $('.filter.open').find('.active').length )
        $('.filter.open').addClass('filtered');
      else
        $('.filter.open').removeClass('filtered');
      
        $('.filter.open').removeClass("open");
    }
  });
 
  $(document).on("click",".toggle:not(.date)",function(e){
    e.stopPropagation();
    $(this).parent().toggleClass("open");
    //$(this).next().toggleClass("open");
  })

  $(document).on("click", "#current-namespace , #ns-select a", function(){
    $("#current-namespace").toggleClass("open");
    $("#ns-select").slideToggle();
    $(".set.active:not(.conf)").removeClass('active');
  });

  $("#darkmode").click(function(){
    $("body").toggleClass("darkmode");
    $(this).toggleClass("active");

    if($("body").hasClass("darkmode")) {
      store.commit('setTheme', 'dark');
      
      if($('#grafana').length)
        $('#grafana').attr("src", $('#grafana').attr("src").replace('light','dark'));
    }
    else {
      store.commit('setTheme', 'light');

      if($('#grafana').length)
        $('#grafana').attr("src", $('#grafana').attr("src").replace('dark','light'));
    }

    
  });

  $(document).on("click","[data-active]", function(){
    $($(this).data("active")).addClass("active");
  });

  $(document).on("click",".openConfig", function(){
    $(this).toggleClass('open');
    $(this).parents('tr').toggleClass('open');
    $(this).parents('tr').find('.parameters').toggleClass('open');
  });

  $(document).on('click', 'ul.select .selected', function(){
    $(this).parent().toggleClass('active');
  });

  $(document).on('click', '.set > .addnew', function(){
    if(!$(this).parent().hasClass('active')) {
      $('.set.active:not(.conf)').removeClass('active');
      $('.set ul.show').removeClass('show');
      $(this).parent().addClass('active');
    }
  });

  $('form.noSubmit').on('submit',function(e){
    e.preventDefault
  });

  onmousemove = function (e) {

    if( (window.innerWidth - e.clientX) > 420 ) {
      $('#nameTooltip, #infoTooltip').css({
        "top": e.clientY+20, 
        "right": "auto",
        "left": e.clientX+20
      })
    } else {
      $('#nameTooltip, #infoTooltip').css({
        "top": e.clientY+20, 
        "left": "auto",
        "right": window.innerWidth - e.clientX + 20
      })
    }
  }
  
  $(document).on('mouseenter', 'td.hasTooltip', function(){
    const c = $(this).children('span');
    if(c.width() > $(this).width()){
      $('#nameTooltip .info').text(c.text());
      $('#nameTooltip').addClass('show');
    }
      
  });

  $(document).on('mouseleave', 'td.hasTooltip', function(){ 
    $('#nameTooltip .info').text('');
    $('#nameTooltip').removeClass('show');
  });

  $(document).on('click','[data-tooltip]', function(e){
    if( (window.innerWidth - e.clientX) > 420 ) {
      $('#helpTooltip').css({
        "top": e.clientY+10, 
        "right": "auto",
        "left": e.clientX+10
      })
    } else {
      $('#helpTooltip').css({
        "top": e.clientY+10, 
        "left": "auto",
        "right": window.innerWidth - e.clientX + 10
      })
    }

    if(!$(this).hasClass('show')) {
      store.commit('setTooltipsText', $(this).data('tooltip'))
      $('.helpTooltip.show').removeClass('show')
      $('#helpTooltip').addClass('show').show()
    } else {
      store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.')
      $('#helpTooltip').removeClass('show').hide()
    }

    $(this).toggleClass('show')  
  })

  $(document).on("click", "#helpTooltip a", function(e) {
    e.preventDefault()
    window.open($(this).prop('href'));
    return false;
  })

  $(document).on('click','a.help', function(){
    $('a.help.active').removeClass('active')
    $(this).addClass('active')
  })

  $('#contentTooltip .close').click(function(){
    $('#contentTooltip').removeClass('show');
    $('#contentTooltip .info .content').html('');
  })

  $(document).on("click", "#side", function(e) {

    if($('#contentTooltip').hasClass('show')) {
      $('#contentTooltip').removeClass('show')
      $('#contentTooltip .content').html('');
    }
  });

 /*  onmousemove = function (e) {

    if( (window.innerWidth - e.clientX) > 420 ) {
      $('#helpTooltip:not(.show)').css({
        "top": e.clientY+20, 
        "right": "auto",
        "left": e.clientX+20
      })
    } else {
      $('#helpTooltip:not(.show)').css({
        "top": e.clientY+20, 
        "left": "auto",
        "right": window.innerWidth - e.clientX + 20
      })
    }
  } */

  // Hide divs on click out
  $(document).click(function(event) { 
    var $target = $(event.target);
    
    if( $('.hideOnClick.show').length && !$target.closest('.show').length) {
      $('.hideOnClick.show').removeClass('show').fadeOut()
      $('.helpTooltip.show').removeClass('show')
    }
  });

  $(window).on('scroll', function(){
    $('#helpTooltip.show').removeClass('show').fadeOut()
    $('.helpTooltip.show').removeClass('show')
  })

});
