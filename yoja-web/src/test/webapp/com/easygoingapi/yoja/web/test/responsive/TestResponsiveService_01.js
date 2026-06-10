'use strict'

const responsiveService = yojaWebApi.responsiveService

ywAssert.assertEquals([{ name: 'smartphone', maxWidth: 480 },
                       { name: 'tablet', maxWidth: 768 },
                       { name: 'tabletLandscape', maxWidth: 1024 },
                       { name: 'laptop', maxWidth: 1600 },
                       { name: 'desktop' }],
                    responsiveService.getListOfMedia())
ywAssert.assertEquals({ name: 'smartphone', maxWidth: 480 },
                    responsiveService.findMedia('smartphone'), 
                    "findMedia test")     
