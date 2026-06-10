'use strict'

const httpClient = yojaWebApi.httpClient

let onRequest = null
httpClient.onRequest(v => {
    onRequest = v
})   
let onResponse = null
httpClient.onResponse((request, response) => {
    onResponse = {'request': request, 
                  'response': response}
})
const result = await httpClient.get({url: '/file_1.txt', 
                                     fetchAs:'text',
                                     parameters: {value: 'get_8'}})

ywAssert.assertEquals(false, httpClient.isOffline())
ywAssert.assertEquals('{"url":"/file_1.txt","fetchAs":"text","parameters":{"value":"get_8"},"method":"GET"}', JSON.stringify(onRequest))
ywAssert.assertEquals('{"request":{"url":"/file_1.txt","fetchAs":"text","parameters":{"value":"get_8"},"method":"GET"},' 
                  + '"response":{"status":200,"headers":{},"bodyUsed":true,"body":"hello world"}}',
                    JSON.stringify(onResponse))
