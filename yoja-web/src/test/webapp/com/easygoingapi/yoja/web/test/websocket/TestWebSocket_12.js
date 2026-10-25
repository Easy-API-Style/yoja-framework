'use strict';

// in their order -- the one taken off not told, the one failing not keeping the next from being told
export default function test(args, resolve, repeat) {
    const expected = ['list:one', 'b:one', 'a2:one'];
    if (JSON.stringify(window.wsLog) === JSON.stringify(expected)
          && typeof window.offB === 'function') {
        resolve();
    }
}
