'use strict';

// what the page and the changes gave: awaited here, asserted below (testJsUnit calls the tests as is)
const languageService = yojaWebApi.languageService;
const text = selector => yojaWeb.firstTag(selector).innerHTML;
const count = (events, tag) => events.filter(event => event.tag === tag).length;

const shownLabel = window.shownLabel;
const frKept = text('.kept');
const frEmpty = text('.empty');

window.events = [];
await languageService.refresh();
const refreshEvents = [...window.events];

window.throwOn = 'hello';
const switched = await languageService.setLanguage('en').catch(error => ({error: error}));
window.throwOn = null;
const enKept = text('.kept');
const enFirst = text('.first');
const enSecond = text('.second');

window.events = [];
const row = document.createElement('label');
row.className = 'row';
row.setAttribute('yw-i18n', 'bye');
await yojaWeb.appendTag(yojaWeb.firstTag('.rows'), row);
const appendEvents = [...window.events];
const rowText = text('.row');

const getItem = Storage.prototype.getItem;
const setItem = Storage.prototype.setItem;
Storage.prototype.getItem = () => {
    throw new Error('a storage failing on purpose');
};
Storage.prototype.setItem = Storage.prototype.getItem;
const unstored = await languageService.setLanguage('fr').catch(error => ({error: error}));
const unstoredLanguage = (() => {
    try {
        return languageService.getLanguage();
    }
    catch (error) {
        return error;
    }
})();
const unstoredFirst = text('.first');
Storage.prototype.getItem = getItem;
Storage.prototype.setItem = setItem;

// the page shown once translated: not its keys first
export function test_01() {
    ywAssert.assertEquals('Bonjour', shownLabel);
}

// an empty translation applied; a key missing in the language: the page's own text, not another language's
export function test_02() {
    ywAssert.assertEquals('Seulement en français', frKept);
    ywAssert.assertEquals('', frEmpty);
    ywAssert.assertEquals('Par défaut', enKept);
}

// a tag translated once -- its text and its attribute, once each --; a section in sections too
export function test_03() {
    ywAssert.assertEquals(2, count(refreshEvents, 'both'));
    ywAssert.assertEquals(1, count(refreshEvents, 'deep'));
    ywAssert.assertEquals(7, refreshEvents.length);
}

// an onTranslate action failing: the others translated all the same, the language set
export function test_04() {
    ywAssert.assertEquals(undefined, switched.error);
    ywAssert.assertEquals(true, switched.updated);
    ywAssert.assertEquals('Bye', enSecond);
    ywAssert.assertEquals('Hello', enFirst);
}

// a tag added: only it translated, not the rest of its section
export function test_05() {
    ywAssert.assertEquals('Bye', rowText);
    ywAssert.assertEquals(1, appendEvents.length);
    ywAssert.assertEquals('row', appendEvents[0].tag);
}

// the storage failing: the language kept for the page
export function test_06() {
    ywAssert.assertEquals(undefined, unstored.error);
    ywAssert.assertEquals('fr', unstoredLanguage);
    ywAssert.assertEquals('Bonjour', unstoredFirst);
}
