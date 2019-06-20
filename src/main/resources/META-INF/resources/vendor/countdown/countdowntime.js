(function ($) {
    "use strict";

    $.fn.extend({ 

      countdown100: function(options) {
        var defaults = {
          timeZone: "",
          endtimeYear: 0,
          endtimeMonth: 0,
          endtimeDate: 0,
          endtimeHours: 0,
          endtimeMinutes: 0,
          endtimeSeconds: 0,
        }

        var options =  $.extend(defaults, options);

        return this.each(function() {
          var obj = $(this);
          var timeNow = new Date();

          var tZ = options.timeZone; 
          var endYear = options.endtimeYear;
          var endMonth = options.endtimeMonth;
          var endDate = options.endtimeDate;
          var endHours = options.endtimeHours;
          var endMinutes = options.endtimeMinutes;
          var endSeconds = options.endtimeSeconds;

          if(tZ === "") {
            var deadline = new Date(endYear, endMonth - 1, endDate, endHours, endMinutes, endSeconds); 
          } 
          else {
            var deadline = moment.tz([endYear, endMonth - 1, endDate, endHours, endMinutes, endSeconds], tZ).format(); 
          }

          if(Date.parse(deadline) < Date.parse(timeNow)) {
            var deadline = new Date(Date.parse(new Date()) + endDate * 24 * 60 * 60 * 1000 + endHours * 60 * 60 * 1000); 
          }
          
          
          $(obj).countdown(Date.parse(deadline), function(event) {
            $(this).find('.months').text(event.strftime('%m'));

            if($(this).find('.months').length > 0) {
              $(this).find('.days').text(event.strftime('%n'));
            }
            else {
              $(this).find('.days').text(event.strftime('%D'));
            }
            
            $(this).find('.hours').text(event.strftime('%H'));
            $(this).find('.minutes').text(event.strftime('%M'));
            $(this).find('.seconds').text(event.strftime('%S'));
          });   


        });
      }
    });

    

})(jQuery);