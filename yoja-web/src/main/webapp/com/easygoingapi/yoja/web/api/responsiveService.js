/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict'
const config = yojaWeb.config
const {Listeners} = await import(yojaWeb.path('../util/listenerUtil.js'));

class ResponsiveService {
    
    // the application's actions: each onXXX(action) gives back its off (listenerUtil)
    #mediaActions = new Listeners('responsiveService media');
    #resizeActions = new Listeners('responsiveService resize');
    
    #listOfMedia = [];
    #media = null;
    
    constructor(mediaDescriptions) {
        this.initalistMediaList(mediaDescriptions);
        window.addEventListener('resize', () => this.#resizeListener());
    }
    
    initalistMediaList(mediaDescriptions) {
        const result = [];
        this.#media = null;
        if (mediaDescriptions && mediaDescriptions.length) {
            let biggerMedia = null;
            const maxWidths = [];
            const mediaList = {};
            for (const mediaDescription of mediaDescriptions) {
                const media = mediaDescription.maxWidth 
                                ? {name: mediaDescription.name, 
                                   maxWidth: mediaDescription.maxWidth} 
                                : {name: mediaDescription.name};
                const maxWidth = media.maxWidth;
                let doAdd = false;
                if (maxWidth) {
                    if ((typeof maxWidth === 'number' || maxWidth instanceof Number)
                            && !maxWidths.includes(maxWidth)) {
                        maxWidths.push(maxWidth);
                        doAdd = true;
                    }
                }
                else {
                    if (!biggerMedia) {
                        biggerMedia = media;
                        doAdd = true;
                    }
                }
                if (doAdd) {
                    mediaList[maxWidth] = media;
                }
            }
            maxWidths.sort((a, b) => a - b);
            for (const max of maxWidths) {
                result.push(mediaList[max]);
            }
            if (biggerMedia) {
                result.push(biggerMedia);
            }
        }
        this.#listOfMedia = result;
        if (this.#listOfMedia.length > 0) {
            console.info('media config: ', this.#listOfMedia);
        }
        this.#resizeListener();
    }
    
    findMedia(mediaName) {
        let result;
        for (const media of this.#listOfMedia) {
            if (media.name === mediaName) {
                result = media;
                break;
            }
        }
        return result;
    }
    
    getListOfMedia() {
        return [...this.#listOfMedia];
    }
       
    getMedia() {
        return this.#media;
    }
    
    /** @return its off */
    onMedia(action) {
        return this.#mediaActions.add(action);
    }
    
    /** @return its off */
    onResize(action) {
        return this.#resizeActions.add(action);
    }
    
    #applyResizeActions() {
        this.#resizeActions.run({event: 'resize', 
                                 media: this.getMedia(),
                                 width: window.innerWidth,
                                 height: window.innerHeight});
    }
    
    #resizeListener() {
        // list of media existing
        if (this.#listOfMedia.length) {
            // look for media
            let mediaToShow = null;
            const width = window.innerWidth;
            const biggerMedia = this.#listOfMedia[this.#listOfMedia.length - 1];
            mediaToShow = !biggerMedia.maxWidth ? biggerMedia : undefined;
            for (const media of this.#listOfMedia) {
                const maxWidth = media.maxWidth;
                if (maxWidth && width <= maxWidth) {
                    mediaToShow = media;
                    break;
                }
            }
            // throw event -> onMedia and onResize
            if (this.#media !== null) {
                if (mediaToShow != this.#media) {
                    console.info('media: ' + this.#media.name + ' -> ' + mediaToShow.name);
                    const handler = {event: 'media',
                                     previousMedia: this.#media,
                                     media: mediaToShow,
                                     width: width,
                                     height: window.innerHeight};
                    this.#media = mediaToShow;
                    this.#mediaActions.run(handler);
                    handler.event = 'resize';
                    this.#resizeActions.run(handler);
                }
                else {
                    this.#applyResizeActions();
                }
            }
            // initialize current media
            else {
                this.#media = mediaToShow;
            }
        }
        // only on resize event
        else {
            this.#applyResizeActions();
        }
    }
}

const responsiveService = new ResponsiveService(config.mediaDescriptions);

export function initialize(mediaDescriptions) {
     responsiveService.initalistMediaList(mediaDescriptions);
}

export function getListOfMedia() {
    return responsiveService.getListOfMedia();
}

export function getMedia() {
    return responsiveService.getMedia();
}

export function findMedia(mediaName) {
    return responsiveService.findMedia(mediaName);
}

export function onMedia(callback) {
    return responsiveService.onMedia(callback);
}

export function onResize(callback) {
    return responsiveService.onResize(callback);
}

