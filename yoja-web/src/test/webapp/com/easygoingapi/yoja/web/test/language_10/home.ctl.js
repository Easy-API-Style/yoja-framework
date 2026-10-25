'use strict';
const languageService = yojaWebApi.languageService;

// the translations made: {key, tag (its class), attribute}
window.events = [];
// a key whose translation fails (an onTranslate action throwing)
window.throwOn = null;
// the label's text when the page is shown
window.shownLabel = undefined;

export default class HomeControler {

    constructor(section) {
        languageService.onLanguageTranslate(h => {
            window.events.push({key: h.key, tag: h.tag.className, attribute: h.attribute});
            if (h.key === window.throwOn) {
                throw new Error('an onTranslate action failing on purpose');
            }
        });
        const observer = new MutationObserver(() => {
            if (document.body.style.display !== 'none' && window.shownLabel === undefined) {
                window.shownLabel = yojaWeb.firstTag('.first').innerHTML;
                observer.disconnect();
            }
        });
        observer.observe(document.body, {attributes: true, attributeFilter: ['style']});
    }

}
