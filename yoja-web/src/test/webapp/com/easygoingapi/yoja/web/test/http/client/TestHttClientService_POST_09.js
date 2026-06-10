'use strict'

const httpClient = yojaWebApi.httpClient

const array = new Uint8Array(8)
array[1] = 65
const result = await httpClient.post({url: '/post_01', contentType: 'uint8'}, array)  
   
const decoder = new TextDecoder();
ywAssert.assertEquals('{"0":0,"1":65,"2":0,"3":0,"4":0,"5":0,"6":0,"7":0}', decoder.decode(result.body))
ywAssert.assertEquals('uint8', result.headers.get('content-type'))

