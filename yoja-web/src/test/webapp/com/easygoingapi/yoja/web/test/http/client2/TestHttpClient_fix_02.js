'use strict';

const httpClient = yojaWebApi.httpClient;

/** @return the promise's outcome within 3s: {value} or {error} -- {pending: true} when it is left pending */
const within = promise => Promise.race([promise.then(value => ({value: value}), error => ({error: error})),
                                        new Promise(resolve => setTimeout(() => resolve({pending: true}), 3000))]);

// a json body not json: rejected -- not left pending --, an Error with the request
const bad = await within(httpClient.get('/bad.json'));
ywAssert.assertEquals(undefined, bad.pending);
ywAssert.assertEquals(true, bad.error instanceof Error);
ywAssert.assertEquals('/bad.json', bad.error.request.url);
