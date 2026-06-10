'use strict'

const httpClient = yojaWebApi.httpClient

/*
 * onRequest
 */
let onRequest = null
httpClient.onRequest(v => {
    onRequest = v
})   

const result = await httpClient.get({url: '/file_1.txt', 
                                     fetchAs:'text',
                                     headers: new Headers([['header-value', 'header-value_10']])})

ywAssert.assertEquals('{"url":"/file_1.txt","fetchAs":"text","headers":{},"method":"GET"}', JSON.stringify(onRequest))
ywAssert.assertEquals('header-value_10', onRequest.headers.get('Header-Value'))

