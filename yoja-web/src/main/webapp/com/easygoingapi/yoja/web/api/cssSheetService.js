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
const sectionService = yojaWeb.sectionService;

class CssSheetService {
    
    constructor() {
    }
    
    getProperties(key) {
        const result = [];
        if (key && key.startsWith('--')) {
            this.walkCssRule(h => {
                 const styleElements = h.cssRule.style;
                 if (styleElements) {
                    for (const styleElement of styleElements) {
                        if (key === styleElement) {
                            const selectors = [];
                            for (const selector of h.cssRule.selectorText.split(',')) {
                                selectors.push(selector.trim());
                            }
                            result.push({section: h.section,
                                         media: h.media,
                                         path: h.path,
                                         sheet: h.sheet,
                                         cssRule: h.cssRule,
                                         selectors: selectors,
                                         key: key,
                                         isApplied: function() {
                                            let result = true;
                                            if (h.media) {
                                                result = window.matchMedia(h.media).matches;
                                            }
                                            return result;
                                         },
                                         getPropertyValue: function() {
                                            return styleElements.getPropertyValue(key);
                                         },
                                         updatePropertyValue: function(value) {
                                            styleElements.setProperty(key, value);
                                         }})
                        }
                    }
                 }
            })
        }
        return result;
    }
    
    updateProperties(key, value) {
        for (const property of this.getProperties(key)) {
            property.updatePropertyValue(value);
        }
    }
    
    walkCssSheet(consumer) {
        sectionService.walk(document, section => {
            //const sheets = section.shadowTag.adoptedStyleSheets
            const sheetEntities = section?.css?.sheetEntities;
            if (sheetEntities) {
                for (let i = 0; i < sheetEntities.length; i++) {
                    const sheetEntity = sheetEntities[i]
                    const sheet = sheetEntity.sheet
                    const path = sheetEntity.path
                    const media = sheetEntity.media
                    consumer({section: section,
                              media: media,
                              path: path,
                              sheet: sheet,
                              isApplied: function() {
                                 let result = true;
                                 if (media) {
                                    result = window.matchMedia(media).matches;
                                 }
                                 return result;
                               }})
                }
            }
        })
    }
    
    walkCssRule(consumer) {
        sectionService.walk(document, section => {
           //const sheets = section.shadowTag.adoptedStyleSheets
           const sheetEntities = section?.css?.sheetEntities;
           if (sheetEntities) {
               for (let i = 0; i < sheetEntities.length; i++) {
                   const sheetEntity = sheetEntities[i];
                   const sheet = sheetEntity.sheet;
                   const cssRules = sheet.cssRules;
                   if (cssRules) {
                       const path = sheetEntity.path;
                       const media = sheetEntity.media;
                       for (const cssRule of cssRules) {
                            consumer({section: section,
                                      sheet: sheet,
                                      media: media,
                                      path: path,
                                      cssRule: cssRule,
                                      isApplied: function() {
                                         let result = true
                                         if (media) {
                                            result = window.matchMedia(media).matches;
                                         }
                                         return result;
                                      }})
                       }
                   }
               }
           }
        })
   }
    
}

const cssSheetService = new CssSheetService()

export function getProperties(key) {
    return cssSheetService.getProperties(key);
}

export function updateProperties(key, value) {
    cssSheetService.updateProperties(key, value);
}

export function walkCssSheet(consumer) {
    cssSheetService.walkCssSheet(consumer);
}

export function walkCssRule(consumer) {
    cssSheetService.walkCssRule(consumer);
}
