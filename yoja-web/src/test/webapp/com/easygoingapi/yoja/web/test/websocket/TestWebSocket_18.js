'use strict';

export default function test(args, resolve, repeat) {
    if (window.lastMessage === 'after') {
        resolve();
    }
}
