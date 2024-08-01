/*
Copyright 2020 The Knative Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

const express = require('express')
const { CloudEvent, HTTP } = require('cloudevents')
const PORT = process.env.PORT || 8080
const app = express()
const axios = require('axios').default;

const main = () => {
  app.listen(PORT, function () {
    console.log(`Listening for cloudevents on port ${PORT}!`)
  })
}

app.use((request, response, next) => {
  let data = ''
  request.setEncoding('utf8')
  request.on('data', function (chunk) {
    data += chunk
  })
  request.on('end', function () {
    request.body = data
    next()
  })
})

app.post('/', function (request, response) {
  try {
    const event = HTTP.toEvent({headers: request.headers, body: request.body})
    eval(process.env.SCRIPT);
  } catch (err) {
    console.error(err)
    response.status(500)
      .header('Content-Type', 'application/json')
      .send(JSON.stringify(err))
  }
})

main()
