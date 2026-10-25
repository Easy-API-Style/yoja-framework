'use strict';

const httpClient = yojaWebApi.httpClient;

/** @return the promise's outcome within 3s: {value} or {error} -- {pending: true} when it is left pending */
const within = promise => Promise.race([promise.then(value => ({value: value}), error => ({error: error})),
                                        new Promise(resolve => setTimeout(() => resolve({pending: true}), 3000))]);

// fetch failing: its url told
let message = null;
await httpClient.fetch('http://127.0.0.1:1/nowhere').catch(error => message = error.message);
ywAssert.assertEquals('fetch url failed: http://127.0.0.1:1/nowhere', message);
