import axios from 'axios';

const baseURL = '/stackgres';

const resources = {
  can_i: '/auth/rbac/can-i',
  login: 'auth/login',
  namespaces: '/namespaces',
  sgclusters: '/sgclusters',
  sginstanceprofiles: '/sginstanceprofiles',
  sgbackups: '/sgbackups',
  sgpgconfigs: '/sgpgconfigs',
  sgpoolconfigs: '/sgpoolconfigs',
  sgbackupconfigs: '/sgbackupconfigs',
  sgdistributedlogs: '/sgdistributedlogs',
  sgdbops: '/sgdbops',
  storageclasses: '/storageclasses',
  extensions: '/extensions',
  applications: '/applications'
};

export default {
    
  get(resource) {
    return axios.get(baseURL + resources[resource])
  },

  create(resource, data) {
    return axios.post(baseURL + resources[resource], data)
  },

  update(resource, data) {
    return axios.put(baseURL + resources[resource], data)
  },

  delete(resource, data) {
    return axios.delete(baseURL + resources[resource], data)
  },

  getResourceDetails(resource, namespace, name, details = '', query = '') {
    return axios.get(baseURL + '/namespaces/' + namespace + resources[resource] + '/' + name + (details.length ? ('/' + details + query) : '') )
  },

  getCustomResource(endpoint) {
    return axios.get(baseURL + endpoint)
  },

  createCustomResource(endpoint, data) {
    return axios.post(baseURL + endpoint, data)
  },

  getPostgresVersions(flavor) {
    return axios.get(baseURL + '/version/postgresql?flavor=' + flavor)
  },

  getPostgresExtensions(version, flavor = 'vanilla') {
    return axios.get(baseURL + '/extensions/' + version + '?flavor=' + flavor )
  },

}
