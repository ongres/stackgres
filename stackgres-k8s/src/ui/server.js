var express = require("express")
var proxy = require('express-http-proxy');
var app = express();

app.use(express.static('public'));

var target = 'https://ongres.mooo.com:8371/stackgres/';

var server = app.listen(8081, function(){
    var port = server.address().port;
    console.log("Server started at http://localhost:%s", port);
});

app.use((req, res, next) => {
    res.set('Cache-Control', 'no-store')
    next()
});

app.use('/stackgres', proxy(target, {
    proxyReqOptDecorator: function(proxyReqOpts, originalReq) {
      proxyReqOpts.rejectUnauthorized = false
      return proxyReqOpts;
    },
    proxyReqPathResolver: function (req) {
        let url = req.url
        url = "/stackgres" + url; 
        return url;
      }
  }));