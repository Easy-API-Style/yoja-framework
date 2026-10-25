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
const apiVersion = '1.6.0';
console.info('yojaWeb apiVersion: ', apiVersion);
const scriptTags = [...document.querySelectorAll('script[type="module"]')];
let scriptTag = scriptTags.find(s => s.src && new URL(s.src).href === import.meta.url);
if (!scriptTag) {
    scriptTag = scriptTags.find(s => s?.getAttribute('yw-config-path'));
}

const sectionDescriptions = [];

// error: the actions given while the config loads -- {action, off}, off once they are listeners (errorActions)
const earlyErrorActions = [];

/**
 * @return an Error -- its stack, its message told -- holding the details given (path, httpStatus, nodes...): a
 *         rejection's reason; {@code cause} (or {@code error}) its cause
 */
function yojaError(message, details = {}) {
    const error = new Error(message, {cause: details.cause ?? details.error});
    Object.assign(error, details);
    return error;
}
let yojaWebReady = false;
window.yojaWeb = {};
window.yojaWeb.onError = action => {
    const early = {action: action, off: null};
    earlyErrorActions.push(early);
    return () => early.off ? early.off() : earlyErrorActions.splice(earlyErrorActions.indexOf(early) >>> 0, 1);
}
window.yojaWeb.isReady = function() {
    return yojaWebReady;
}

// constant
const sectionDirectiveName = 'yw-section';
const includeDirectiveName = 'yw-include';
const directiveAttributeId = 'yw-section-id';

// path
let version = null;

/**
 * A path of the application, its version added: an url of another origin is left as it is; one of the page's
 * origin is its path -- its parameters and hash kept; the parameters added after the ones it holds.
 *
 * @param options {version (true by default), apiVersion, force (a date: never from a cache)}
 */
function formatPath(path, options) {
    // an option not given keeps its default: {force: true} is versioned too
    const addApiVersion = options?.apiVersion ?? false;
    const addVersion = options?.version ?? true;
    const addDate = options?.force ?? false;
    if (!path) {
        return path;
    }
    if (URL.canParse(path) && URL.parse(path).origin !== window.location.origin) {
        return path;   // another origin's: not the application's to version
    }
    const url = new URL(path, window.location.href);
    if (addVersion && version && !url.searchParams.has('version')) {
        url.searchParams.append('version', version);
    }
    if (addApiVersion) {
        url.searchParams.append('apiVersion', apiVersion);
    }
    if (addDate) {
        url.searchParams.append('date', Date.now());
    }
    // a relative path kept relative: resolved by its caller, as it was
    const relative = !URL.canParse(path) && !path.startsWith('/');
    const pathname = relative ? path.split(/[?#]/)[0] : url.pathname;
    return pathname + url.search + url.hash;
}

/**
 * A path a file declares (yw-css, yw-slot...), from that file: a relative one -- './x', '../x' or 'x' -- resolved
 * from its folder, '.' and '..' resolved (one file, one path); then versioned (formatPath).
 *
 * @param fromPath the file declaring it (none: the page)
 */
function formatPathFrom(path, fromPath, options) {
    if (!path) {
        return undefined;
    }
    if (URL.canParse(path) && URL.parse(path).origin !== window.location.origin) {
        return path;
    }
    const from = new URL(fromPath || window.location.pathname, window.location.href);
    const url = new URL(path, from);
    return formatPath(url.pathname + url.search + url.hash, options);
}

// config
let config = {};

const configPath = scriptTag?.getAttribute('yw-config-path');
if (configPath) {
    try {
        const configModule = await import(formatPath(configPath, {force: true, version: false}));
        config = configModule.default || {};
    }
    catch (error) {
        console.warn('yojaWeb no config file: ' + configPath, {cause: error});
    }
}

console.info('yojaWeb config: ', config);
if (config.version) {
    version = config.version;
}

/**
 * @param predicate a function of the element -- none (null, undefined): every element
 * @return whether the element is one the predicate keeps
 * @throws TypeError when the predicate is something else: a mistake said, not every element kept unseen
 */
function test(predicate, element) {
    if (!element) {
        return false;
    }
    if (predicate === undefined || predicate === null) {
        return true;
    }
    if (predicate instanceof Function) {
        return Boolean(predicate(element));
    }
    throw new TypeError('yojaWeb: a predicate is a function, not ' + typeof predicate + ': ' + predicate);
}

// dom 
const domUtil = await import(formatPath('./util/domUtil.js'));
const jsUtil = await import(formatPath('./util/javascriptUtil.js'));

// listeners: every onXXX(action) gives back its off (listenerUtil)
const listenerUtil = await import(formatPath('./util/listenerUtil.js'));
const errorActions = new listenerUtil.Listeners('YojaWeb error', {report: false});
for (const early of earlyErrorActions) {
    early.off = errorActions.add(early.action);
}

function logSection(section, option) {
    const directiveName = section.include ? includeDirectiveName : sectionDirectiveName;
    const level = option && option.level ? option.level : 'debug';
    const tab = option && option.tab ? option.tab : 2;
    let message = '<' + directiveName
                   + ' index="' + section.index + '"'
                   + ' deep="' + section.deep  + '"'
                   + ' inside="' + jsUtil.onlyUrlPath(section.fromPath) + '"'
                   + ' tag="' + section.tagName + '"';
    if (section.id) {
        message = message + ' id="' + section.id + '"';
    }
    if (section.controler) {
        message = message + ' controler="' + jsUtil.onlyUrlPath(section.controler.path) + '"';
    }
    if (section.languagePath) {
        message = message  + ' language="' + jsUtil.onlyUrlPath(section.languagePath) + '"';
    }
    if (section.include) {
        message = message + ' include="' + jsUtil.onlyUrlPath(section.include.path) + '"';
    }
    if (section.slot) {
        message = message + ' slot="' + jsUtil.onlyUrlPath(section.slot.path) + '"';
    }
    if (section.recursive) {
        message = message + ' recursive';
    }
    const cssRootPath = section?.css?.rootPath
    if (cssRootPath) {
        message = message + ' css="' + jsUtil.onlyUrlPath(cssRootPath) + '"';
    }
    else if (section.inheritedCss) {
        message = message + ' css';
    }
    if (option && option.open) {
        message = message + ' >';
    }
    else {
        message = message + ' />';
    }
    if ('info' === level) {
        console.info(' '.repeat(section.deep * tab) + message);
    }
    else {
        console.debug(' '.repeat(section.deep * tab) + message);
    }
}

function logCss(section, tab = 1) {
    logSection(section, {tab: 0, open: true, level: 'info'});
    
    let inherited = false;
    if (section?.inheritedCss?.rootPath) {
        inherited = true;
        section.css = section.inheritedCss;
    } 
    const cssRootPath = section?.css?.rootPath;
    const cssDeep = 1 + (section.deep * tab);
    if (cssRootPath) {
        console.info(' '.repeat(cssDeep * tab) + '<css path="' + jsUtil.onlyUrlPath(cssRootPath) + '"' + (inherited ? ' inherited' : '') +  ' />');
        section.css.graph.walkChildren(cssRootPath, h => {
            const deep = cssDeep + h.deep;
            // a file is its node's id (cssLoader)
            let message = '<css path="' + jsUtil.onlyUrlPath(section.css.graph.id(h.node)) + '"';
            if (h.recursive) {
                message = message + ' recursive';
            }
            message = message + ' />';
            console.info(' '.repeat(deep * tab) + message);
        })
        if (section.css.sheetEntities.length > 1) {
            console.info(' '.repeat(cssDeep * tab) + '<css-cascade>');
            for (const cssSheetEntity of section.css.sheetEntities) {
                if (cssSheetEntity.media) {
                    const medias = cssSheetEntity.medias ?? [cssSheetEntity.media];   // every one along its path
                    console.info(' '.repeat((cssDeep + 1) * tab)
                                    + '<css path="' + jsUtil.onlyUrlPath(cssSheetEntity.path) + '"'
                                    + ' media="' + cssSheetEntity.media +'"'
                                    + ' active="' + medias.every(media => window.matchMedia(media).matches) + '" />');
                }
                else {
                    console.info(' '.repeat((cssDeep + 1) * tab)
                                    + '<css path="' + jsUtil.onlyUrlPath(cssSheetEntity.path) + '" />');
                }
            }
            console.info(' '.repeat(cssDeep * tab) + '</css-cascade>');
        }
        else if (section.css.sheetEntities.length == 0) {
            console.info(' '.repeat((cssDeep + 1) * tab) + '<css empty />');
        }
    }
    console.info('</' + sectionDirectiveName + '>');
}

// class
class ControlerService {
    
    #sectionService;
    
    constructor(sectionService) {
        this.#sectionService = sectionService;
    }
    
    #manageParameters(parameter_1, parameter_2) {
        let tag = null;
        let predicate = null;
        if (parameter_1 instanceof Object
              && (parameter_1.tag || parameter_1.predicate)) {
            tag = parameter_1.tag;
            predicate = parameter_1.predicate;
        }
        else {
            tag = parameter_1;
            predicate = parameter_2;
        }
        return {tag: tag ? tag : document, predicate: predicate};
    }
    
    first(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate);
        
        let result = null;
        const consumer = controler => {
            if (test(parameters.predicate, controler)) {
                result = controler;
                return false;
            }
        }
        this.walk(parameters.tag, consumer);
        return result;
    }
    
    find(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate);
        
        const result = [];
        const consumer = controler => {
            if (test(parameters.predicate, controler)) {
                result.push(controler);
            }
        }
        this.walk(parameters.tag, consumer);
        return result;
    }
    
    children(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate)
        
        const children = domUtil.childrenSection(parameters.tag)
        const result = []
        for (const child of children) {
            const controler = child.controler
            if (test(parameters.predicate, controler)) {
                result.push(controler)
            }
        }
        return result
    }
    
    closest(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate);
        
        let result = null;
        let previousTag = parameters.tag;
        while (true) {
            const section = this.#sectionService.closest(previousTag);
            if (section 
                  && section.controler
                  && test(parameters.predicate, section.controler)) {
                result = section.controler;
                break;
            }
            else if (section) {
                previousTag = domUtil.parent(section.tag);
            }
            else {
                break;
            }
        }
        return result;
    }
    
    parent(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate);
        
        let result = null;
        let previousTag = parameters.tag;
        while (true) {
            const section = this.#sectionService.parent(previousTag);
            if (section 
                  && section.controler
                  && test(parameters.predicate, section.controler)) {
                result = section.controler;
                break;
            }
            else if (section) {
                previousTag = section.tag;
            }
            else {
                break;
            }
        }
        return result;
    }
        
    parents(tag, predicate) {
        const parameters = this.#manageParameters(tag, predicate);
        
        const parents = this.#sectionService.parents(parameters.tag);
        const result = [];
        for (const parent of parents) {
            const controler = parent.controler;
            if (test(parameters.predicate, controler)) {
                result.push(controler);
            }
        }
        return result;
    }
    
    root(tag) {
        let result = null;
        const parents = this.parents(tag);
        if (parents.length) {
            result = parents[parents.length - 1];
        }
        return result;
    }
    
    walk(tag, consumer) {
        const controlerConsumer = section => {
            if (section.controler) {
                return consumer(section.controler);
            }
        }
        this.#sectionService.walk(tag, controlerConsumer);
    }
    
}

class SectionService {
    
    first(tag, predicate) {
        let result = null;
        const consumer = section => {
            if (test(predicate, section)) {
                result = section;
                return false;
            }
        }
        this.walk(tag, consumer);
        return result ;
    }
    
    find(tag, predicate) {
        const result = [];
        const consumer = section => {
            if (test(predicate, section)) {
                result.push(section);
            }
        }
        this.walk(tag, consumer);
        return result;
    }
    
    children(tag, predicate) {
        const children = domUtil.childrenSection(tag);
        const result = [];
        for (const child of children) {
            if (test(predicate, child)) {
                result.push(child);
            }
        }
        return result;
    }
    
    closest(tag, predicate) {
        let result = null;
        let previousTag = tag;
        while (true) {
            const section = domUtil.closestSection(previousTag);
            if (section
                  && test(predicate, section)) {
                result = section;
                break;
            }
            else if (section) {
                previousTag = domUtil.parent(section.tag);
            }
            else {
                break;
            }
        }
        return result;
    }
    
    parent(tag, predicate) {
        let result = null;
        let previousTag = tag;
        while (true) {
            const section = domUtil.parentSection(previousTag);
            if (section 
                  && test(predicate, section)) {
                result = section;
                break;
            }
            else if (section) {
                previousTag = section.tag;
            }
            else {
                break;
            }
        }
        return result;
    }
    
    parents(tag, predicate) {
        const parents = domUtil.parentSections(tag);
        const result = [];
        for (const parent of parents) {
            if (test(predicate, parent)) {
                result.push(parent);
            }
        }
        return result;
    }
    
    root(tag) {
        let result = null;
        const parents = this.parents(tag);
        if (parents.length) {
            result = parents[parents.length - 1];
        }
        return result;
    }
    
    walk(tag, consumer) {
        return domUtil.walkSection(tag, consumer);
    }
    
}

const controlerReadyActions = {};
const pageReadyActions = {};

// id -> {tag, cleanups, ready}: the sections built, until they leave the page (forgetGoneSections) -- ready once
// their ready actions have run
const builtSections = new Map();

/**
 * Forgets the sections no more in the page -- their description, their ready actions, their media listeners:
 * kept, a page rebuilding its screens would hold every one it ever built. Only a section whose ready actions have
 * run: an addition still loading is not touched.
 */
function forgetGoneSections() {
    for (const [id, built] of builtSections) {
        // not ready: an addition still loading -- its tags maybe not in the page yet -- keeps it
        if (built.ready && !built.tag.isConnected) {
            for (const cleanup of built.cleanups) {
                cleanup();
            }
            builtSections.delete(id);
            delete sectionDescriptions[id];
            delete controlerReadyActions[id];
            delete pageReadyActions[id];
        }
    }
}

// before an event is told, the sections gone forgotten; an action failed, the error actions told
listenerUtil.hookWith({sweep: forgetGoneSections,
                       report: error => errorActions.run(error)});
const tagReadyActions = new listenerUtil.Listeners('YojaWeb tagReady');
const documentReadyActions = new listenerUtil.Listeners('YojaWeb documentReady');

class Section {

    #id;
    #sectionTag;
    #controler;
    #languagePath;
    #cssPath;
    #css;
    #slotPath;
    #path;
    
    constructor(sectionTag, 
                section) {
        this.#id = sectionTag.getAttribute(directiveAttributeId);
        controlerReadyActions[this.#id] = [];
        pageReadyActions[this.#id] = [];
        if (!builtSections.has(this.#id)) {
            builtSections.set(this.#id, {tag: sectionTag, cleanups: [], ready: false});
        }
        this.#sectionTag = sectionTag;
        
        this.#languagePath = jsUtil.onlyUrlPath(section?.languagePath);
        this.#cssPath = jsUtil.onlyUrlPath(section?.css?.rootPath);
        this.#css = {rootPath: section?.css?.rootPath,
                     sheetEntities: section?.css?.sheetEntities};
        this.#slotPath = jsUtil.onlyUrlPath(section?.slot?.path);
        this.#path = jsUtil.onlyUrlPath(section?.fromPath);
                                             
        this.#sectionTag.ywSection = this;                 
        const controlerClass = section?.controler?.service;
        if (controlerClass) {
            try {
                this.#controler = new controlerClass(this);
                this.#controler.className = function() {
                    return this.constructor.name;
                }
            }
            catch (error) {
                // the other sections built all the same: this one without its controler, said
                this.#controler = undefined;
                console.error('YojaWeb controler failed: ' + section?.controler?.path, error);
                errorActions.run(error);
            }
        }
    }
    
    /*
     * ACTION
     */
    controlerReady(action) {
        controlerReadyActions[this.#id].push(action);
    }
    
    pageReady(action) {
        pageReadyActions[this.#id].push(action);
    }

    /**
     * The off given -- an onXXX(...)'s -- runs once the section has left the page: its action no more told. Gone
     * already: at once.
     *
     * @return the off itself
     * @throws TypeError when it is not a function
     */
    track(off) {
        if (typeof off !== 'function') {
            throw new TypeError('yojaWeb: track takes an off, a function, not ' + typeof off + ': ' + off);
        }
        const built = builtSections.get(this.#id);
        if (built) {
            built.cleanups.push(off);
        }
        else {
            off();
        }
        return off;
    }
    
    /*
     * GETTER
     */
    get languagePath() {
        return this.#languagePath;
    }
    
    get cssPath() {
        return this.#cssPath;
    }
    
    get css() {
        return this.#css;
    }
    
    get slotPath() {
        return this.#slotPath;
    }
    
    get path() {
        return this.#path;
    }
    
    get controler() {
        return this.#controler;
    }
    
    /*
     * TAG
     */
    get tag() {
        return this.#sectionTag;
    }
    
    get shadowTag() {
        return this.#sectionTag.shadowRoot;
    }
    
    firstTag(selector) {
        return domUtil.firstTag(this.#sectionTag, selector);
    }

    findTags(selector) {
        return domUtil.findTags(this.#sectionTag, selector);
    }
    
    walkTag(consumer) {
        domUtil.walkTag(this.#sectionTag, consumer);
    }

    deepFirstTag(selector) {
        return domUtil.deepFirstTag(this.#sectionTag, selector);
    }
    
    deepFindTags(selector) {
        return domUtil.deepFindTags(this.#sectionTag, selector);
    }
    
    deepWalkTag(consumer) {
        domUtil.deepWalkTag(this.#sectionTag, consumer);
    }

    /*
     * SECTION
     */
    firstSection(predicate) {
       return window.yojaWeb
                    .sectionService
                    .first(this.#sectionTag, predicate);
    }
    
    findSections(predicate) {
        return window.yojaWeb                        
                     .sectionService
                     .find(this.#sectionTag, predicate);
    }
    
    childrenSections(predicate) {
        return window.yojaWeb
                     .sectionService
                     .children(this.#sectionTag, predicate);
    }
    
    closestSection(predicate) {
        return window.yojaWeb
                     .sectionService
                     .closest(this.#sectionTag, predicate);
    }
    
    parentSection(predicate) {
        return window.yojaWeb
                     .sectionService
                     .parent(this.#sectionTag, predicate);
    }
    
    parentSections(predicate) {
        return window.yojaWeb
                     .sectionService
                     .parents(this.#sectionTag, predicate);
    }

    rootSection() {
        return window.yojaWeb
                     .sectionService
                     .root(this.#sectionTag);
    }
    
    walkSection(consumer) {
        return window.yojaWeb
                     .sectionService
                     .walk(this.#sectionTag, consumer);
    }
    
    /*
     * CONTROLER
     */
    firstControler(predicate) {
        return window.yojaWeb
                     .controlerService
                     .first(this.#sectionTag, predicate);
    }
    
    findControlers(predicate) {
        return window.yojaWeb
                     .controlerService
                     .find(this.#sectionTag, predicate);
    }
    
    childrenControlers(predicate) {
        return window.yojaWeb
                     .controlerService
                     .children(this.#sectionTag, predicate);
    }
    
    closestControler(predicate) {
        return window.yojaWeb
                     .controlerService
                     .closest(this.#sectionTag, predicate);
    }
    
    parentControler(predicate) {
        return window.yojaWeb
                     .controlerService
                     .parent(this.#sectionTag, predicate);
    }
    
    parentControlers(predicate) {
        return window.yojaWeb
                     .controlerService
                     .parents(this.#sectionTag, predicate);
    }

    rootControler() {
        return window.yojaWeb
                     .controlerService
                     .root(this.#sectionTag);
    }
    
    walkControler(consumer) {
        return window.yojaWeb
                     .controlerService
                     .walk(this.#sectionTag, consumer);
    }
    
    /*
     * LOG
     */
    log() {
        const sections = [];
        const id = this.tag.getAttribute(directiveAttributeId);
        const section = {...sectionDescriptions[id]};
        section.index = 0;
        section.deep = 0;
        if (domUtil.isEmptyAttribute(this.tag, 'yw-css')) {
            section.inheritedCss = true;
        }
        sections.push(section);
        
        this.walkSection((s, i) => {
            const id = s.tag.getAttribute(directiveAttributeId);
            const section = {...sectionDescriptions[id]};
            section.index = i.index;
            section.deep = i.deep;
            if (domUtil.isEmptyAttribute(s.tag, 'yw-css')) {
                section.inheritedCss = true;
            }
            sections.push(section);
        })
        for (const section of sections) {
            logSection(section, {level: 'info'});
        }
    }
    
    logCss() {
        const id = this.tag.getAttribute(directiveAttributeId);
        const section = {...sectionDescriptions[id]};
        section.index = 0;
        section.deep = 0;
        if (domUtil.isEmptyAttribute(this.tag, 'yw-css')) { 
            let parentSection = this.parentSection();
            while (true) {
                if (parentSection) {
                    if (domUtil.isEmptyAttribute(parentSection.tag, 'yw-css')) { 
                        parentSection = parentSection.parentSection();
                    }
                    else {
                        const id = parentSection.tag.getAttribute(directiveAttributeId)
                        section.inheritedCss = sectionDescriptions[id].css;
                        break;
                    }
                }
                else {
                    break;
                }
            }
        }
        logCss(section)   
    }
    
}

// YojaWeb section
class YojaWeb {
    
    #sectionService;
    #controlerService;
    #config;
    #version;
    
    constructor(sectionService, 
                controlerService, 
                config,
                version) {
        this.#sectionService = sectionService;
        this.#controlerService = controlerService;
        this.#config = config;
        this.#version = version;
    }
    
    get sectionService() {
        return this.#sectionService;
    }
    
    get controlerService() {
        return this.#controlerService;
    }

    get config() {
        return this.#config;
    }
    
    firstTag(selector, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.deepFirstTag(tag, selector);
    }
    
    findTags(selector, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.deepFindTags(tag, selector);
    }

    walkTag(consumer, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.deepWalkTag(tag, consumer);
    }
    
    firstSectionTag(selector, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.firstTag(tag, selector);
    }

    findSectionTags(selector, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.findTags(tag, selector);
    }

    walkSectionTag(consumer, fromTag) {
        const tag = fromTag ? fromTag : document;
        return domUtil.walkTag(tag, consumer);
    }
    
    path(path, option) {
        return formatPath(path, option);
    }
    
    version() {
        return this.#version;
    }
    
    appendTag(fromTag, tags) {
        return new Promise((resolve, reject) => {
            this.#addOnlyTag('append', fromTag, tags, this.#sectionPath(fromTag), resolve, reject);
        });
    }

    prependTag(fromTag, tags) {
        return new Promise((resolve, reject) => {
            this.#addOnlyTag('prepend', fromTag, tags, this.#sectionPath(fromTag), resolve, reject);
        });
    }

    /**
     * Path of the section {@code fromTag} belongs to (its own section when
     * {@code fromTag} is a section host), walking up to the nearest ancestor
     * that has a path. Uses {@code closest} — which includes {@code fromTag}'s
     * own section — rather than {@code parents}, which only looks at ancestors.
     */
    #sectionPath(fromTag) {
        let section = this.#sectionService.closest(fromTag);
        while (section) {
            if (section.path) {
                return section.path;
            }
            section = this.#sectionService.parent(section.tag);
        }
        return undefined;
    }

    append(fromTag, path) {
        return this.#addTag('append', fromTag, path);
    }

    prepend(fromTag, path) {
        return this.#addTag('prepend', fromTag, path);
    }
    
    #addOnlyTag(mode, fromTag, tags, 
                path, resolve, reject) {
        const shadow = fromTag.shadowRoot;
        let _fromTag = fromTag;
        if (shadow) {
            _fromTag = shadow;
        }
        let nodes;
        if (Array.isArray(tags)) {
            nodes = [...tags];
        }
        else {
            nodes = [tags];
        }
        // expand any DocumentFragment into its element children (a fragment has
        // no style and is emptied by appendChild, so work on its children)
        nodes = nodes.flatMap(node => node instanceof DocumentFragment ? [...node.childNodes] : [node]);
        // hidden while they load, then shown as they were -- their own display kept -- failing too: a part that
        // could not be built is shown all the same, not left hidden
        const displays = new Map();
        for (const node of nodes) {
            if (node.style) {
                displays.set(node, node.style.display);
                node.style.display = 'none';
            }
        }
        const show = () => {
            for (const [node, display] of displays) {
                node.style.display = display;
            }
        };
        if (mode === 'append') {
            for (const node of nodes) {
                _fromTag.append(node);
            }
        }
        else {
            for (const node of [...nodes].reverse()) {   // the order given kept
                _fromTag.prepend(node);
            }
        }
        applyYojaWebOnTags(path, nodes)
            .then(() => {
                // the tags added only: not the rest of their section
                languageService.refreshFrom(nodes)
                               .then(() => {
                                   show();
                                   resolve(nodes);
                               })
                               .catch(error => {
                                   show();
                                   reject(yojaError('apply yoja on tags failed during refreshing language', {
                                       nodes: nodes,
                                       error: error
                                   }));
                               });
            })
            .catch(error => {
                show();
                reject(yojaError('apply yoja on tags failed', {
                    nodes: nodes,
                    error: error
                }));
            });
    }
    
    #addTag(mode, fromTag, path) {
        return new Promise((resolve, reject) => {
            httpClient.get({url: path, fetchAs: 'text'})
                      .then(response => {
                if (response.status === 200) {
                    const body = domUtil.parseHtml(response.body).body;
                    const children = Array.from(body.childNodes);
                    this.#addOnlyTag(mode, fromTag, children, path, resolve, reject);
                }
                else {
                    reject(yojaError('http request for append tag failed', { 
                            path: path,
                            httpStatus: response.status,
                            httpBody: response.body}));
                }
            })
            .catch(error => reject(yojaError('append tag failed', { 
                                    error: error})));
        });
    }    
    
    /** @return its off */
    onTagReady(action) {
        return tagReadyActions.add(action);
    }
    
    /** @return its off */
    onDocumentReady(action) {
        return documentReadyActions.add(action);
    }
    
    isReady() {
        return yojaWebReady;
    }
    
    /** @return its off */
    onError(action) {
        return errorActions.add(action);
    }
    
}

class YojaWebApi {

    #sectionService;
    #controlerService;
    #eventService;
    #httpClient;
    #languageService;
    #navigationService;
    #responsiveService;
    #storageService;
    #urlParameterService;
    #webSocketService;
    #cssSheetService;
    
    constructor(sectionService,
                controlerService,
                eventService,
                httpClient,
                languageService,
                navigationService,
                responsiveService,
                storageService,
                urlParameterService,
                webSocketService,
                cssSheetService) {
        this.#sectionService = sectionService;
        this.#controlerService = controlerService;
        this.#eventService = eventService;
        this.#httpClient = httpClient;
        this.#languageService = languageService;
        this.#navigationService = navigationService;
        this.#responsiveService = responsiveService;
        this.#storageService = storageService;
        this.#urlParameterService = urlParameterService;
        this.#webSocketService = webSocketService;
        this.#cssSheetService = cssSheetService;
    }
    
    get sectionService() {
        return this.#sectionService;
    }

    get controlerService() {
        return this.#controlerService;
    }

    get eventService() {
        return this.#eventService;
    }

    get httpClient() {
        return this.#httpClient;
    }
    
    get languageService() {
        return this.#languageService;
    }
    
    get navigationService() {
        return this.#navigationService;
    }
    
    get responsiveService() {
        return this.#responsiveService;
    }
    
    get storageService() {
        return this.#storageService;
    }
    
    get urlParameterService() {
        return this.#urlParameterService;
    }
    
    get webSocketService() {
        return this.#webSocketService;
    }
    
    get cssSheetService() {
        return this.#cssSheetService;
    }

}

// YojaWeb instance
const sectionService = new SectionService();
const controlerService = new ControlerService(sectionService);
window.yojaWeb = new YojaWeb(sectionService, 
                             controlerService, 
                             config,
                             apiVersion);
                             
// import
const httpClient = await import(formatPath('./api/httpClient.js'));
const cssSheetService = await import(formatPath('./api/cssSheetService.js'));
const urlParameterService = await import(formatPath('./api/urlParameterService.js'));
const languageService = await import(formatPath('./api/languageService.js'));
const storageService = await import(formatPath('./api/storageService.js'));
const navigationService = await import(formatPath('./api/navigationService.js'));
const responsiveService = await import(formatPath('./api/responsiveService.js'));
const eventService = await import(formatPath('./api/eventService.js'));
const webSocketService = await import(formatPath('./api/webSocketService.js'));

window.yojaWebApi = new YojaWebApi(sectionService,
                                   controlerService,
                                   eventService, 
                                   httpClient,
                                   languageService,
                                   navigationService,
                                   responsiveService,
                                   storageService,
                                   urlParameterService,
                                   webSocketService,
                                   cssSheetService);

/*

    LOAD

 */
const cssLoader = await import(formatPath('./loader/cssLoader.js'));
const Graph = await import(formatPath('./util/Graph.js'));

const fileGraph = Graph.newInstance();

function getIncludeMode(sectionTag) {
    let mode = sectionTag.getAttribute('yw-include-mode');
    let result = 'outer';
    if (mode) {
        if (mode === 'inner') {
            result = 'inner';
        }
        else if (mode === 'outer') {
            result = 'outer';
        }
    }
    return result;
}

/**
 * Loads the sections of a tag -- their includes and slots with their own sections, each time they are used: a
 * file used again (another slot, a screen built again) is loaded again; only one that comes back on its own
 * inclusion path is not (a file including itself).
 *
 * @param fromPath  the file the tag comes from
 * @param ancestors the files on its inclusion path
 */
async function load(fromPath, fromTag, ancestors = new Set()) {
    fromPath = fromPath || window.location.pathname;   // tags added out of any section's file: from the page
    const inclusionPath = new Set(ancestors).add(fromPath);
    const allPromises = [];
    for (const [index, sectionTag] of domUtil.findSection(fromTag).entries()) {
        const sectionPromises = [];
        let languagePath = undefined;
        // include
        if (domUtil.isInclude(sectionTag)) {
            const includePromise = new Promise((resolve, reject) => {
                const includePath = formatPathFrom(sectionTag.getAttribute('yw-include'), fromPath);
                if (includePath) {
                    httpClient.get({url: includePath, fetchAs: 'text'})
                              .then(includeResponse => {
                                    if (includeResponse.status === 200) {
                                        const result = {};
                                        result.path = includePath;
                                        result.body = includeResponse.body;
                                        result.mode = getIncludeMode(sectionTag);
                                        resolve(result);
                                    }
                                    else {
                                        reject(yojaError('load yojaWeb include failed', { 
                                                path: includePath,
                                                httpStatus: includeResponse.status,
                                                httpBody: includeResponse.body}));
                                    }
                               })
                               .catch(error => reject(yojaError('load yojaWeb include failed', {
                                                       path: includePath,
                                                       cause: error})));
                }
                else {
                    resolve();
                }
            })    
            sectionPromises.push(includePromise);
            sectionPromises.push(Promise.resolve());
            sectionPromises.push(Promise.resolve());
            sectionPromises.push(Promise.resolve());
        }
        else {
            sectionPromises.push(Promise.resolve());
            // slot
            const slotPromise = new Promise((resolve, reject) => {
                const slotPath = formatPathFrom(sectionTag.getAttribute('yw-slot'), fromPath);
                if (slotPath) {
                    httpClient.get({url: slotPath, fetchAs: 'text'})
                              .then(slotResponse => {
                                    if (slotResponse.status === 200) {
                                        const result = {};
                                        result.path = slotPath;
                                        result.body = slotResponse.body;
                                        resolve(result);
                                    }
                                    else {
                                        reject(yojaError('load yojaWeb slot failed', { 
                                                path: slotPath,
                                                httpStatus: slotResponse.status,
                                                httpBody: slotResponse.body}));
                                    }
                               })
                               .catch(error => reject(yojaError('load yojaWeb slot failed', {
                                                       path: slotPath,
                                                       cause: error})));
                }
                else {
                    resolve();
                }
            })    
            sectionPromises.push(slotPromise);
            // css
            const cssPath = formatPathFrom(sectionTag.getAttribute('yw-css'), fromPath, {version: false});
            const cssPromise = cssPath 
                                 ? cssLoader.load(cssPath) 
                                 : Promise.resolve();
            sectionPromises.push(cssPromise);
            // controler
            const controlerPromise = new Promise((resolve, reject) => {
                 const controlerPath = formatPathFrom(sectionTag.getAttribute('yw-controler'), fromPath);
                 if (controlerPath) {
                    import(controlerPath).then(module => {
                        const controler = module.default;
                        
                        const result = {};
                        result.path = controlerPath;
                        result.service = controler;
                        resolve(result);
                    })
                    .catch(error => reject(yojaError('load yojaWeb controler failed', {
                                            path: controlerPath,
                                            cause: error})));
                 }
                 else {
                    resolve();
                 }
            })  
            sectionPromises.push(controlerPromise);
            // language
            languagePath = formatPathFrom(sectionTag.getAttribute('yw-language'), fromPath);
        }
        // all futures
        const includeTagPromises = new Promise((resolve, reject) => {
           Promise.all(sectionPromises)
                  .then(promises => {
                     const section = {};
                     section.index = index;
                     section.fromPath = fromPath;
                     section.languagePath = languagePath;
                     section.tagName = sectionTag.localName;
                     section.include = promises[0];
                     section.slot = promises[1];
                     section.css = promises[2];
                     section.controler = promises[3];
                     
                     if (section.slot) {
                         fileGraph.addLink(fromPath, section.slot.path);
                     }
                     if (section.include) {
                         fileGraph.addLink(fromPath, section.include.path);
                     }
                     
                     if (section.slot
                            && !inclusionPath.has(section.slot.path)) {
                         const bodyTag = domUtil.parseHtml(section.slot.body).body;
                         load(section.slot.path, bodyTag, inclusionPath)
                           .then(children => {
                             section.children = children;
                             resolve(section);
                           })
                           .catch(error => reject(yojaError('load yojaWeb children slot failed', {
                                                   cause: error})));
                     }
                     else if (section.include
                                 && !inclusionPath.has(section.include.path)) {
                        const bodyTag = domUtil.parseHtml(section.include.body).body;
                        load(section.include.path, bodyTag, inclusionPath)
                          .then(children => {
                            section.children = children;
                            resolve(section);
                          })
                          .catch(error => reject(yojaError('load yojaWeb children include failed', {
                                                  cause: error})));
                     }
                     else {
                         resolve(section);
                     }
                  })
                  .catch(error => reject(yojaError('load yojaWeb controler/css/slot failed', {
                                          cause: error})));
            })
        allPromises.push(includeTagPromises);
    }
    // all section promises
    return Promise.all(allPromises);
}    

/*

    FLAT SECTION

 */
function groupSectionsByPath(sections) {
    const result = {};
    for (const section of sections) {
        let childrenSections = result[section.fromPath];
        if (!childrenSections) {
            childrenSections = [];
            result[section.fromPath] = childrenSections;
        }
        childrenSections.push(section);
        if (section.children) {
            for (const [path, childSections] of Object.entries(groupSectionsByPath(section.children))) {
                if (!result[path]) {
                    result[path] = childSections;
                }
            }
        }
    }
    return result;
}

function cloneSection(section) {
    const result = {};
    for (const key of Object.keys(section)) {
        if (key === 'children') {
            continue;
        }
        if (section[key] !== undefined) {
            result[key] = section[key];
        }
    }
    let type = 'section';
    if (section.include) {
        type = 'include';
    }
    else if (section.slot) {
        type = 'slot';
    }
    result.type = type;
    return result;
}

function addGraphLinkSection(graph, section) {
    const cleanedSection = cloneSection(section);
    const parentNode = {id: cleanedSection.fromPath};
    const childNode = {id: cleanedSection.fromPath + '||' + cleanedSection.index, 
                       section: cleanedSection};
    graph.addLink(parentNode, childNode);
    if (cleanedSection?.slot?.path) {
        graph.addLink(childNode, {id: cleanedSection.slot.path});
    }
    else if (cleanedSection?.include?.path) {
        graph.addLink(childNode, {id: cleanedSection.include.path});
    }
}

function flatSections(sections) {
    const result = [];
    if (sections && sections.length) {
        const rootPath = sections[0].fromPath;
        const sectionsByPath = groupSectionsByPath(sections);

        const sectionGraph = Graph.newInstance();
        const rootSections = sectionsByPath[rootPath];
        if (rootSections) {
            for (const section of rootSections) {
                addGraphLinkSection(sectionGraph, section);
            }
        }
        fileGraph.walkChildren(rootPath, h => {
            const sections = sectionsByPath[h.node]
            if (sections) {
                for (const section of sections) {
                    addGraphLinkSection(sectionGraph, section);
                }
            }
        })
        const recursiveService = fileGraph.newRecursiveService(rootPath);
        sectionGraph.walkChildren(rootPath, h => {
            if (h?.node?.section) {
                const section = h.node.section;
                let deep = 0;
                for (const parent of h.parentPath) {
                    if (parent.section) {
                        deep++;
                    }
                }
                let id = h.node.id;
                if (id.includes('||')) {
                    id = id.split('||')[0];
                }
                section.recursive = recursiveService.isRecursiveNode(id);
                section.deep = deep;
                result.push(section);
            }
        })
        for (const section of result) {
            logSection(section);
        }
    }
    return result;
}
    
/*

    CSS

 */
const cssSheetEmpty = new CSSStyleSheet();

/** A sheet applies when every media along its imports' path does (cssLoader: medias). */
function handleCssMedia(mediaQueryLists, shadow, index, cssSheet) {
    shadow.adoptedStyleSheets[index] = mediaQueryLists.every(mediaQueryList => mediaQueryList.matches)
                                           ? cssSheet
                                           : cssSheetEmpty;
}

/** @return what removes the media listeners it adds */
function addCssMediaListener(cssSheetEntities, shadow) {
    const removals = [];
    for (let i = 0; i < cssSheetEntities.length; i++) {
        const cssSheet = cssSheetEntities[i].sheet;
        const cssMedias = cssSheetEntities[i].medias
                          ?? (cssSheetEntities[i].media ? [cssSheetEntities[i].media] : []);
        if (cssMedias.length) {
            shadow.adoptedStyleSheets.push(cssSheetEmpty);
            const mediaQueryLists = cssMedias.map(cssMedia => window.matchMedia(cssMedia));
            const listener = () => handleCssMedia(mediaQueryLists, shadow, i, cssSheet);
            for (const mediaQueryList of mediaQueryLists) {
                mediaQueryList.addEventListener('change', listener);
                removals.push(() => mediaQueryList.removeEventListener('change', listener));
            }
            handleCssMedia(mediaQueryLists, shadow, i, cssSheet);
        }
        else {
            shadow.adoptedStyleSheets.push(cssSheet);
        }
    }
    return () => removals.forEach(removal => removal());
}

function manageCss(section, sectionTag, shadow) {
    // the section is registered once built: its media listeners kept until then, then handed to it
    let removal = null;
    if (section?.css?.sheetEntities) {
        shadow.adoptedStyleSheets = [];
        removal = addCssMediaListener(section.css.sheetEntities, shadow);
    }
    else if (domUtil.isEmptyAttribute(sectionTag, 'yw-css')) {
        const parentSection = window.yojaWeb
                                    .sectionService
                                    .parent(sectionTag);
        if (parentSection?.css?.sheetEntities) {
            // its parent's sheets, with listeners of its own: a media changing updates this shadow too
            shadow.adoptedStyleSheets = [];
            removal = addCssMediaListener(parentSection.css.sheetEntities, shadow);
        }
        else if (parentSection?.shadowTag?.adoptedStyleSheets) {
            shadow.adoptedStyleSheets = parentSection.shadowTag.adoptedStyleSheets;
        }
    }
    if (removal) {
        const id = sectionTag.getAttribute(directiveAttributeId);
        builtSections.set(id, {tag: sectionTag, cleanups: [removal], ready: false});
    }
}

/*

    APPEND

 */
let idSequence = 0;

/**
 * Builds the sections loaded: each one's content moved -- not copied -- into its shadow (a slot: its file's,
 * parsed once), an include's in its place. Moved, the tags are the very ones given: what the application set on
 * them (a listener, a property, a field's value) is kept, and each section's tag is the one its id was given to --
 * no search for it.
 */
function append(tag, loadedSections) {
    forgetGoneSections();
    const sections = flatSections(loadedSections);

    const result = [];
    // id -> its section tag: the tags are moved, never recreated, so the ones marked are the ones built
    const sectionTagsById = new Map();
    const mark = root => {
        const marked = [];
        for (const sectionTag of domUtil.findSection(root)) {
            const id = ++idSequence;
            sectionTag.setAttribute(directiveAttributeId, id);
            sectionTagsById.set(id, sectionTag);
            marked.push(id);
        }
        return marked;
    };
    let ids = mark(tag);
    for (const section of sections) {
        const id = ids.shift();
        section.id = id;
        sectionDescriptions[id] = section;
        const sectionTag = sectionTagsById.get(id);

        const isInclude = section.include ? true : false;
        const isSlot = section.slot ? true : false;

        if (isSlot) {
            const bodyTag = domUtil.parseHtml(section.slot.body).body;
            ids = [...mark(bodyTag), ...ids];   // its file's sections next
            const shadow = sectionTag.attachShadow({ mode: 'open' });
            manageCss(section, sectionTag, shadow);
            shadow.append(...bodyTag.childNodes);
        }
        else if (isInclude) {
            const bodyTag = domUtil.parseHtml(section.include.body).body;
            ids = [...mark(bodyTag), ...ids];
            builtSections.set(String(id), {tag: sectionTag, cleanups: [], ready: true});   // no ready actions
            if (section.include.mode === 'inner') {
                sectionTag.replaceChildren(...bodyTag.childNodes);
            }
            else {
                sectionTag.after(...bodyTag.childNodes);
                sectionTag.remove();
            }
        }
        else {
            const shadow = sectionTag.attachShadow({ mode: 'open' });
            manageCss(section, sectionTag, shadow);
            shadow.append(...sectionTag.childNodes);
        }
        if (!isInclude) {
            new Section(sectionTag, section);
            result.push(section);
        }
    }
    return result;
}

/*

    READY

 */
/** Runs an action of the application: it failing, said -- the others run all the same. */
function runAction(action, name) {
    try {
        action();
    }
    catch (error) {
        console.error('YojaWeb ' + name + ' action failed', error);
        errorActions.run(error);
    }
}

function sectionReady(sections) {
    const controlerActions = [];
    const pageActions = [];
    for (const section of sections) {
        controlerActions.push(...(controlerReadyActions[section.id] || []));
        pageActions.push(...(pageReadyActions[section.id] || []));
    }
    console.debug('YojaWeb controlerReady')
    for (const action of controlerActions) {
        runAction(action, 'controlerReady');
    }
    console.debug('YojaWeb pageReady');
    for (const action of pageActions) {
        runAction(action, 'pageReady');
    }
    for (const section of sections) {
        const built = builtSections.get(String(section.id));
        if (built) {
            built.ready = true;   // from now on, forgotten once it leaves the page
        }
    }
}

function tagReady(sections, tags) {
    console.debug('YojaWeb tagReady');
    for (const action of tagReadyActions.actions()) {
        runAction(() => action(tags), 'tagReady');
    }
}
   
function documentReady(sections) {
    console.debug('YojaWeb documentReady');
    for (const action of documentReadyActions.actions()) {
        runAction(action, 'documentReady');
    }
    yojaWebReady = true;
}
                             
async function applyYojaWebOnTags(path, tags) {
    const futures = [];
    for (const tag of tags) {
        futures.push(applyYojaWebOnTag(path, tag));
    }
    return Promise.all(futures)
                  .then(futureResults => {
                      const sections = [] 
                      for (const futureResult of futureResults) {
                          sections.push(...futureResult);
                      }
                      console.debug('applyYojaWebOnTags');
                      sectionReady(sections);
                      tagReady(sections, tags);
                  });
}

async function applyYojaWebOnTag(path, tag) {
    return load(path, tag).then(sectionDescriptions => append(tag, sectionDescriptions));
}

async function applyYojaWebOnDocument() {
    const path = window.location.pathname;
    document.body.style.display = 'none';
    return load(path, document)
             .then(sectionDescriptions => append(document, sectionDescriptions))
             .then(flatSectionDescriptions => {
                console.debug('applyYojaWebOnDocument');
                sectionReady(flatSectionDescriptions);
                // the language set before the ready actions -- they may change it --, the page shown once translated:
                // not its keys first
                const translated = languageService.start()
                                                  .catch(error => console.error('YojaWeb: the page not translated', error));
                documentReady(flatSectionDescriptions);
                return translated;
              })
             .then(() => {
                document.body.style.display = '';
              });
}
    
/** The page failed: shown all the same -- what could be built, not a blank page --, the error said. */
function failed(error) {
    document.body.style.display = '';
    console.error('YojaWeb failed: ' + error?.message, error);
    errorActions.run(error);
}

function ready() {
    document.removeEventListener('DOMContentLoaded', ready);
    window.removeEventListener('load', ready);
    try {
        applyYojaWebOnDocument().catch(failed);
    }
    catch (error) {
        failed(error);
    }
}

if (document.readyState === 'complete'
       || (document.readyState !== 'loading' 
              && !document.documentElement.doScroll)) {
    window.setTimeout(ready);
} 
else {
    document.addEventListener('DOMContentLoaded', ready);
    window.addEventListener('load', ready);
}

