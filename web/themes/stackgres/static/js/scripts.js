// Recaptcha callback function
function rccallback() {
    document.getElementById('formsubmit').removeAttribute('disabled');
    
    if(document.getElementById('privacyTool')!=undefined){ 
        var recap = document.getElementById('recap');
        recap.setAttribute('captcha-verified',true);
        
        if(!document.getElementById('privacyTool').checked){
            document.getElementById('formsubmit').setAttribute('disabled', true);
        }
    }
}

function checkReCapSubmit() { 
    if(document.getElementById('recap') != undefined){ 
        var submitbutton = document.getElementById('formsubmit');
        var recap = document.getElementById('recap');

        if( recap.getAttribute('captcha-verified') == 'false'){
            submitbutton.setAttribute('disabled', true);
        }
    }
}

$(document).ready(function() {

    // Block submit without checking privacy checkbox 
    $('#crmWebToEntityForm form').on('submit',function(e){
        if($('#privacyTool').length && !$('#privacyTool').is(':checked')) {
            e.preventDefault;
            alert('You must accept the Privacy Policy to submit your request');
            return false
        }
    })
    
    // Show recaptcha only when checked
    $('[name="privacyTool"]').on('change',function(){
        if($(this).is(':checked'))
            $(this).parents('form').find('#recaptcha').fadeIn()

        checkReCapSubmit();
    });  

   $('#nav-icon').click(function() {
        $(this).toggleClass('open');
        $(".desktop-menu-container").slideToggle();
   });

    $('.slider.vertical').slick({
        dots: false,
        autoplay: true,
        arrows: false,
        vertical: true
    });

    $('.singleSlider').slick({
        dots: false,
        autoplay: false,
        arrows: false,
        slidesToShow: 1,
        slidesToScroll: 1,
        infinite: true,
    });

    $('.thumbSlider').slick({
        dots: false,
        autoplay: false,
        arrows: true,
        slidesToShow: 2,
        slidesToScroll: 1,
        infinite: true,
    });

    $('.thumbSlider, .singleSlider').slickLightbox({
        src: 'src',
        itemSelector: 'img.border'
      });


    $("#demo .img.lightbox").click(function(){
            $("#lightbox img, #lightbox iframe, #lightbox .subscribed").detach();
            $("#lightbox").append("<img src='"+$(this).attr("src")+"' alt='"+$(this).attr("alt")+"' />");
            $("#lightbox").addClass("show");
    });

    $("#demo .iframe.lightbox").click(function(){
        $("#lightbox img, #lightbox iframe, #lightbox .subscribed").detach();

        var iframeHeight = parseInt((68*$(window).innerHeight())/1450);

        $("#lightbox").append('<iframe src="https://asciinema.org/a/qxpsRvpJdhVvr9gI0CW2FKWF3/embed?rows='+iframeHeight+'&size:20px" id="asciicast-iframe-qxpsRvpJdhVvr9gI0CW2FKWF3" name="asciicast-iframe-qxpsRvpJdhVvr9gI0CW2FKWF3" scrolling="no" allowfullscreen="true"></iframe>');
        $("#lightbox").addClass("show");
    });

    $("#lightbox").click(function(){
        $("#lightbox img, #lightbox .subscribed, #lightbox iframe").detach();
        $(this).removeClass("show");
    });

    function getParameterByName(name, url) {
        if (!url) url = window.location.href;
        name = name.replace(/[\[\]]/g, '\\$&');
        var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, ' '));
    }

    if(getParameterByName('thanks') == '1') {
        $("#lightbox img, #lightbox iframe, #lightbox .subscribed").detach();
        $("#lightbox").append("<div class='subscribed'><h3>Thank you for your interest!</h3><p>Your request was successfully sent.</p><a href='javascript:void(0)' class='btn white'>Close</a></div>");
        $("#lightbox").addClass("show subs");
    }

    $('.tabs ul.titles a').click(function(){
        event.preventDefault()
        $(this).parents('.tabs').find('li.active').removeClass('active')
        $(this).parent().addClass('active')
        $($(this).attr('href')).addClass('active')
    })

    $('.customizeParams input, .customizeParams select').on('change keydown keyup',function() {
            let parent = $(this).parents('li');
            let command = parent.find('.copyValue')
            
            if($(this).hasClass('namespace') && (event.which === 32))
                return false

            if(parent.attr('id') == 'kube-custom') {
                let namespace = parent.find('.namespace').val().length ? 'namespace=' + parent.find('.namespace').val() : ''
                let service = parent.find('.service').val().length ? 'adminui-service-type=' + parent.find('.service').val() : ''
                let grafana = parent.find('.grafana').is(':checked') ? 'grafana-autoEmbed=true' : ''

                command.text('kubectl apply -f \'https://sgres.io/install' + ( (namespace.length || service.length || grafana.length) ? '?' : '') + namespace + (namespace.length && service.length ? ('&' + service) : service) + (grafana.length ? ((namespace.length || service.length ? ('&' + grafana) : grafana)) : '') + '\'')
            } else {
                let namespace = parent.find('.namespace').val()
                let service = parent.find('.service').val()
                let grafana = parent.find('.grafana').is(':checked') ? ' --set grafana.autoEmbed=true ' : ''

                command.text('helm install --create-namespace --namespace ' + ( namespace.length ? namespace : 'stackgres' ) + ' stackgres-operator --set-string adminui.service.type=' + (service.length ? service : 'LoadBalancer') + grafana + ' https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz')
            }
    })

    $('.thumbSlider a.slick-slide').height($('.thumbSlider a.slick-slide:first-child').height())

    $(window).on('resize', function() {
        $('.thumbSlider a.slick-slide:first-child').css('height','auto')
        $('.thumbSlider a.slick-slide').height($('.thumbSlider a.slick-slide:first-child').height())
    })

    $('.copyLink').click(function(){
        event.preventDefault()

        let el = $(this)

        // Check if text isn't being copied already
        if(!el.hasClass('showTooltip')) {
            let copyText = document.getElementById('copyText');
            copyText.value = (window.location.protocol+'//'+window.location.host+location.pathname) + '#' + el.parents('li').prop('id');
            copyText.select();
            copyText.setSelectionRange(0, 99999); /* For mobile devices */
            document.execCommand("copy");

            el.toggleClass('showTooltip')

            setTimeout(function(){
                el.toggleClass('showTooltip')
            },3000)
        }

        return false
    })
    
    $('input#search').on("keyup", searchExt)

    $('.clearSearch svg').on("click", function() {
        $('input#search').val("")
        $('#extensions-table tr.base').removeClass("hidden")
        $('#extensions-table tr.noResults').css("display", "none")
        $('.clearSearch').removeClass('active')
    })

    $('select#pgVersion').on( "change", filterExtensions)

    if($('select#pgVersion').length)
        filterExtensions.apply($('select#pgVersion'))

});

function navToggle() {
    $('.mobileMenu, #header').toggleClass('menuOpen')
}

function toggleDiv( id ) {
    $(id).fadeToggle()
}

function copyValue() {
    let txt = event.target.previousElementSibling;

    // Check if text isn't being copied already
    if(!$(txt).find('.copied').length) {
        let copyText = document.getElementById('copyText');
        copyText.value = $(txt)[0].textContent;
        copyText.select();
        copyText.setSelectionRange(0, 99999); /* For mobile devices */
        document.execCommand("copy");

        $(txt).append('<span class="copied">Copied to Clipboard!</span>')

        setTimeout(function(){
            $('span.copied').fadeOut().remove()
        },3000)
    }
}

function copyUrl() {
    let txt = event.target.parentNode;
    event.preventDefault()

    // Check if text isn't being copied already
    if(!$(txt).hasClass('showTooltip')) {
        let copyText = document.getElementById('copyText');
        copyText.value = $(txt).attr('href');
        copyText.select();
        copyText.setSelectionRange(0, 99999); /* For mobile devices */
        document.execCommand("copy");

        $(txt).toggleClass('showTooltip')

        setTimeout(function(){
            $(txt).toggleClass('showTooltip')
        },3000)
    }

    return false
}

function filterExtensions() {
    var selectedVersion = $(this).val()
    
    if(selectedVersion.length) {
        $('#extensions-table tr.base').each(function(){
            var versions= $(this).find('td.versionTree ul ul li').filter(function(){
                return $(this).text().startsWith(selectedVersion)
            })

            if(versions.length === 0) {
                $(this).addClass("hidden notAvailable")
            } else {
                $(this).removeClass("hidden notAvailable")
                
                var selectedExtVersions = $.map(versions, function(version) {
                    return $(version).parent().parent().parent().children('li:first-of-type').text()
                })

                $(this).find('td.extVersion span').each(function() {
                    var extVersion = $(this).text()
                    
                    var matchedVersions = selectedExtVersions.filter(function(selectedExtVersion) {
                        return selectedExtVersion === extVersion
                    })

                    if(matchedVersions.length === 0)
                        $(this).addClass("hidden")
                    else
                        $(this).removeClass("hidden")
                })
            }
        })
    }

    if($('input#search').val().length)
        searchExt()
}

function searchExt() {
    var searchTerm = $('input#search').val()
    
    $('#extensions-table tr.base:not(.notAvailable)').each(function(){
        if($(this).find('td.extName span:contains('+searchTerm+')').length === 0) {
            $(this).addClass("hidden")
        }else{
            $(this).removeClass("hidden")
        }
    })

    if(!$('tr.base:not(.hidden)').length)
        $('tr.noResults').css("display", "table-row")
    else
        $('tr.noResults').css("display", "none")


    if($('input#search').val().length) 
        $('.clearSearch').addClass('active')
    else
        $('.clearSearch').removeClass('active')
}