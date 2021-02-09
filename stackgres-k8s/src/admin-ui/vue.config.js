module.exports = {
    runtimeCompiler: true,
    publicPath: '/admin/',
    lintOnSave: false,
    devServer:{
      proxy: {
        [process.env.VUE_APP_API_URL]: {
          target: process.env.VUE_APP_API_PROXY_URL,
          pathRewrite: { ['^'+process.env.VUE_APP_API_URL]: '' },
          changeOrigin: true,
          secure: false,
          logLevel: 'debug'
        },
      }
    }
  }
  