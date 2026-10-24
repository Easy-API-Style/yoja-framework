'use strict';

const cards = yojaWeb.findTags('.card');
const items = yojaWeb.findTags('.item');
// a slot to the card file, added once the page is built: that file loaded already
const later = document.createElement('div');
document.body.append(later);
await yojaWeb.append(later, '/later.html');
const cardsAfter = yojaWeb.findTags('.card');

// the same slot file twice: two cards, each built by its controler
export function test_01() {
    ywAssert.assertEquals(2, cards.length);
    ywAssert.assertEquals('yes,yes', cards.map(card => card.dataset.built).join(','));
}

// the same include file twice: two items, each built by its controler
export function test_02() {
    ywAssert.assertEquals(2, items.length);
    ywAssert.assertEquals('yes,yes', items.map(item => item.dataset.built).join(','));
}

// a slot to a file loaded already, added later: its card built too
export function test_03() {
    ywAssert.assertEquals(3, cardsAfter.length);
    ywAssert.assertEquals('yes,yes,yes', cardsAfter.map(card => card.dataset.built).join(','));
}
