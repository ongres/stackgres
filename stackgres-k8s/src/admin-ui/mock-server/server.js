/*
 * StackGres admin-ui mock API server.
 *
 * A zero-dependency-on-the-app Express server that answers the StackGres REST
 * API from static JSON fixtures, so the web console can run locally without a
 * real operator/backend.
 *
 * The Vue dev server proxies `^/stackgres` to this server, rewriting away the
 * `/stackgres` prefix (see vue.config.js). So the paths handled here are the
 * un-prefixed ones: `/auth/type`, `/sgclusters`, `/namespaces/:ns/...`, etc.
 *
 * Run standalone:   node mock-server/server.js          (listens on :8090)
 * Run with the UI:  npm run dev:mock                     (see mock-server/start.js)
 */

const express = require('express');
const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, 'data');

// ---------------------------------------------------------------------------
// Fixture loading
// ---------------------------------------------------------------------------

// Read a fixture file by basename (without extension). Returns `fallback` when
// the file is missing so a half-populated data dir never crashes the server.
function load(name, fallback) {
  const file = path.join(DATA_DIR, name + '.json');
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (err) {
    if (err.code !== 'ENOENT') {
      console.error(`[mock] failed to parse data/${name}.json:`, err.message);
    }
    return fallback;
  }
}

// Namespaced CRD resources exposed as `GET /<resource>` (array of CRD objects).
const NAMESPACED_RESOURCES = [
  'sgclusters', 'sgshardedclusters', 'sgstreams', 'sginstanceprofiles',
  'sgbackups', 'sgpgconfigs', 'sgpoolconfigs', 'sgobjectstorages',
  'sgscripts', 'sgdistributedlogs', 'sgdbops',
];

// Kinds the UI probes via iCan() for a namespace. Every one MUST be present in
// each namespace's `resources` map or the UI throws on `.includes(...)`.
const NAMESPACED_KINDS = NAMESPACED_RESOURCES.concat([
  'secrets', 'configmaps', 'pods', 'events', 'persistentvolumeclaims',
  'roles', 'rolebindings',
]);

// Cluster-scoped kinds probed via iCan() without a namespace.
const UNNAMESPACED_KINDS = [
  'namespaces', 'storageclasses', 'clusterroles', 'clusterrolebindings',
  'sgconfigs', 'customresourcedefinitions',
];

const ALL_VERBS = ['list', 'get', 'create', 'update', 'patch', 'delete'];

// ---------------------------------------------------------------------------
// Generated responses
// ---------------------------------------------------------------------------

// Build a permissive `auth/rbac/can-i` response: full access to every kind in
// every known namespace, plus all cluster-scoped kinds.
function buildPermissions(namespaces) {
  const resources = {};
  NAMESPACED_KINDS.forEach((k) => { resources[k] = ALL_VERBS.slice(); });

  const unnamespaced = {};
  UNNAMESPACED_KINDS.forEach((k) => { unnamespaced[k] = ALL_VERBS.slice(); });

  return {
    namespaced: namespaces.map((namespace) => ({ namespace, resources })),
    unnamespaced,
  };
}

// Synthesize a `/stats` payload for a cluster from its pod list, so we don't
// have to author a stats fixture per cluster.
function buildStats(clusterItem) {
  const pods = (clusterItem && clusterItem.pods) || [];
  const statPods = pods.map((p, i) => ({
    name: p.name,
    role: p.role,
    status: 'Active',
    ip: '10.244.0.' + (10 + i),
    cpuRequested: '1',
    cpuPsiAvg60: (0.05 + i * 0.01).toFixed(2),
    memoryRequested: '2.00Gi',
    memoryPsiAvg60: '0.00',
    diskRequested: '10Gi',
    diskUsed: (1.2 + i * 0.1).toFixed(2) + 'Gi',
    diskPsiAvg60: '0.00',
    containers: '5',
    containersReady: '5',
  }));

  return {
    cpuRequested: String(pods.length || 0),
    memoryRequested: (2 * (pods.length || 0)).toFixed(2) + 'Gi',
    diskRequested: '10Gi',
    diskUsed: '1.20Gi',
    podsReady: pods.length,
    averageLoad1m: '0.12',
    cpuPsiAvg60: '0.05',
    memoryPsiAvg60: '0.00',
    diskPsiAvg60: '0.00',
    pods: statPods,
  };
}

// Sharded cluster `/stats`: a coordinator section (first member cluster) and
// a workers section (the rest), each aggregating the member pods.
function buildShardedStats(shardedItem, memberLookup) {
  const clusters = (shardedItem && shardedItem.status && shardedItem.status.clusters) || [];
  const section = (names) => {
    const pods = [];
    names.forEach((name) => {
      const member = memberLookup(name);
      if (member) pods.push(...buildStats(member).pods);
    });
    return {
      cpuRequested: String(pods.length || 0),
      cpuPsiAvg60: '0.05',
      memoryRequested: (2 * (pods.length || 0)).toFixed(2) + 'Gi',
      memoryPsiAvg60: '0.00',
      diskRequested: 10 * (names.length || 0) + 'Gi',
      diskUsed: '1.20Gi',
      diskPsiAvg60: '0.00',
      averageLoad1m: '0.12',
      connections: '5',
      podsReady: pods.length,
      pods,
    };
  };
  return {
    metadata: shardedItem ? shardedItem.metadata : {},
    coordinator: section(clusters.slice(0, 1)),
    workers: section(clusters.slice(1)),
  };
}

// ---------------------------------------------------------------------------
// Server
// ---------------------------------------------------------------------------

function createApp() {
  const app = express();
  app.use(express.json({ limit: '5mb' }));

  // Permissive CORS (harmless behind the dev proxy, handy for direct curling).
  app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Authorization,Content-Type,X-Requested-With');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    console.log(`[mock] ${req.method} ${req.originalUrl}`);
    next();
  });

  const namespaces = load('namespaces', ['default']);
  const requestsLog = [];

  // ----- Grafana (served by nginx at the console origin in production) -----
  // /grafana-list returns [{name, url}] with same-origin dashboard URLs; the
  // /d/:uid/:slug placeholder stands in for an embedded Grafana dashboard so
  // the monitoring iframe shows something in mock mode.
  const GRAFANA_DASHBOARDS = [
    'archiving', 'connection-pooling', 'current-activity', 'db-info',
    'db-objects', 'db-os', 'queries', 'replication',
  ];
  app.get('/grafana-list', (req, res) => {
    res.json(GRAFANA_DASHBOARDS.map((name) => ({ name, url: `/d/mock-${name}/${name}` })));
  });
  app.get('/d/:uid/:slug', (req, res) => {
    res.type('html').send(
      `<body style="font-family: sans-serif; background: ${
        req.query.theme === 'dark' ? '#111' : '#eee'
      }; color: ${req.query.theme === 'dark' ? '#eee' : '#111'};">` +
        `<h1>Grafana dashboard: ${req.params.slug}</h1>` +
        `<p>instance=${req.query['var-instance'] || ''} pod=${req.query['var-pod'] || ''}</p>` +
        `<p>from=${req.query.from || ''} to=${req.query.to || ''}</p></body>`
    );
  });

  // ----- Auth -----
  app.get('/auth/type', (req, res) => res.json(load('auth-type', { type: 'JWT' })));
  app.post('/auth/login', (req, res) => res.json({ access_token: 'mock-jwt-token' }));
  app.get('/auth/logout', (req, res) => res.sendStatus(200));
  app.get('/auth/rbac/can-i', (req, res) => res.json(buildPermissions(namespaces)));

  // ----- Postgres versions & extensions -----
  app.get('/version/postgresql', (req, res) => {
    const versions = load('postgres-versions', { vanilla: [], babelfish: [] });
    const flavor = req.query.flavor || 'vanilla';
    res.json({ postgresql: versions[flavor] || [] });
  });
  app.get('/extensions/:version', (req, res) => {
    res.json(load('extensions', { extensions: [], publishers: [] }));
  });

  // ----- Simple list/object resources -----
  app.get('/namespaces', (req, res) => res.json(namespaces));
  app.get('/storageclasses', (req, res) => res.json(load('storageclasses', ['standard'])));
  app.get('/applications', (req, res) => res.json(load('applications', { applications: [] })));
  let sgconfigs = null;
  const sgconfigsItems = () => {
    if (!sgconfigs) sgconfigs = load('sgconfigs', []);
    return sgconfigs;
  };
  app.get('/sgconfigs', (req, res) => res.json(sgconfigsItems()));
  app.put('/sgconfigs', (req, res) => {
    const body = req.body;
    requestsLog.push({ method: 'PUT', resource: 'sgconfigs', dryRun: req.query.dryRun === 'true', body });
    const index = sgconfigsItems().findIndex((c) => c.metadata.name === body.metadata.name);
    if (index < 0) return notFound(res, 'sgconfigs', body.metadata && body.metadata.name);
    if (req.query.dryRun !== 'true') sgconfigsItems()[index] = body;
    res.json(body);
  });
  app.get('/users', (req, res) => res.json(load('users', [])));
  app.get('/roles', (req, res) => res.json(load('roles', [])));
  app.get('/clusterroles', (req, res) => res.json(load('clusterroles', [])));

  // ----- Resource detail: /namespaces/:ns/:resource/:name/:detail? -----
  // `stats` is synthesized; an explicit detail fixture (data/details/<res>/<detail>.json)
  // wins if present; no detail returns the single CRD; unknown details return [].
  app.get('/namespaces/:ns/:resource/:name/:detail?', (req, res) => {
    const { ns, resource, name, detail } = req.params;
    // reads the mutable store so created/updated resources resolve too
    const list = NAMESPACED_RESOURCES.includes(resource) ? items(resource) : load(resource, []);
    const item = Array.isArray(list)
      ? list.find((r) => r.metadata && r.metadata.name === name && r.metadata.namespace === ns)
      : null;

    if (!detail) {
      return item ? res.json(item) : notFound(res, resource, name);
    }
    if (detail === 'stats') {
      if (resource === 'sgshardedclusters') {
        return res.json(buildShardedStats(item, (member) =>
          items('sgclusters').find(
            (c) => c.metadata.name === member && c.metadata.namespace === ns
          )
        ));
      }
      return res.json(buildStats(item));
    }
    const explicit = load(path.join('details', resource, detail), null);
    if (explicit !== null) {
      if (detail === 'logs' && Array.isArray(explicit)) {
        return res.json(filterLogs(explicit, req.query));
      }
      return res.json(explicit);
    }
    return res.json([]); // logs, events, etc. — empty by default
  });

  // ----- Generic CRD list endpoints -----
  // Fixture lists are copied into an in-memory store so the write endpoints
  // below can mutate them; a restart reloads the fixtures.
  const store = {};
  const items = (resource) => {
    if (!store[resource]) {
      store[resource] = load(resource, []);
    }
    return store[resource];
  };
  NAMESPACED_RESOURCES.forEach((resource) => {
    app.get('/' + resource, (req, res) => res.json(items(resource)));
  });

  // ----- Generic CRD write endpoints -----
  // POST/PUT/DELETE mirror the REST API shape the console uses: the body is
  // the full resource, ?dryRun=true validates without persisting, and every
  // request is appended to /requests-log so tests can assert exact payloads.
  app.get('/requests-log', (req, res) => res.json(requestsLog));
  const RFC1123 = /^[a-z0-9]([-a-z0-9]*[a-z0-9])?$/;
  const findIndexOf = (resource, body) =>
    items(resource).findIndex(
      (r) =>
        r.metadata.name === body.metadata.name &&
        r.metadata.namespace === body.metadata.namespace
    );
  // Minimal semantics of the real config endpoints: a POST/PUT response (dry
  // run included) carries a status with the StackGres default parameters and
  // the resolved configuration, which the console's "Default Parameters"
  // tables read.
  const PG_DEFAULT_PARAMETERS = {
    autovacuum_max_workers: '3',
    checkpoint_completion_target: '0.9',
    checkpoint_timeout: '15min',
    default_statistics_target: '200',
    effective_cache_size: '512MB',
    log_checkpoints: 'on',
    maintenance_work_mem: '64MB',
    max_connections: '100',
    password_encryption: 'scram-sha-256',
    random_page_cost: '1.5',
    shared_buffers: '128MB',
    wal_compression: 'lz4',
    work_mem: '4MB',
  };
  const PGBOUNCER_DEFAULT_PARAMETERS = {
    default_pool_size: '50',
    ignore_startup_parameters: 'extra_float_digits',
    max_client_conn: '1000',
    max_db_connections: '0',
    max_user_connections: '0',
    pool_mode: 'session',
  };
  const parseParamsBlob = (blob) => {
    const result = {};
    String(typeof blob === 'string' ? blob : '').split('\n').forEach((line) => {
      const match = line.match(/^\s*([^=#\s]+)\s*=\s*(.*?)\s*$/);
      if (match) result[match[1]] = match[2].replace(/^'(.*)'$/, '$1');
    });
    return result;
  };
  const resolveParams = (defaults, overrides, withDocs) => {
    const merged = Object.entries(defaults).map(([parameter, value]) => ({
      parameter,
      value: Object.prototype.hasOwnProperty.call(overrides, parameter)
        ? overrides[parameter]
        : value,
      ...(withDocs && {
        documentationLink: 'https://postgresqlco.nf/doc/en/param/' + parameter + '/',
      }),
    }));
    Object.entries(overrides).forEach(([parameter, value]) => {
      if (!Object.prototype.hasOwnProperty.call(defaults, parameter)) {
        merged.push({ parameter, value });
      }
    });
    return merged;
  };
  const attachConfigStatus = (resource, item) => {
    if (resource === 'sgpgconfigs') {
      const overrides = parseParamsBlob(item.spec && item.spec['postgresql.conf']);
      item.status = {
        ...(item.status || {}),
        defaultParameters: PG_DEFAULT_PARAMETERS,
        'postgresql.conf': resolveParams(PG_DEFAULT_PARAMETERS, overrides, true),
      };
    } else if (resource === 'sgpoolconfigs') {
      const overrides = parseParamsBlob(
        item.spec && item.spec.pgBouncer && item.spec.pgBouncer['pgbouncer.ini']
      );
      item.status = {
        ...(item.status || {}),
        pgBouncer: {
          defaultParameters: PGBOUNCER_DEFAULT_PARAMETERS,
          'pgbouncer.ini': resolveParams(PGBOUNCER_DEFAULT_PARAMETERS, overrides, false),
        },
      };
    }
  };

  // Flat RBAC collections (users, roles, clusterroles): same store-backed
  // CRUD as the namespaced kinds, addressed by name (+ namespace when the
  // body carries one).
  ;['users', 'roles', 'clusterroles'].forEach((resource) => {
    const matches = (candidate, body) =>
      candidate.metadata.name === body.metadata.name &&
      (body.metadata.namespace === undefined ||
        candidate.metadata.namespace === body.metadata.namespace ||
        resource === 'users');
    app.get('/' + resource, (req, res) => res.json(items(resource)));
    app.post('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'POST', resource, dryRun: req.query.dryRun === 'true', body });
      if (!body.metadata || !RFC1123.test(body.metadata.name || '')) {
        return res.status(422).json({ message: 'Invalid name', detail: 'RFC 1123' });
      }
      if (items(resource).some((candidate) => matches(candidate, body))) {
        return res.status(409).json({
          message: `${resource} "${body.metadata.name}" already exists`,
          detail: 'AlreadyExists',
        });
      }
      if (req.query.dryRun !== 'true') {
        items(resource).push(body);
      }
      res.json(body);
    });
    app.put('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'PUT', resource, dryRun: req.query.dryRun === 'true', body });
      const index = items(resource).findIndex((candidate) => matches(candidate, body));
      if (index < 0) {
        return notFound(res, resource, body.metadata && body.metadata.name);
      }
      if (req.query.dryRun !== 'true') {
        items(resource)[index] = body;
      }
      res.json(body);
    });
    app.delete('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'DELETE', resource, dryRun: req.query.dryRun === 'true', body });
      const index = items(resource).findIndex((candidate) => matches(candidate, body));
      if (index < 0) {
        return notFound(res, resource, body.metadata && body.metadata.name);
      }
      if (req.query.dryRun !== 'true') {
        items(resource).splice(index, 1);
      }
      res.sendStatus(204);
    });
  });

  // Namespace creation (the real endpoint takes the name in the path and no
  // body, answering 204).
  app.post('/namespaces/:name', (req, res) => {
    const { name } = req.params;
    requestsLog.push({
      method: 'POST',
      resource: 'namespaces',
      dryRun: req.query.dryRun === 'true',
      body: { name },
    });
    if (!RFC1123.test(name)) {
      return res.status(422).json({
        message: 'Invalid name',
        detail: 'a lowercase RFC 1123 label must consist of lower case alphanumeric characters',
      });
    }
    if (req.query.dryRun !== 'true' && !namespaces.includes(name)) {
      namespaces.push(name);
    }
    res.sendStatus(204);
  });

  NAMESPACED_RESOURCES.forEach((resource) => {
    app.post('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'POST', resource, dryRun: req.query.dryRun === 'true', body });
      if (!body.metadata || !RFC1123.test(body.metadata.name || '')) {
        return res.status(422).json({
          message: 'Invalid name',
          detail:
            'a lowercase RFC 1123 subdomain must consist of lower case alphanumeric characters',
        });
      }
      if (findIndexOf(resource, body) >= 0) {
        return res.status(409).json({
          message: `${resource} "${body.metadata.name}" already exists`,
          detail: 'AlreadyExists',
        });
      }
      attachConfigStatus(resource, body);
      if (req.query.dryRun !== 'true') {
        items(resource).push(body);
      }
      res.json(body);
    });
    app.put('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'PUT', resource, dryRun: req.query.dryRun === 'true', body });
      const index = findIndexOf(resource, body);
      if (index < 0) {
        return notFound(res, resource, body.metadata && body.metadata.name);
      }
      attachConfigStatus(resource, body);
      if (req.query.dryRun !== 'true') {
        items(resource)[index] = body;
      }
      res.json(body);
    });
    app.delete('/' + resource, (req, res) => {
      const body = req.body;
      requestsLog.push({ method: 'DELETE', resource, dryRun: req.query.dryRun === 'true', body });
      const index = findIndexOf(resource, body);
      if (index < 0) {
        return notFound(res, resource, body.metadata && body.metadata.name);
      }
      if (req.query.dryRun !== 'true') {
        items(resource).splice(index, 1);
      }
      res.sendStatus(204);
    });
  });

  // ----- Fallback -----
  app.use((req, res) => {
    console.warn(`[mock] unhandled ${req.method} ${req.originalUrl} -> 404`);
    notFound(res, req.path, '');
  });

  return app;
}

// Minimal semantics of the real logs endpoint: 'from' is a 'logTime,logTimeIndex'
// cursor (entries strictly after it), 'text' a substring match, 'records' the
// page size, 'sort' asc|desc by logTime.
function filterLogs(entries, query) {
  let logs = entries.slice();
  if (query.text) {
    const needle = String(query.text).toLowerCase();
    logs = logs.filter((l) => JSON.stringify(l).toLowerCase().includes(needle));
  }
  if (query.logType) {
    const types = [].concat(query.logType);
    logs = logs.filter((l) => types.includes(l.logType));
  }
  if (query.errorLevel) {
    const levels = [].concat(query.errorLevel);
    logs = logs.filter((l) => levels.includes(l.errorLevel));
  }
  if (query.from) {
    // 'from' is either a 'logTime,logTimeIndex' cursor or a plain UTC
    // timestamp (date-range filter); logTime values carry no zone suffix.
    const [time, index] = String(query.from).replace('Z', '').split(',');
    logs = logs.filter(
      (l) => l.logTime > time || (l.logTime === time && Number(l.logTimeIndex) > Number(index || 0))
    );
  }
  if (query.to) {
    const to = String(query.to).replace('Z', '');
    logs = logs.filter((l) => l.logTime <= to || l.logTime.startsWith(to));
  }
  logs.sort((a, b) =>
    a.logTime === b.logTime ? a.logTimeIndex - b.logTimeIndex : a.logTime < b.logTime ? -1 : 1
  );
  if (query.sort === 'desc') logs.reverse();
  return logs.slice(0, Number(query.records) || logs.length);
}

function notFound(res, kind, name) {
  res.status(404).json({
    kind: 'Status',
    apiVersion: 'v1',
    status: 'Failure',
    message: `${kind} "${name}" not found`,
    reason: 'NotFound',
    code: 404,
  });
}

function startServer(port) {
  const app = createApp();
  return app.listen(port, () => {
    console.log(`[mock] StackGres mock API listening on http://localhost:${port}`);
  });
}

module.exports = { createApp, startServer };

if (require.main === module) {
  startServer(process.env.MOCK_PORT || 8090);
}
