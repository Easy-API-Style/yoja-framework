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
'use strict';
const domUtil = await import(yojaWeb.path('../util/domUtil.js'));
const jsUtil = await import(yojaWeb.path('../util/javascriptUtil.js'));
const languageLoader = await import(yojaWeb.path('../loader/languageLoader.js'));
const {Listeners} = await import(yojaWeb.path('../util/listenerUtil.js'));

const sectionService = yojaWeb.sectionService;
const config = yojaWeb.config;

function hasI18n(tag) {
    return Boolean(tag?.getAttributeNames?.().some(name => name.startsWith('yw-i18n')));
}

class LanguageService {

    #localStorageKey = 'ywLanguage';
    // the language, when the storage cannot keep it (disabled, a sandboxed frame...)
    #language = null;

    // the application's actions: each onXXX(action) gives back its off (listenerUtil)
    #onChangeActions = new Listeners('languageService change');
    #onTranslateActions = new Listeners('languageService translate');

    // path -> a promise of its texts: key -> its <yw-text>, the last one (an import is an include: the file's own, over
    // what it includes -- languageLoader)
    #texts = new Map();
    // tag -> its texts before any translation (attribute name -> value, null -> its html): what a key not translated
    // gets back -- not the text of another language
    #initials = new WeakMap();

    /**
     * The language at start: the stored one, else the configured one (then stored), and the page translated --
     * awaited by YojaWeb before the page is shown.
     */
    start() {
        const stored = this.getLanguage();
        const language = stored || config?.defaultLanguage || null;
        if (language && language !== stored) {
            this.#storeLanguage(language);
        }
        return this.update(language);
    }

    #applyOnChange(handler) {
        this.#onChangeActions.run(handler);
    }

    onChange(action) {
        return this.#onChangeActions.add(action);
    }

    async setLanguage(language) {
        if (!language) {
            return {updated: false, language: null};
        }
        if (language === this.getLanguage()) {
            return {updated: false, language: language};
        }
        this.#storeLanguage(language);
        try {
            await this.update(language);
        }
        catch (error) {
            throw new Error('set language failed: ' + language, {cause: error});
        }
        return {updated: true, language: language};
    }

    getLanguage() {
        try {
            return localStorage.getItem(this.#localStorageKey) ?? this.#language;
        }
        catch {
            return this.#language;
        }
    }

    #storeLanguage(language) {
        this.#language = language;
        try {
            localStorage.setItem(this.#localStorageKey, language);
        }
        catch (error) {
            console.warn('languageService: the language is not stored, kept for this page only', error);
        }
    }

    hasLanguage() {
        return Boolean(this.getLanguage());
    }

    async update(language) {
        const futures = [];
        if (language) {
            // the first sections only: each one updates its own
            for (const section of sectionService.children(document)) {
                futures.push(this.#updateSection(section, language));
            }
        }
        return Promise.all(futures)
                      .then(() => this.#applyOnChange({updated: true,
                                                       language: language}));
    }

    /** Translates the tags given and their own -- not the rest of their section. */
    async updateFrom(tags, language) {
        const futures = [];
        if (language && tags) {
            const sources = Array.isArray(tags) || tags instanceof NodeList ? tags : [tags];
            for (const source of sources) {
                if (source?.nodeType !== Node.ELEMENT_NODE) {
                    continue;
                }
                const section = sectionService.closest(source);
                if (!section) {
                    continue;
                }
                if (section.tag === source) {
                    futures.push(this.#updateSection(section, language));
                }
                else {
                    futures.push(this.#updateTags(this.#i18nTags(source, true), section, language));
                    for (const childSection of sectionService.children(source)) {
                        futures.push(this.#updateSection(childSection, language));
                    }
                }
            }
        }
        return Promise.all(futures)
                      .then(() => this.#applyOnChange({updated: true,
                                                       language: language,
                                                       fromTags: tags}));
    }

    #updateSection(section, language) {
        const futures = [this.#updateTags(this.#i18nTags(section.tag, false), section, language)];
        for (const childSection of section.childrenSections()) {
            futures.push(this.#updateSection(childSection, language));
        }
        return Promise.all(futures);
    }

    /** @return the tags to translate under one -- not those of its sections --, itself too when asked */
    #i18nTags(tag, itself) {
        const result = [];
        if (itself && hasI18n(tag)) {
            result.push(tag);
        }
        domUtil.walkTag(tag, child => {
            if (hasI18n(child)) {
                result.push(child);
            }
        });
        return result;
    }

    async #updateTags(tags, section, language) {
        if (tags.length) {
            const texts = await this.#sectionTexts(section);
            for (const tag of tags) {
                this.#updateTag(tag, section, texts, language);
            }
        }
    }

    /** @return a promise of the section's texts -- null: it has none, or its file could not be loaded (told) */
    #sectionTexts(section) {
        const languagePath = this.#getLanguagePath(section);
        if (!languagePath) {
            return Promise.resolve(null);
        }
        return this.#textsOf(languagePath).catch(error => {
            console.error('language file not loaded: ' + languagePath, error);
            return null;
        });
    }

    /** @return a promise of the file's texts, indexed once: key -> its last <yw-text> */
    #textsOf(path) {
        let texts = this.#texts.get(path);
        if (!texts) {
            texts = languageLoader.load(path).then(xmlEntity => {
                const result = new Map();
                for (const text of xmlEntity.xml.querySelectorAll('[key]')) {
                    result.set(text.getAttribute('key'), text);
                }
                return result;
            });
            // failed: not kept, a later load tries again
            texts.catch(() => this.#texts.delete(path));
            this.#texts.set(path, texts);
        }
        return texts;
    }

    #updateTag(tag, section, texts, language) {
        // a copy: setting an attribute changes the tag's own
        for (const attribute of [...tag.attributes]) {
            const name = attribute.name;
            if (name === 'yw-i18n') {
                const initial = this.#initialOf(tag, null);
                const value = this.#applyOnTranslate({tag: tag,
                                                      key: attribute.value,
                                                      value: this.#findTranslation(texts, attribute.value, language),
                                                      type: 'tag',
                                                      section: section});
                tag.innerHTML = value ?? initial;
            }
            else if (name.startsWith('yw-i18n-')) {
                const attributeName = name.substring('yw-i18n-'.length);
                const initial = this.#initialOf(tag, attributeName);
                const value = this.#applyOnTranslate({tag: tag,
                                                      key: attribute.value,
                                                      value: this.#findTranslation(texts, attribute.value, language),
                                                      attribute: attributeName,
                                                      type: 'attribute',
                                                      section: section});
                tag.setAttribute(attributeName, value ?? initial);
            }
        }
    }

    /** @return the tag's text (attribute null: its html) before its first translation */
    #initialOf(tag, attributeName) {
        let initials = this.#initials.get(tag);
        if (!initials) {
            initials = new Map();
            this.#initials.set(tag, initials);
        }
        if (!initials.has(attributeName)) {
            initials.set(attributeName, attributeName === null ? tag.innerHTML : tag.getAttribute(attributeName) ?? '');
        }
        return initials.get(attributeName);
    }

    #getLanguagePath(section) {
        let result;
        let _section = section;
        while (_section) {
            result = _section.languagePath;
            if (result || !domUtil.isEmptyAttribute(_section.tag, 'yw-language')) {
                break;
            }
            _section = _section.parentSection();
        }
        return result;
    }

    /** @return the key's text in the language -- null: not found */
    #findTranslation(texts, key, language) {
        const text = texts && language && key ? texts.get(key) : null;
        if (text) {
            for (const child of text.children) {
                if (child.tagName === language) {
                    return child.innerHTML;
                }
            }
        }
        return null;
    }

    #applyOnTranslate(handler) {
        for (const action of this.#onTranslateActions.actions()) {
            const value = this.#onTranslateActions.call(action, handler);
            if (value !== undefined) {
                handler.value = value;
            }
        }
        return handler.value;
    }

    onTranslate(action) {
        return this.#onTranslateActions.add(action);
    }

    /** @return a promise of a translator(key [, language]) of the file -- its imports included, as for a section */
    loadTranslator(path) {
        return this.#textsOf(path)
                   .then(texts => (key, language) => this.#findTranslation(texts, key, language || this.getLanguage()));
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
        });
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

/** The language at start, the page translated: YojaWeb's own, before the page is shown. */
export function start() {
    return languageService.start();
}

export function setLanguage(value) {
    return languageService.setLanguage(value);
}

export function getLanguage() {
    return languageService.getLanguage();
}

/** @return its off */
export function onLanguageChange(action) {
    return languageService.onChange(action);
}

export function refresh(language) {
    language = language ? language : getLanguage();
    return languageService.update(language);
}

export function refreshFrom(tags, language) {
    language = language ? language : getLanguage();
    return languageService.updateFrom(tags, language);
}

/** @return its off */
export function onLanguageTranslate(action) {
    return languageService.onTranslate(action);
}

export function loadTranslator(path) {
    return languageService.loadTranslator(path);
}

export function log(path) {
    return languageLoader.load(path)
                         .then(v => languageService.logXmlPaths(v));
}
