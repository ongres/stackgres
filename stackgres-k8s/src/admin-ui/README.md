# Development set up
## Installation requirements
* node 14.4.0 or greater

To install all node dependencies run:

``` sh
npm install
```

## Launch 
In order to launch the UI for local development the environment
 variable "SERVER" must be provided. 

Optionally the could specified in in a .env file, that can have a
 content like this:
``` sh
# Local server
SERVER=https://localhost:8433/stackgres
```

Once you have the environment variables in place, simply run:

``` sh
npm run dev
```

Then, you access the UI at http://localhost:8081/admin/

# Tests

In order to do some e2e testing with UI the StackGres will rely on Cypress to do so. 

Cypress will need some environment varibles in order to work. A 
 convinient way to pass this variables is by specifying a 
 cypress.env.json 

``` json
{
    "username": "<UI username>",
    "password": "<UI password>",
    "host": "http://localhost:8081/"
}
```