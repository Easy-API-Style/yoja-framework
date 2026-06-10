'use strict'

const responsiveService = yojaWebApi.responsiveService

const mediaList = [{ name: 'smartphone', maxWidth: 480 },
                   { name: 'tablet', maxWidth: 768 },
                   { name: 'desktop' }]

responsiveService.initialize(mediaList)                 
                   
ywAssert.assertEquals(mediaList,
                    responsiveService.getListOfMedia())
ywAssert.assertEquals({ name: 'desktop' },
                    responsiveService.findMedia('desktop'), 
                    "findMedia test")     
