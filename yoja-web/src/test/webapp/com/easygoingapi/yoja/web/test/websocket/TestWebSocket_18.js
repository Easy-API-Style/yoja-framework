'use strict';

// its listeners, list and map, told on the socket connected again
export default function test(args, resolve, repeat) {
    if (window.listMessage === 'after' && window.mapMessage === 'after') {
        resolve();
    }
}
