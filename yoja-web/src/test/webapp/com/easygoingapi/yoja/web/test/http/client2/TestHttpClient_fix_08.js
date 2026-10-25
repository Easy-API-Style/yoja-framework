'use strict';

const httpClient = yojaWebApi.httpClient;

/** @return the promise's outcome within 3s: {value} or {error} -- {pending: true} when it is left pending */
const within = promise => Promise.race([promise.then(value => ({value: value}), error => ({error: error})),
                                        new Promise(resolve => setTimeout(() => resolve({pending: true}), 3000))]);

// a response action failing: the request answered all the same
httpClient.onResponse(() => {
    throw new Error('a response action failing on purpose');
});
const answered = await within(httpClient.get('/data.xml'));
ywAssert.assertEquals(undefined, answered.pending);
ywAssert.assertEquals(undefined, answered.error);
ywAssert.assertEquals('<a>1</a>', answered.value.body);
