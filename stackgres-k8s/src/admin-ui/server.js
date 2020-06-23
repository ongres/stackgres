require('dotenv').config()
const express = require("express")
const proxy = require('express-http-proxy');
const app = express();

app.use(express.static('public'));

const target = process.env.SERVER;

const server = app.listen(8081, function(){
    let port = server.address().port;
    console.log("Server started at http://localhost:%s", port);
    console.log("Proxy targeting to %s", target)
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

app.use('/grafana', proxy(target, {
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