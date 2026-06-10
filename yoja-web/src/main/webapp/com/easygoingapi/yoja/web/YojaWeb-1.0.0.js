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

const apiVersion = '1.0.0';

const sectionDescriptions = [];

// error
const errorActions = [];
let yojaWebReady = false;
window.yojaWeb = {};
window.yojaWeb.onError = action => {
    errorActions.push(action);
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

// options: boolean force, boolean version
function formatPath(path, options) {
    const addApiVersion = options ? options.apiVersion : false;
    const addVersion = options ? options.version : true;
    const addDate = options ? options.force : false;
    let result = path;
    if (result) {
        if (URL.canParse(result)) {
           const url = URL.parse(result);
           result = url.pathname;
        }
        const parameters = []
        if (addVersion && version) {
            parameters.push('version=' + version);
        }
        if (addApiVersion) {
            parameters.push('apiVersion=' + apiVersion);
        }
        if (addDate) {
            parameters.push('date=' + Date.now());
        }
        if (parameters.length) {
            result = result + '?' + parameters.join('&');
        }
    }
    return result;
}

function formatPathFrom(path, fromPath, options) {
    let result;
    if (path) {
        if (URL.canParse(path)) {
           const url = URL.parse(path);
           path = url.pathname;
        }
        if (!fromPath) {
            fromPath = window.location.pathname;
        }
        else if (URL.canParse(fromPath)) {
            const url = URL.parse(fromPath);
            fromPath = url.pathname;
        }
        const base = fromPath.substring(0, fromPath.lastIndexOf('/') + 1);
        if (base) {
            if (path.startsWith('./')) {
                result = base + path.substring(2);
            }
            else if (path.startsWith('../')) {
                result = base + path;
            }
            else {
                result = path;
            }
        }
        else {
            result = path;
        }
        if (result) {
            result = formatPath(result, options);
        }
    }
    return result;
}

// config
let config = {};
try {
    const configModule = await import(formatPath('/YojaWeb.conf.js', {force: true, version: false}))
                                  .catch(error => console.warn('yojaWew no config file: /YojaWeb.conf.js', {cause: error}));
    if (configModule.default) {
       config = configModule.default;
    }
}
catch(error) {
    config = {};
}

console.info('yojaWeb config: ', config);
if (config.version) {
    version = config.version;
}

function test(predicate, element) {
    let result = false;
    if (element) {
        if (predicate instanceof Function) { 
            result = predicate(element);
        }
        else {
            result = true;
        }
    }
    return result;
}

// dom 
const domUtil = await import(formatPath('./util/domUtil.js'));
const jsUtil = await import(formatPath('./util/javascriptUtil.js'));

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
            let message = '<css path="' + jsUtil.onlyUrlPath(h.node.path) + '"';
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
                    console.info(' '.repeat((cssDeep + 1) * tab)
                                    + '<css path="' + jsUtil.onlyUrlPath(cssSheetEntity.path) + '"'
                                    + ' media="' + cssSheetEntity.media +'"'
                                    + ' active="' + window.matchMedia(cssSheetEntity.media).matches + '" />');
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
                  && section
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

const tagReadyActions = [];
const documentReadyActions = [];

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
            this.#controler = new controlerClass(this);
            
            this.#controler.className = function() {
                return this.constructor.name;
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
        return domUtil.deepFindTags(tag, selector);;
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
    
    append(tag, path) {
        return this.#addTag('append', tag, path);
    }
    
    prepend(tag, path) {
        return this.#addTag('prepend', tag, path);
    }
    
    #addTag(mode, tag, path) {
        return new Promise((resolve, reject) => {
            httpClient.get({url: path, fetchAs: 'text'})
                      .then(response => {
                if (response.status === 200) {
                    const body = domUtil.parseHtml(response.body).body;
                    const children = Array.from(body.childNodes);
                    const shadow = tag.shadowRoot;
                    if (shadow) {
                        tag = shadow;
                    }
                    if (mode === 'append') {
                        for (const child of children) {
                            tag.appendChild(child);
                        }
                    }
                    else {
                        for (const child of children.reverse()) {
                            tag.prepend(child);
                        }
                    }
                    applyYojaWebOnTags(path, children)
                        .then(() => resolve(children))
                        .catch(error => reject({message: 'apply yoja on tags failed', 
                                                tags: children,
                                                error: error}));
                }
                else {
                    reject({message: 'http request for append tag failed', 
                            path: path,
                            httpStatus: response.status,
                            httpBody: response.body});
                }
            })
            .catch(error => reject({message: 'append tag failed', 
                                    error: error}));
        });
    }    
    
    onTagReady(action) {
        tagReadyActions.push(action);
    }
    
    onDocumentReady(action) {
        documentReadyActions.push(action);
    }
    
    isReady() {
        return yojaWebReady;
    }
    
    onError(action) {
        errorActions.push(action);
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
const loadedHtmlFiles = [];

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

async function load(fromPath, fromTag) {
    loadedHtmlFiles.push(fromPath);
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
                                        reject({message: 'load yojaWeb include failed', 
                                                path: includePath,
                                                httpStatus: includeResponse.status,
                                                httpBody: includeResponse.body});
                                    }
                               })
                               .catch(error => reject({message: 'load yojaWeb include failed',
                                                       path: includePath,
                                                       cause: error}));
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
                                        reject({message: 'load yojaWeb slot failed', 
                                                path: slotPath,
                                                httpStatus: slotResponse.status,
                                                httpBody: slotResponse.body});
                                    }
                               })
                               .catch(error => reject({message: 'load yojaWeb slot failed',
                                                       path: slotPath,
                                                       cause: error}));
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
                    .catch(error => reject({message: 'load yojaWeb controler failed',
                                            path: controlerPath,
                                            cause: error}));
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
                            && !loadedHtmlFiles.includes(section.slot.path)) {
                         const bodyTag = domUtil.parseHtml(section.slot.body).body;
                         load(section.slot.path, bodyTag)
                           .then(children => {
                             section.children = children;
                             resolve(section);
                           })
                           .catch(error => reject({message: 'load yojaWeb children slot failed',
                                                   cause: error}));
                     }
                     else if (section.include
                                 && !loadedHtmlFiles.includes(section.include.path)) {
                        const bodyTag = domUtil.parseHtml(section.include.body).body;
                        load(section.include.path, bodyTag)
                          .then(children => {
                            section.children = children;
                            resolve(section);
                          })
                          .catch(error => reject({message: 'load yojaWeb children include failed',
                                                  cause: error}));
                     }
                     else {
                         resolve(section);
                     }
                  })
                  .catch(error => reject({message: 'load yojaWeb controler/css/slot failed',
                                          cause: error}));
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
    else if (section.include) {
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

function handleCssMedia(mediaQueryList, shadow, index, cssSheet) {
    if (mediaQueryList.matches) {
         shadow.adoptedStyleSheets[index] = cssSheet;
    } 
    else {
        shadow.adoptedStyleSheets[index] = cssSheetEmpty;
    }
}

function addCssMediaListener(cssSheetEntities, shadow) {
    for (let i = 0; i < cssSheetEntities.length; i++) {
        const cssSheet = cssSheetEntities[i].sheet;
        const cssMedia = cssSheetEntities[i].media;
        if (cssMedia) {
            shadow.adoptedStyleSheets.push(cssSheetEmpty);
            const mediaQueryList = window.matchMedia(cssMedia);
            mediaQueryList.addListener(mql => handleCssMedia(mql, shadow, i, cssSheet));
            handleCssMedia(mediaQueryList, shadow, i, cssSheet);
        }
        else {
            shadow.adoptedStyleSheets.push(cssSheet);
        }
    }
}

function manageCss(section, sectionTag, shadow) {
    if (section?.css?.sheetEntities) {
        shadow.adoptedStyleSheets = [];
        addCssMediaListener(section.css.sheetEntities, shadow);
    }
    else if (domUtil.isEmptyAttribute(sectionTag, 'yw-css')) {
        const parentSection = window.yojaWeb
                                    .sectionService
                                    .parent(sectionTag);
        if (parentSection?.shadowTag?.adoptedStyleSheets) {
            shadow.adoptedStyleSheets = parentSection.shadowTag.adoptedStyleSheets;
            addCssMediaListener(parentSection.css, shadow);
        }
    }
}

/*

    APPEND

 */
let idSequence = 0;

function append(tag, loadedSections) {
    const sections = flatSections(loadedSections);
    
    const result = [];
    
    let ids = [];
    const sectionTags = domUtil.findSection(tag);
    for (const sectionTag of sectionTags) {
        sectionTag.setAttribute(directiveAttributeId, ++idSequence);
        ids.push(idSequence);
    }
    for (const section of sections) {
        const id = ids.shift();
        section.id = id;
        sectionDescriptions[id] = section;
        const sectionTag = findSectionTagFromId(tag, id);
        
        const isInclude = section.include ? true : false;
        const isSlot = section.slot ? true : false;

        if (isSlot) {
            const bodyTag = domUtil.parseHtml(section.slot.body).body;
            const sectionTags = domUtil.findSection(bodyTag);
            const _ids = [];
            for (const sectionTag of sectionTags) {
                sectionTag.setAttribute(directiveAttributeId, ++idSequence);
                _ids.push(idSequence);
            }
            ids = [..._ids, ...ids];
            const html = bodyTag.innerHTML;
            const shadow = sectionTag.attachShadow({ mode: 'open' });
            manageCss(section, sectionTag, shadow);
            shadow.innerHTML = html;
        }
        else if (isInclude) {
            const bodyTag = domUtil.parseHtml(section.include.body).body;
            const sectionTags = domUtil.findSection(bodyTag);
            const _ids = [];
            for (const sectionTag of sectionTags) {
                sectionTag.setAttribute(directiveAttributeId, ++idSequence);
                _ids.push(idSequence);
            }
            ids = [..._ids, ...ids];
            const html = bodyTag.innerHTML;
            if (section.include.mode === 'inner') {
                sectionTag.innerHTML = html;
            }
            else {
                const children = Array.from(bodyTag.childNodes);
                children.reverse();
                for (const child of children) {
                    sectionTag.after(child);
                }
                sectionTag.remove();
            }
        }
        else {
            const innerHTML = sectionTag.innerHTML;
            sectionTag.innerHTML = '';
            const shadow = sectionTag.attachShadow({ mode: 'open' });
            manageCss(section, sectionTag, shadow);
            shadow.innerHTML = innerHTML;
        }
        if (!isInclude) {
            new Section(sectionTag, section);
            result.push(section);
        }
    }
    return result;
}

/*

    UTIL

 */
function findSectionTagFromId(tagRoot, id) {
    let result = null;
    if (tagRoot) {
        if ((domUtil.isSection(tagRoot) || domUtil.isInclude(tagRoot))
                && tagRoot.getAttribute(directiveAttributeId) == id) {
            result = tagRoot;
        }
        if (result === null) {
            domUtil.walk(tagRoot, tag => {
                let keepOn = true;
                if ((domUtil.isSection(tag) || domUtil.isInclude(tag))
                    && tag.getAttribute(directiveAttributeId) == id) {
                    result = tag;
                    keepOn = false;
                }
                return keepOn;
            })
        }
    }
    return result;
}

/*

    READY

 */
function sectionReady(sections) {
    const controlerActions = [];
    const pageActions = [];
    for (const section of sections) {
        controlerActions.push(...controlerReadyActions[section.id]);
        pageActions.push(...pageReadyActions[section.id]);
    }
    console.debug('YojaWeb controlerReady')
    for (let i = 0; i < controlerActions.length; i++) {
        controlerActions[i]();
    }
    console.debug('YojaWeb pageReady');
    for (let i = 0; i < pageActions.length; i++) {
        pageActions[i]();
    }
}

function tagReady(sections, tags) {
    console.debug('YojaWeb tagReady');
    for (const action of tagReadyActions) {
        action(tags);
    }
}
   
function documentReady(sections) {
    console.debug('YojaWeb documentReady');
    for (const action of documentReadyActions) {
        action();
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
    return load(path, tag)
             .then(sectionDescriptions => append(tag, sectionDescriptions));
}

async function applyYojaWebOnDocument() {
    const path = window.location.pathname;
    return load(path, document)
             .then(sectionDescriptions => append(document, sectionDescriptions))
             .then(flatSectionDescriptions => {
                console.debug('applyYojaWebOnDocument');
                sectionReady(flatSectionDescriptions);
                documentReady(flatSectionDescriptions);
              });
}
    
function ready() {
    document.removeEventListener('DOMContentLoaded', ready);
    window.removeEventListener('load', ready);
    try {
        applyYojaWebOnDocument().catch(error => {
            console.error('YojaWeb failed: ' + error.message, {cause: error})
            for (const action of errorActions) {
                action(error);
            }
        })
    }
    catch(error) {
        console.error('YojaWeb failed: ' + error.message, {cause: error});
        for (const action of errorActions) {
            action(error);
        }
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

