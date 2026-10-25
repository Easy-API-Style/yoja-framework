'use strict';

const httpClient = yojaWebApi.httpClient;

/** @return the promise's outcome within 3s: {value} or {error} -- {pending: true} when it is left pending */
const within = promise => Promise.race([promise.then(value => ({value: value}), error => ({error: error})),
                                        new Promise(resolve => setTimeout(() => resolve({pending: true}), 3000))]);

// a post with no body
const noBody = await within(httpClient.post('/echo'));
ywAssert.assertEquals(undefined, noBody.error);
ywAssert.assertEquals(200, noBody.value.status);
ywAssert.assertEquals('', noBody.value.body.body);
