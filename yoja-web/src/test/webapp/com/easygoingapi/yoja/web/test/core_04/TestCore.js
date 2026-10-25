'use strict';

// the work awaited here, the module's own: the tests assert on what it gave (testJsUnit calls them as is)
const built = ['.level-1', '.level-2', '.in-slot'].map(selector => yojaWeb.firstTag(selector));
const ids = yojaWeb.findTags('[yw-section-id]').map(tag => tag.getAttribute('yw-section-id'));

// an element given with its listener, a property of its own and a field typed in: kept once in the page
const wrapper = document.createElement('yw-section');
wrapper.setAttribute('yw-controler', '/mark.ctl.js');
const button = Object.assign(document.createElement('button'), {className: 'kept', textContent: 'click'});
let clicks = 0;
button.addEventListener('click', () => clicks++);
button.mine = 42;
const field = Object.assign(document.createElement('input'), {className: 'typed'});
field.value = 'typed before';
wrapper.append(button, field);
await yojaWeb.appendTag(document.querySelector('.host'), [wrapper]);
const buttonInPage = yojaWeb.firstTag('button.kept');
buttonInPage.click();

// three levels of sections, a slot the deepest: each built by its controler, each its own id
export function test_01() {
    ywAssert.assertEquals('yes,yes,yes', built.map(tag => tag && tag.dataset.built).join(','));
    ywAssert.assertEquals(ids.length, new Set(ids).size);
}

// the element given is the one in the page: its listener, its property, its field's value kept
export function test_02() {
    ywAssert.assertEquals(true, buttonInPage === button);
    ywAssert.assertEquals(1, clicks);
    ywAssert.assertEquals(42, buttonInPage.mine);
    ywAssert.assertEquals('typed before', yojaWeb.firstTag('input.typed').value);
    ywAssert.assertEquals('yes', wrapper.dataset.built);
}
