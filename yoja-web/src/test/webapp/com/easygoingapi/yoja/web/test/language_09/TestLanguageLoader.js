'use strict';

// the loads are awaited here, the module's own: the tests assert on what they gave (testJsUnit calls them as is)
const languageLoader = await import(yojaWeb.path('/yoja/loader/languageLoader.js'));
const languageService = yojaWebApi.languageService;

const empty = await languageLoader.load('/empty.xml');
const broken = await languageLoader.load('/broken.xml');
await Promise.all([languageLoader.load('/counted.xml'), languageLoader.load('/counted.xml')]);
await languageLoader.load('/counted.xml');
const countedFetches = performance.getEntriesByType('resource').filter(entry => entry.name.includes('/counted.xml')).length;
const merged = await languageLoader.load('/root.xml');
const translator = await languageService.loadTranslator('/root.xml');

// an empty file, a file not well formed: no text -- not a failure
export function test_01() {
    ywAssert.assertEquals(0, empty.xml.querySelectorAll('yw-text').length);
    ywAssert.assertEquals(0, broken.xml.querySelectorAll('yw-text').length);
}

// a file fetched once: loaded at the same time, then again
export function test_02() {
    ywAssert.assertEquals(1, countedFetches);
}

// an import is an include: the file's own text wins over its include's; an include coming back to the file is not
// followed again
export function test_03() {
    ywAssert.assertEquals(['/included.xml', '/root.xml'], merged.paths);
    ywAssert.assertEquals('Du fichier', translator('greeting', 'fr'));
    ywAssert.assertEquals('Au revoir', translator('farewell', 'fr'));
}
