# StackGres Web Console

## Installation requirements

- node 14.4.0 or greater

To install all node dependencies run:

```sh
npm install
```

## Local deploy with API proxy

When working on development mode, and if connecting to an external API, an extra variable must be set in order to enable the proxy to bypass CORS validations:

```sh
VUE_APP_API_PROXY_URL=https://api.host.com/stackgres    # External API address
```

You can specify this variable either manually on each run, or by placing it in the `.env.development.local` file in the project's root folder. Be aware that this variable will be used only on your local development environment.

## Running the UI (Compiles and hot-reloads for development)

Once you have the environment variables in place, simply run:

```sh
npm run serve
```

Then, you access the UI at http://localhost:8081/admin/

## Bulding the UI (Compiles and minifies for production)

```sh
npm run build
```

This will build the UI and save into the `dist` folder

## Tests

In order to do some e2e testing with UI the StackGres will rely on Cypress to do so.

Cypress will need some environment varibles in order to work. A
convinient way to pass this variables is by specifying a
cypress.env.json

```json
{
  "username": "<UI username>",
  "password": "<UI password>",
  "host": "http://localhost:8081/"
}
```

## More Information

### Customizing Env Vars

See [Modes and Environment Variables](https://cli.vuejs.org/guide/mode-and-env.html#modes)

### Customizing configuration

See [Configuration Reference](https://cli.vuejs.org/config/).
