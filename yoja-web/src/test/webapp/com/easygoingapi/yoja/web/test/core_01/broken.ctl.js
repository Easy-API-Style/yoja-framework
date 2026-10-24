export default class BrokenControler {
    constructor() {
        throw new Error('broken on purpose');
    }
}
