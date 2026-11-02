export default class EarlyControler {
    constructor(section) {
        // className() there already in the constructor
        section.tag.dataset.className = this.className();
    }
}
