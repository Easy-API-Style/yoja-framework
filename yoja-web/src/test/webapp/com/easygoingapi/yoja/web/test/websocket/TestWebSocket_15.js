'use strict';

// closed, from zero: its listeners dropped, nothing sent on it any more; webSocket(url) gives a new one
const webSocketService = yojaWebApi.webSocketService;

export default function test(args, resolve, reject) {
    const old = webSocketService.webSocket('/websocket/test');
    old.close(3000, 'over')
       .then(() => {
           ywAssert.assertEquals(true, old.is('CLOSED'));
           ywAssert.assertEquals(false, old.hasMessage('a'));
           const fresh = webSocketService.webSocket('/websocket/test');
           ywAssert.assertEquals(true, fresh !== old);
           ywAssert.assertEquals(false, fresh.hasMessage('a'));
           return old.send('again')
                     .then(() => {
                         throw new Error('sent on a socket over');
                     }, () => fresh.close());
       })
       .then(() => resolve())
       .catch(error => reject(error));
}
