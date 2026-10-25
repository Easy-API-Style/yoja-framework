'use strict';

// the work awaited here, the module's own: the tests assert on what it gave (testJsUnit calls them as is)
await yojaWeb.append(document.querySelector('.good'), '/sub/panel.html');
const panel = yojaWeb.firstTag('.panel');

// a controler failing in its constructor: its section built without it, the others with theirs; the page shown
export function test_01() {
    ywAssert.assertEquals('yes', document.querySelector('.good').dataset.built);
    ywAssert.assertEquals(undefined, document.querySelector('.broken').ywSection.controler);
    ywAssert.assertEquals('', document.body.style.display);
}

// a path: another origin's left as it is; the page's, its parameters and hash kept, the ones added after them
export function test_02() {
    ywAssert.assertEquals('https://other.example/x.js', yojaWeb.path('https://other.example/x.js', {apiVersion: true}));
    ywAssert.assertEquals('/a.css?x=1&apiVersion=1.6.0', yojaWeb.path('/a.css?x=1', {apiVersion: true}));
    ywAssert.assertEquals('/p.js?q=1&apiVersion=1.6.0#h', yojaWeb.path(location.origin + '/p.js?q=1#h', {apiVersion: true}));
}

// a path a file declares bare ("panel.ctl.js"): resolved from that file's folder, not from the page
export function test_03() {
    ywAssert.assertEquals('yes', panel.dataset.built);
}

// a predicate: a function, or none -- anything else a mistake said, not every section kept unseen
export function test_04() {
    const sectionService = yojaWeb.sectionService;
    ywAssert.assertEquals(true, sectionService.first(document) !== null);
    ywAssert.assertEquals('good', sectionService.first(document, section => section.tag.classList.contains('good')).tag.className);
    let refused = null;
    try {
        sectionService.first(document, '.good');
    }
    catch (error) {
        refused = error;
    }
    ywAssert.assertEquals(true, refused instanceof TypeError);
}
