module.exports = {
  runtimeCompiler: true,
  publicPath: '/admin/',
  lintOnSave: false,
  devServer:{
    proxy: {
      '^/stackgres': {
        target: process.env.VUE_APP_API_PROXY_URL,
        pathRewrite: { '^/stackgres' : '' },
        changeOrigin: true,
        secure: false,
        logLevel: 'debug'
      },
      '^/grafana': {
        target: process.env.VUE_APP_GRAFANA_PROXY_URL,
        changeOrigin: true,
        secure: false,
        logLevel: 'debug'
      },
      '^/d/^': {
        target: process.env.VUE_APP_GRAFANA_DASHBOARD_PROXY_URL,
        pathRewrite: { '^/d/^' : '' },
        changeOrigin: true,
        secure: false,
        logLevel: 'debug'
      }
    }
  }
}
