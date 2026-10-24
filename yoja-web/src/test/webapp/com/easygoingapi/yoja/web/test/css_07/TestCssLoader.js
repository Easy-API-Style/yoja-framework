'use strict';

// the loads are awaited here, the module's own: the tests assert on what they gave (testJsUnit calls them as is)
const cssLoader = await import(yojaWeb.path('/yoja/loader/cssLoader.js'));

const root = await cssLoader.load('/root.css');
const byPath = Object.fromEntries(root.sheetEntities.map(entity => [entity.path, entity]));
await Promise.all([cssLoader.load('/counted.css'), cssLoader.load('/counted.css')]);
await cssLoader.load('/counted.css');
const countedFetches = performance.getEntriesByType('resource').filter(entry => entry.name.includes('/counted.css')).length;
const image = await cssLoader.load('/sub/image.css');
document.adoptedStyleSheets = [...document.adoptedStyleSheets, image.sheetEntities[0].sheet];
const holder = Object.assign(document.createElement('div'), {className: 'image-holder'});
document.body.append(holder);
const imageUrl = getComputedStyle(holder).backgroundImage;

// an import: quoted or not, url() or not; its media, its layer and its supports() told apart; a comment's import
// not one; a string's comment kept
export function test_01() {
    ywAssert.assertEquals('print', byPath['/unquoted.css'].media);
    ywAssert.assertEquals('screen', byPath['/mid.css'].media);
    ywAssert.assertEquals(null, byPath['/layered.css'].media);
    ywAssert.assertEquals(true, byPath['/supported.css'] !== undefined);
    ywAssert.assertEquals(true, byPath['/unsupported.css'] === undefined);
    ywAssert.assertEquals(true, byPath['/missing.css'] === undefined);
    ywAssert.assertEquals(true, byPath['/root.css'].sheet.cssRules[0].cssText.includes('/* kept */'));
}

// the media along the path: the leaf applies when its import's and its parent's do
export function test_02() {
    ywAssert.assertEquals(['screen', '(min-width: 1px)'], byPath['/leaf.css'].medias);
    ywAssert.assertEquals(['screen'], byPath['/mid.css'].medias);
}

// a file fetched once: loaded at the same time, then again
export function test_03() {
    ywAssert.assertEquals(1, countedFetches);
}

// a relative url() resolved from its file, not from the page
export function test_04() {
    ywAssert.assertEquals(true, imageUrl.includes('/sub/logo.png'));
}
