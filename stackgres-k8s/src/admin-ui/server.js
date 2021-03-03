const history = require('connect-history-api-fallback');
require('dotenv').config()
const express = require("express")
const proxy = require('express-http-proxy');
const app = express();

app.use(history({
  index:"/admin/index.html"
}));
app.use("/admin",express.static('public'));


const target = process.env.SERVER;

const server = app.listen(8081, function(){
    let port = server.address().port;
    console.log("Server started at http://localhost:%s/admin", port);
    console.log("Proxy targeting to %s", target)
});

app.use((req, res, next) => {
    res.set('Cache-Control', 'no-store')
    next()
});

const proxyPaths = ['/stackgres', '/grafana', "/api", "/d", "/public"]

proxyPaths.forEach((proxyPath) => {
  app.use(proxyPath, proxy(target, {
    proxyReqOptDecorator: function(proxyReqOpts) {
      proxyReqOpts.rejectUnauthorized = false
      return proxyReqOpts;
    },
    proxyReqPathResolver: function (req) {
        let url = req.url
        url = proxyPath + url; 
        return url;
      }
  }));
})
