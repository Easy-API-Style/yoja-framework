'use strict'

const storageService = yojaWebApi.storageService 
const responsiveService = yojaWebApi.responsiveService

storageService.setSessionItem('resize', [])
let countResize = 0
responsiveService.onResize(h => {
    countResize++
    const resizeEvents = storageService.getSessionItem('resize')
    h.count = countResize
    resizeEvents.push(h)
    storageService.setSessionItem('resize', resizeEvents)
})

storageService.setSessionItem('media', [])
let countMedia = 0
responsiveService.onMedia(h => {
    countMedia++
    const mediaEvents = storageService.getSessionItem('media')
    h.count = countMedia
    mediaEvents.push(h)
    storageService.setSessionItem('media', mediaEvents)
})