'use strict'

const storageService = yojaWebApi.storageService 

export default function test(args, resolve, repeat) {
    const mediaEvents = storageService.getSessionItem('media')
    if (mediaEvents.length == 2
          && mediaEvents[0].previousMedia.name === 'tablet'
          && mediaEvents[0].media.name === 'desktop'
          && mediaEvents[1].previousMedia.name === 'desktop'
          && mediaEvents[1].media.name === 'tablet') {
        resolve() 
    }
}