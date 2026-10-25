'use strict';

export default function test(args, resolve, repeat) {
    if (JSON.stringify(window.wsLog) === JSON.stringify(['list:two', 'a2:two'])) {
        resolve();
    }
}
