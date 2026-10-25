'use strict';

const domUtil = await import(yojaWeb.path('/yoja/util/domUtil.js'));

/** A tag: its name, its class, its children -- a section when 'yw-controler' is in its attributes. */
function tag(name, attributes = {}, ...children) {
    const element = document.createElement('div');
    element.dataset.name = name;
    for (const [key, value] of Object.entries(attributes)) {
        element.setAttribute(key, value);
    }
    if (element.hasAttribute('yw-controler')) {
        element.ywSection = {tag: element, name: name};
    }
    element.append(...children);
    return element;
}

const names = tags => tags.map(element => element.dataset ? element.dataset.name : element.name);

// walkTag: a section neither given nor entered -- the tags after it given all the same
export function test_01() {
    const root = tag('root', {}, tag('wrapper', {}, tag('a'), tag('section', {'yw-controler': 'x'}, tag('inSection')),
                                     tag('b')));
    const visited = [];
    domUtil.walkTag(root, element => {
        visited.push(element);
    });
    ywAssert.assertEquals('wrapper,a,b', names(visited).join(','));
}

// walk, walkSection: the consumer returning false stops the whole walk
export function test_02() {
    const root = tag('root', {}, tag('a', {}, tag('a1')), tag('b'));
    const visited = [];
    domUtil.walk(root, element => {
        visited.push(element);
        return element.dataset.name !== 'a1';
    });
    ywAssert.assertEquals('a,a1', names(visited).join(','));

    const sections = tag('root', {},
                         tag('s1', {'yw-controler': 'x'}, tag('s1a', {'yw-controler': 'x'})),
                         tag('s2', {'yw-controler': 'x'}));
    const walked = [];
    domUtil.walkSection(sections, section => {
        walked.push(section);
        return section.name !== 's1a';   // found: the first one -- s2 not walked to
    });
    ywAssert.assertEquals('s1,s1a', names(walked).join(','));
}

// firstTag, findTags: in the document's order -- as querySelector --, not into the sections
export function test_03() {
    const root = tag('root', {}, tag('a', {}, tag('deep', {class: 'm'})), tag('b', {class: 'm'}),
                     tag('section', {'yw-controler': 'x'}, tag('inSection', {class: 'm'})));
    ywAssert.assertEquals('deep', domUtil.firstTag(root, '.m').dataset.name);
    ywAssert.assertEquals('deep,b', names(domUtil.findTags(root, '.m')).join(','));
}

// deepFirstTag / deepFindTags: into the sections and the shadow roots; a selector's spaces, '>' and ',' inside
// [...], (...) or quotes left as they are; '+' and '~' refused
export function test_04() {
    const host = tag('host', {class: 'host'});
    host.attachShadow({mode: 'open'}).append(tag('inner', {class: 'inner'}));
    const input = document.createElement('input');
    input.setAttribute('placeholder', 'a > b, c');
    const root = tag('root', {}, tag('form', {class: 'form'}, input), host,
                     tag('section', {'yw-controler': 'x'}, tag('q', {class: 'q'})));
    ywAssert.assertEquals('inner', domUtil.deepFirstTag(root, '.host .inner').dataset.name);
    ywAssert.assertEquals('inner', domUtil.deepFirstTag(root, '.host > .inner').dataset.name);
    ywAssert.assertEquals(input, domUtil.deepFirstTag(root, '.form input[placeholder="a > b, c"]'));
    ywAssert.assertEquals('q', domUtil.deepFirstTag(root, ':is(.p, .q)').dataset.name);
    ywAssert.assertEquals('inner,q', names(domUtil.deepFindTags(root, '.inner, .q')).join(','));
    // a list: the first one in the document's order, whichever of its selectors it matches
    ywAssert.assertEquals('inner', domUtil.deepFirstTag(root, '.q, .inner').dataset.name);
    ywAssert.assertEquals('inner,q', names(domUtil.deepFindTags(root, '.q, .inner')).join(','));
    // a descendant first in the document's order, the ancestors matched right to left
    const ordered = tag('ordered', {}, tag('x1', {class: 'x'}, tag('y-in-x1', {class: 'y'})), tag('y-out', {class: 'y'}),
                        tag('x2', {class: 'x'}, tag('y-in-x2', {class: 'y'})));
    ywAssert.assertEquals('y-in-x1,y-in-x2', names(domUtil.deepFindTags(ordered, '.x .y')).join(','));
    ywAssert.assertEquals('y-in-x1', domUtil.deepFirstTag(ordered, '.x > .y').dataset.name);
    let refused = null;
    try {
        domUtil.deepFirstTag(root, '.form + .host');
    }
    catch (error) {
        refused = error.message;
    }
    ywAssert.assertEquals(true, refused !== null && refused.includes('"+" is not supported'));
}
