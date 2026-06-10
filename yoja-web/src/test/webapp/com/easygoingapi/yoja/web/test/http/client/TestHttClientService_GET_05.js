'use strict'

const httpClient = yojaWebApi.httpClient

let onRequest = null
httpClient.onRequest(v => {
   onRequest = v
})   

const result = await httpClient.get({url: '/file_1.txt', 
                                     fetchAs:'text',
                                     parameters: new URLSearchParams({value: 'get_9'})})

ywAssert.assertEquals('{"url":"/file_1.txt","fetchAs":"text","parameters":{},"method":"GET"}', JSON.stringify(onRequest))
ywAssert.assertEquals('get_9', onRequest.parameters.get('value'))
