export default class NamedControler {
    constructor(section) {
        section.tag.dataset.className = this.className();
    }

    // its own: kept
    className() {
        return 'its own';
    }
}
