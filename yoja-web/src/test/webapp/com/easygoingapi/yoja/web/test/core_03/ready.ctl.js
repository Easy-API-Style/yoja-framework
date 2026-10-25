export default class ReadyControler {
    constructor(section) {
        section.pageReady(() => section.tag.dataset.ready = 'yes');
    }
}
