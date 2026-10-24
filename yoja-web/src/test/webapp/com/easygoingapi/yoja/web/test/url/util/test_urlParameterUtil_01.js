'use strict';

const urlParameterUtil = await import(yojaWeb.path('/yoja/util/urlParameterUtil.js'));

const entries = urlParameter => JSON.stringify(urlParameterUtil.parseUrlParameter(urlParameter).entries());

// a string is decoded once, as URLSearchParams does: '%25' is a '%', a lone '%' kept
export function test_01() {
    ywAssert.assertEquals('[{"key":"a","value":"%41"}]', entries('a=%2541'));
    ywAssert.assertEquals('[{"key":"remise","value":"100%"}]', entries('remise=100%'));
    ywAssert.assertEquals('[{"key":"a","value":"1&2"},{"key":"b","value":"3"}]', entries('?a=1%262&b=3'));
    ywAssert.assertEquals('[{"key":"q","value":"vin rosé"}]', entries('q=vin%20ros%C3%A9'));
}

// the parameters are added before the url's hash; a hash given replaces it
export function test_02() {
    ywAssert.assertEquals('/p?a=1#x', urlParameterUtil.toUrl('/p#x', {a: 1}));
    ywAssert.assertEquals('/p?a=1#y', urlParameterUtil.toUrl('/p#x', {a: 1}, '#y'));
    ywAssert.assertEquals('/p?a=1', urlParameterUtil.toUrl('/p?', {a: 1}));
    ywAssert.assertEquals('/p?x=1&a=1', urlParameterUtil.toUrl('/p?x=1', {a: 1}));
    ywAssert.assertEquals('/p', urlParameterUtil.toUrl('/p', undefined));
    ywAssert.assertEquals('/p?a=1&a=2&b', urlParameterUtil.toUrl('/p', {a: [1, 2], b: ''}));
}

// an object's own keys only; a Map read as an object
export function test_03() {
    const inherited = Object.create({hidden: 'yes'});
    inherited.shown = 'yes';
    ywAssert.assertEquals('[{"key":"shown","value":"yes"}]', entries(inherited));
    ywAssert.assertEquals('[{"key":"a","value":"1"},{"key":"b","value":"2"},{"key":"b","value":"3"}]',
                          entries(new Map([['a', '1'], ['b', ['2', '3']]])));
}

// its entries are copies; its json is the list parseUrlParameter reads back
export function test_04() {
    const urlParameter = urlParameterUtil.parseUrlParameter({a: '1'});
    urlParameter.entries()[0].value = 'changed';
    ywAssert.assertEquals('1', urlParameter.get('a'));
    ywAssert.assertEquals('[{"key":"a","value":"1"}]', JSON.stringify(urlParameter.toJson()));
    ywAssert.assertEquals(true, urlParameterUtil.parseUrlParameter(urlParameter.toJson()).equals(urlParameter));
}
