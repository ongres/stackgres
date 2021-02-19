# StackGres Web Console

## Installation requirements

- node 14.4.0 or greater

To install all node dependencies run:

```sh
npm install
```

## Environment Variables

In order to enable API access to the UI, the environment variable for the API server must be provided.

```sh
VUE_APP_API_URL=/stackgres                              # Base API url
```

When working on development mode, and if connecting to an external API, an extra variable must be set in order to enable the proxy to bypass CORS validations:

```sh
VUE_APP_API_URL=/apiURL                                 # Local Proxy
VUE_APP_API_PROXY_URL=https://api.host.com/stackgres    # External API address
```

You can specify these variables by placing them in the corresponding mode files in project's root folder:

```sh
.env                # loaded in all cases
.env.local          # loaded in all cases, used only in your local environment, ignored by git
.env.[mode]         # only loaded in specified mode
.env.[mode].local   # only loaded in specified mode, used only in your local environment, ignored by git (PREFERED METHOD)
```

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
