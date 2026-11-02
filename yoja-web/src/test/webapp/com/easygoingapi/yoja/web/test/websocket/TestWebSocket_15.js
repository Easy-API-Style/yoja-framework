'use strict';

// closed, from zero: nothing sent on it any more; webSocket(url) gives a new one
const webSocketService = yojaWebApi.webSocketService;

export default function test(args, resolve, reject) {
    const old = webSocketService.webSocket('/websocket/test');
    old.close(3000, 'over')
       .then(() => {
           ywAssert.assertEquals(true, old.is('CLOSED'));
           const fresh = webSocketService.webSocket('/websocket/test');
           ywAssert.assertEquals(true, fresh !== old);
           return old.send('again')
                     .then(() => {
                         throw new Error('sent on a socket over');
                     }, () => fresh.close());
       })
       .then(() => resolve())
       .catch(error => reject(error));
}
