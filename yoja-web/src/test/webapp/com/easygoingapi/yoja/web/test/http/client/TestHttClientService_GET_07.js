'use strict'

const httpClient = yojaWebApi.httpClient

let onRequest = null
httpClient.onRequest(v => {
    onRequest = v
})   
const result = await httpClient.get({url: '/file_1.txt', 
                                     fetchAs:'text',
                                     headers: {'header-value': 'header-value_11'}})

ywAssert.assertEquals('{"url":"/file_1.txt","fetchAs":"text","headers":{"header-value":"header-value_11"},"method":"GET"}', JSON.stringify(onRequest))
ywAssert.assertEquals('header-value_11', onRequest.headers['header-value'])

