'use strict';

// the work awaited here, the module's own: the tests assert on what it gave (testJsUnit calls them as is)
const section = (name, controler, display) => {
    const tag = document.createElement('yw-section');
    tag.className = name;
    tag.setAttribute('yw-controler', controler);
    if (display) {
        tag.style.display = display;
    }
    return tag;
};
const host = document.querySelector('.host');

// 1. two sections added into a tag out of the page: their ready actions run
const detached = document.createElement('div');
const outOfPage = [section('a', '/ready.ctl.js'), section('b', '/ready.ctl.js')];
let outOfPageError = null;
await yojaWeb.appendTag(detached, outOfPage).catch(error => outOfPageError = error.message);

// 2. a pageReady failing: the next one runs all the same
const failing = [section('throwing', '/throwing.ctl.js'), section('after', '/ready.ctl.js')];
let failingError = null;
await yojaWeb.appendTag(host, failing).catch(error => failingError = error.message);

// 3, 8. an addition whose controler is missing: built without it, shown, its own display kept -- and kept when it
// succeeds
const broken = section('broken', '/absent.ctl.js', 'flex');
let brokenRejected = false;
await yojaWeb.appendTag(host, [broken]).catch(() => brokenRejected = true);
const grid = section('grid', '/ready.ctl.js', 'grid');
await yojaWeb.appendTag(host, [grid]);

// 7. prepend: the order given kept
const first = section('first', '/ready.ctl.js');
const second = section('second', '/ready.ctl.js');
const prepended = await yojaWeb.prependTag(host, [first, second]);

const nextTask = () => new Promise(resolve => setTimeout(resolve));

// 9. the document ready already: an action given told all the same -- after the call, not during it; off before: not
const lateDocument = [];
yojaWeb.onDocumentReady(() => lateDocument.push('told'));
yojaWeb.onDocumentReady(() => lateDocument.push('off'))();
const lateDocumentDuringCall = lateDocument.length;
await nextTask();

// 10. a section's ready actions told already: one given told all the same, after the call; the section gone: never,
// forgotten or not yet -- no error
const [late, gone, forgotten] = [section('late', '/ready.ctl.js'), section('gone', '/ready.ctl.js'),
                                 section('forgotten', '/ready.ctl.js')];
await yojaWeb.appendTag(host, [late, gone, forgotten]);
const lateReady = [];
late.ywSection.controlerReady(() => lateReady.push('controlerReady'));
late.ywSection.pageReady(() => lateReady.push('pageReady'));
const lateReadyDuringCall = lateReady.length;
gone.remove();
gone.ywSection.pageReady(() => lateReady.push('gone'));
forgotten.remove();
yojaWebApi.eventService.trigger('core_03 sweep');   // the sections gone forgotten
let forgottenError = null;
try {
    forgotten.ywSection.pageReady(() => lateReady.push('forgotten'));
}
catch (error) {
    forgottenError = error;
}
await nextTask();

export function test_01() {
    ywAssert.assertEquals(null, outOfPageError);
    ywAssert.assertEquals('yes,yes', outOfPage.map(tag => tag.dataset.ready).join(','));
}

export function test_02() {
    ywAssert.assertEquals(null, failingError);
    ywAssert.assertEquals('yes', failing[1].dataset.ready);
}

export function test_03() {
    ywAssert.assertEquals(false, brokenRejected);
    ywAssert.assertEquals(true, broken.ywSection !== undefined);
    ywAssert.assertEquals(undefined, broken.ywSection.controler);
    ywAssert.assertEquals('flex', broken.style.display);
    ywAssert.assertEquals('grid', grid.style.display);
}

// 4. an option given: the version kept all the same
export function test_04() {
    ywAssert.assertEquals('/a.js?version=v1&apiVersion=1.6.0', yojaWeb.path('/a.js', {apiVersion: true}));
}

export function test_06() {
    ywAssert.assertEquals(0, lateDocumentDuringCall);
    ywAssert.assertEquals('told', lateDocument.join(','));
}

export function test_07() {
    ywAssert.assertEquals(0, lateReadyDuringCall);
    ywAssert.assertEquals(null, forgottenError);
    ywAssert.assertEquals('controlerReady,pageReady', lateReady.join(','));
}

export function test_05() {
    ywAssert.assertEquals('first,second', prepended.map(tag => tag.className).join(','));
    ywAssert.assertEquals('first,second', [...host.children].slice(0, 2).map(tag => tag.className).join(','));
}
