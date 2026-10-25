export default class ThrowingControler {
    constructor(section) {
        section.pageReady(() => {
            throw new Error('a pageReady failing on purpose');
        });
    }
}
