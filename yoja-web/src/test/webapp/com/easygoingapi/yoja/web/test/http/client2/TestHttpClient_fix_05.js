'use strict';

const httpClient = yojaWebApi.httpClient;

/** @return the promise's outcome within 3s: {value} or {error} -- {pending: true} when it is left pending */
const within = promise => Promise.race([promise.then(value => ({value: value}), error => ({error: error})),
                                        new Promise(resolve => setTimeout(() => resolve({pending: true}), 3000))]);

// a header given in an object, its case another: sent once, not once more
const xml = await httpClient.post({url: '/echo', headers: {'Content-Type': 'application/xml'}}, '<x/>');
ywAssert.assertEquals('application/xml', xml.body.contentType);
