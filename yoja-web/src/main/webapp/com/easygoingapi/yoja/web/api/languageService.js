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
const httpClient = await import(yojaWeb.path('../api/httpClient.js'));
const domUtil = await import(yojaWeb.path('../util/domUtil.js'));
const jsUtil = await import(yojaWeb.path('../util/javascriptUtil.js'));
const languageLoader = await import(yojaWeb.path('../loader/languageLoader.js'));

const sectionService = yojaWeb.sectionService;
const config = yojaWeb.config;

class LanguageService {

    #localStorageKey = 'ywLanguage';

    #onChangeActions = [];
    #onTranslateActions = [];

    constructor() {
        yojaWeb.onDocumentReady(() => {
            let language = null;
            if (localStorage.getItem(this.#localStorageKey)) {
                language = localStorage.getItem(this.#localStorageKey);
            }
            else if (config?.defaultLanguage) {
                language = config.defaultLanguage;
            }
            this.setLanguage(language)
                .then(h => {
                    if (!h.updated) {
                        this.update(language)
                    }
                });
        })
        yojaWeb.onTagReady(tags => this.#onTagReady(tags));
    }
    
    #onTagReady(tags) {
        this.updateFrom(tags, this.getLanguage());
    }
    
    #applyOnChange(handler) {
        for (const action of this.#onChangeActions) {
            action(handler);
        }
    }

    onChange(action) {
        this.#onChangeActions.push(action);
    }

    setLanguage(language) {
        return new Promise((resolve, reject) => {
            try {
                if (language) {
                    const langFromStorage = localStorage.getItem(this.#localStorageKey);
                    if (langFromStorage !== language) {
                        localStorage.setItem(this.#localStorageKey, language);
                        this.update(language)
                            .then(() => {
                                resolve({ updated: true,
                                          language: language });
                            })
                            .catch(error => reject({message: 'set langauge failed: ' + language, 
                                                    cause: error}));
                    }
                    else {
                        resolve({ updated: false,
                                  language: language });
                    }
                }
                else {
                    resolve({ updated: false, language: null  });
                }
            }
            catch(error) {
                reject({message: 'translate to ' + language + ' failed', 
                        cause: error});
            }
        })
    }

    getLanguage() {
        return localStorage.getItem(this.#localStorageKey);
    }
    
    hasLanguage() {
        return localStorage.getItem(this.#localStorageKey) ? true : false;
    }

    async update(language) {
        const futures = [];
        if (language) {
            sectionService.walk(document, section => futures.push(this.#updateSection(section, language)));
        } 
        return Promise.all(futures) 
                      .then(() => this.#applyOnChange({ updated: true,
                                                        language: language }));
    }
    
    async updateFrom(tags, language) {
        const futures = [];
        if (language && tags) {
            // tagsToUpdate
            const tagsToUpdate = [];
            if (Array.isArray(tags)) {
                tagsToUpdate.push(...tags);
            }
            else {
                tagsToUpdate.push(tags);
            }
            // sectionsToUpdate
            const sectionsToUpdate = [];
            for (const tagToUpdate of tagsToUpdate) {
                if (tagToUpdate.nodeName !== '#text') {
                    const sectionParent = sectionService.closest(tagToUpdate);
                    if (sectionParent && !sectionsToUpdate.includes(sectionParent)) {
                        sectionsToUpdate.push(sectionParent);
                    }
                }
            }
            // updateSection
            for (const sectionToUpdate of sectionsToUpdate) {
                futures.push(this.#updateSection(sectionToUpdate, language));
            }
        }
        return Promise.all(futures) 
                      .then(() => this.#applyOnChange({ updated: true,
                                                        language: language,
                                                        fromTags: tags }));
    }

    #updateSection(section, language) {
        const futures = [];
        // tags
        const tags = [];
        section.walkTag(tag => {
            if (tag && tag.attributes) {
                for (const attribute of tag.attributes) {
                    const name = attribute.name;
                    if (name.startsWith('yw-i18n')) {
                        tags.push(tag);
                    }
                }
            }
        })
        // values found
        const languagePath = this.#getLanguagePath(section);
        if (languagePath) {
            futures.push(languageLoader.load(languagePath)
                                       .then(xmlEntity => {
                                            // this.logXmlPaths(xmlEntity)
                                            this.#updateTags(tags, section, xmlEntity.xml, language);
                                       })
                                       .catch(error => {
                                           console.error('language file not loaded: ' + languagePath, {cause: error});
                                           this.#updateTags(tags, section, null, language);
                                       }))
        }
        // values not found
        else {
            this.#updateTags(tags, section, null, language);
        }
        // children sections
        for (const childSection of section.childrenSections()) {
            futures.push(this.#updateSection(childSection, language));
        }
        return Promise.all(futures);
    }
    
    #updateTags(tags, section, values, language) {
        for (const tag of tags) {
            this.#updateTag(tag, section, values, language);
        }
    }
    
    #updateTag(tag, section, values, language) {
        if (tag && tag.attributes) {
            for (const attribute of tag.attributes) {
                const name = attribute.name;
                if (name === 'yw-i18n') {
                    let value = this.#findTranslation(values, attribute.value, language);
                    value = this.#applyOnTranslate({tag: tag, 
                                                    key: attribute.value,
                                                    value: value,
                                                    type: 'tag',
                                                    section: section});
                    if (value) {
                        tag.innerHTML = value;
                    }
                }
                else if (name.startsWith('yw-i18n-')) {
                    const attributeName = name.substring('yw-i18n-'.length);
                    let value = this.#findTranslation(values, attribute.value, language);
                    value = this.#applyOnTranslate({tag: tag, 
                                                    key: attribute.value,
                                                    value: value,
                                                    attribute: attributeName,
                                                    type: 'attribute',
                                                    section: section});
                    if (value !== null) {
                        tag.setAttribute(attributeName, value);
                    } 
                    else {
                        tag.setAttribute(attributeName, '');
                    }
                }
            }
        }
    }
    
    #getLanguagePath(section) {
        let result;
        let _section = section;
        while (true) {
            if (!_section) {
                break;
            }
            result = _section.languagePath
            if (result) {
                break;
            }
            else if (domUtil.isEmptyAttribute(_section.tag, 'yw-language')) {
                _section = _section.parentSection();
            }
            else {
                break;
            }
        }
        return result;
    }

    #findTranslation(values, key, language) {
        let result = null;
        if (values && language && key) {
            const text = values.querySelectorAll('[key="' + key + '"]');
            if (text && text.length) {
                for (const child of text[0].children) {
                    if (child.tagName === language) {
                        result = child.innerHTML;
                        break;
                    }
                }
            }
        }
        return result;
    }

    #applyOnTranslate(handler) {
        if (this.#onTranslateActions.length) {
            for (const action of this.#onTranslateActions) {
                const value = action(handler);
                if (value !== undefined) {
                    handler.value = value;
                }
            }
        }
        return handler.value;
    }
    
    onTranslate(action) {
        this.#onTranslateActions.push(action);
    }

    loadTranslator(path) {
        const itself = this;
        return httpClient.get({url: path, fetchAs: 'text'})
                         .then(response => {
                            const values = domUtil.parseXml(response.body);
                            return function(key, language) {
                                let result;
                                if (language) {
                                    result = itself.#findTranslation(values, key, language);
                                }
                                else {
                                    result = itself.#findTranslation(values, key, itself.getLanguage());
                                }
                                return result;
                            }
                          })
    }
    
    logXmlPaths(xmlEntity) {
        const tab = 1;
        const rootPath = xmlEntity.rootPath;
        console.debug('<xml path="' + jsUtil.onlyUrlPath(rootPath) + '" />');
        xmlEntity.graph.walkChildren(rootPath, h => {
            const deep = h.deep;
            let message = '<xml path="' + jsUtil.onlyUrlPath(h.node) + '"';
            if (h.recursive) {
                message = message + ' recursive';
            }
            message = message + ' />';
            console.debug(' '.repeat(deep * tab) + message);
        })
        if (xmlEntity.paths.length > 1) {
            console.debug('<xml-cascade>');
            for (const path of xmlEntity.paths) {
                console.debug(' '.repeat(tab) + '<xml path="' + jsUtil.onlyUrlPath(path) + '" />');
            }
            console.debug('</xml-cascade>');
        }
    }

}

const languageService = new LanguageService();

export function setLanguage(value) {
    return languageService.setLanguage(value);
}

export function getLanguage() {
    return languageService.getLanguage();
}

export function onLanguageChange(action) {
    languageService.onChange(action);
}

export function refresh(language) {
    language = language ? language : getLanguage();
    return languageService.update(language);
}

export function refreshFrom(tags, language) {
    language = language ? language : getLanguage();
    return languageService.updateFrom(tags, language);
}

export function onLanguageTranslate(action) {
    languageService.onTranslate(action);
}

export function loadTranslator(path) {
    return languageService.loadTranslator(path);
}

export function log(path) {
    return languageLoader.load(path)
                         .then(v => languageService.logXmlPaths(v));
}
