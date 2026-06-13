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

const domParser = new DOMParser();
const xmlSerializer = new XMLSerializer();

/*
 * PARSE
 */
function parse(value, type) {
    return domParser.parseFromString(value, type);
}

export function parseXml(xml) {
    return parse(xml, 'application/xml');
}

export function serializeXml(xml) {
    return xmlSerializer.serializeToString(xml);
}

export function parseHtml(html) {
    return parse(html, 'text/html');
}

/*
 * JQUERY
 */
export function isJQueryTag(tag) {
    return 'jquery' in Object(tag);
}

export function toArrayDomTag(tag) {
    let result = [];
    if (isJQueryTag(tag)) {
        for (let i = 0; i < tag.length; i++) {
            result.push(tag.get(i));
        }
    }
    else if (Array.isArray(tag)) {
        result = tag;
    }
    else {
        result.push(tag);
    }
    return result;
}

export function toDomTag(tag) {
    let result = tag;
    if (isJQueryTag(tag)) {
        result = null
        if (tag.length == 1) {
            result = tag.get(0);
        }
        else if (tag.length > 1) {
            result = [];
            for (let i = 0; i < tag.length; i++) {
                result.push(tag.get(i));
            }
        }
    }
    return result;
}

/*
 * PARENT
 */
function getParent(element, selector) {
    let result = null;
    let parent = element.parentElement;
    while (parent) {
        if (!selector || parent.matches(selector)) {
            result = parent;
            break;
        }
        parent = parent.parentElement;
    }
    return result;
}

function getParents(element, selector) {
    const parents = [];
    let parent = element.parentElement;
    while (parent) {
        if (!selector || parent.matches(selector)) {
            parents.push(parent);
        }
        parent = parent.parentElement;
    }
    return parents;
}

function parentsRecursively(tag, selector, result) {
    let found = getParents(tag, selector);
    if (found.length) {
        result.push(...found);
    }
    const root = tag.getRootNode();
    if (root === document 
            || !(root instanceof ShadowRoot)) {
        return;
    }
    if (!selector 
           || root.host.matches(selector)) {
        result.push(root.host);
    }
    return parentsRecursively(root.host, selector, result);
}

export function parents(tag, selector) {
    const result = [];
    parentsRecursively(tag, selector, result);
    return result;
}

export function parent(tag, selector) {
    const found = getParent(tag, selector);
    if (found) {
        return found;
    }
    const root = tag.getRootNode();
    if (root === document 
           || !(root instanceof ShadowRoot)) {
        return null;
    }
    if (!selector 
           || root.host.matches(selector)) {
        return root.host;
    }
    return parent(root.host, selector);
}

export function closest(tag, selector) {
    const found = tag.closest(selector);
    if (found) {
        return found;
    }
    const root = tag.getRootNode();
    if (root === document 
           || !(root instanceof ShadowRoot)) {
        return null;
    }
    return closest(root.host, selector);
}

/*
 * SECTION
 */
const sectionAttributeNames = ['yw-slot' , 
                               'yw-controler', 
                               'yw-language',
                               'yw-css'];
                               
function getSectionTagSelectors() {
    const result = [];
    for (const sectionAttributeName of sectionAttributeNames) {
        result.push('[' + sectionAttributeName + ']');
    }
    return result;
}          

function getYojaWebTagSelectors() {
    const result = [...getSectionTagSelectors()];
    result.push('[yw-include]');
    return result;
}                         
                  
const SECTION_TAG_SELECTOR = getSectionTagSelectors().join(',');

const YOJAWEB_TAG_TAG_SELECTOR = getYojaWebTagSelectors().join(',');
                          
function toSection(tag) {
    let result;
    if (tag) {
        result = tag?.ywSection;
    }
    return result;
}

function toSections(tags) {
    let result = [];
    if (tags) {
        for (const tag of tags) {
            const section = toSection(tag);
            if (section) {
                result.push(section);
            }
        }
    }
    return result;
}

function childrenSectionOfTag(tag) {
    const result = [];
    childrenSectionRecursively(result, tag);
    return result;
}

function childrenSectionRecursively(result, parent) {
    for (const child of allChildren(parent)) {
        if (isSection(child)) {
            const section = toSection(child);
            if (section) {
                result.push(section);
            }
        }
        else {
            childrenSectionRecursively(result, child);
        }
    }
}

function walkSectionRecursively(section, consumer, deep) {
    let index = 0;
    for (const sectionChild of childrenSectionOfTag(section.tag)) {
        const keepOn = consumer(sectionChild, {deep: deep, index: index});
        index++;
        if (keepOn !== false) {
            walkSectionRecursively(sectionChild, consumer, deep + 1);
        }
        else {
            break;
        }
    }
}

function walkSectionFrom(section, consumer, info) {
    const keepOn = consumer(section, info);
    if (keepOn !== false) {
        walkSectionRecursively(section, consumer, 2);
    }
    return keepOn;
}

export function isSection(tag) {
    let result = false;
    if (tag && tag.matches) {
         result = tag.matches(SECTION_TAG_SELECTOR);
    }
    return result;
}

export function isInclude(tag) {
    let result = false;
    if (tag && tag.matches) {
         result = tag.matches('[yw-include]');
    }
    return result;
}

export function findSection(tag) {
    let result = [];
    if (tag) {
        if (tag.matches 
               && tag.matches(YOJAWEB_TAG_TAG_SELECTOR)) {
            result.push(tag);
        }
        if (tag.querySelectorAll) {
            const tags = tag.querySelectorAll(YOJAWEB_TAG_TAG_SELECTOR);
            if (tags && tags.length) {
                result.push(...tags);
            }
        }
    }
    return result;
}

export function childrenSection(tag) {
    return  childrenSectionOfTag(tag);
}

export function closestSection(tag) {
    return toSection(closest(tag, SECTION_TAG_SELECTOR));
}

export function parentSection(tag) {
    return toSection(parent(tag, SECTION_TAG_SELECTOR));
}

export function parentSections(tag) {
    return toSections(parents(tag, SECTION_TAG_SELECTOR));
}

export function walkSection(tag, consumer) {
//    if (isSection(tag)) {
//        walkSectionFrom(toSection(tag), consumer)
//    }
//    else {
        let index = 0;
        for (const section of childrenSectionOfTag(tag)) {
              const keepOn = walkSectionFrom(section, consumer, {deep: 1, index: index});
              index++;
              if (keepOn === false) {
                  break;
              }
          }
//    }
}

/*
 * CHILDREN
 */
function getChildren(element, selector) {
    const children = [];
    for (const child of element.children) {
        if (!selector || child.matches(selector)) {
            children.push(child);
        }
    }
    return children;
}

export function allChildren(tag, selector) {
    const children = [];
    for (const child of getChildren(tag, selector)) {
        children.push(child);
    }
    if (tag.shadowRoot) {
        for (const child of getChildren(tag.shadowRoot, selector)) {
            children.push(child);
        }
    }
    return children;
}

export function walk(tag, consumer, selector) {
    for (const child of allChildren(tag, selector)) {
        if (consumer(child) !== false) {
            walk(child, consumer, selector);
        }
        else {
            break;
        }
    }
}

export function getTagPath(tag) {
    let result = [];
    for (const parent of getParents(tag)) {
        result.push(parent.localName);
    }
    result.reverse();
    return result;
}

/*
 * FIND
 */
function findRecursively(selector, parent, result, deep, options) {
    const tags = allChildren(parent, selector);
    let keepOn = true;
    if (tags) {
        for (const tag of tags) {
            if (options.deep || !isSection(tag)) {
                tag.deep = deep;
                result.push(tag);
                if (options.first) {
                    keepOn = false;
                    break;
                }
            }
        }
    }
    if (keepOn) {
        for (const child of allChildren(parent)) {
            if (options.deep || !isSection(child)) {
                findRecursively(selector, child, result, deep + 1, options);
            }
        }
    }
}

export function firstTag(from, selector) {
    const tags = []
    findRecursively(selector, from, tags, 1, {first: true, deep: false});
    return tags.length ? tags[0] : null;
}

export function findTags(from, selector) {
    const tags = [];
    findRecursively(selector, from, tags, 1, {first: false, deep: false});
    return tags;
}

export function walkTag(from, consumer) {
    for (const child of allChildren(from)) {
        if (!isSection(child) 
                && consumer(child) !== false) {
            walk(child, tag => {
                if (isSection(tag)) {
                    return false;
                }
                else {
                    return consumer(tag);
                }
            })
        }
    }
}

function deepTag(from, selector, first) {
    const allResult = [];
    const commaSelectors = selector.split(',');
    
    for (const commaSelector of commaSelectors) {
        const result = [];
        const splitSelector = commaSelector.replaceAll('>', ' > ')
                                           .split(/(\s+)/)
                                           .filter( e => e.trim().length > 0);

        let fromTags = null;
        for (let i = 0; i < splitSelector.length; i++) {
            const tags = [];
            if (fromTags === null) {
                if ('>' === splitSelector[i]) {
                    i++;
                    const children = allChildren(from, splitSelector[i]);
                    tags.push(...children);
                }
                else {
                    const isLast = splitSelector.length === (i + 1);
                    findRecursively(splitSelector[i], 
                                    from, 
                                    tags, 
                                    1, 
                                    {first: isLast ? first : false, deep: true});
                }
            }
            else {
                for (const fromTag of fromTags) {
                    if ('>' === splitSelector[i]) {
                        i++;
                        const children = allChildren(fromTag, splitSelector[i]);
                        tags.push(...children);
                    }
                    else {
                        const isLast = splitSelector.length === (i + 1);
                        findRecursively(splitSelector[i], 
                                        fromTag, 
                                        tags, 
                                        1, 
                                        {first: isLast ? first : false, deep: true});
                    }
                }
            }
            if (splitSelector.length === (i + 1)) {
                result.push(...tags);
            }
            else {
                fromTags = tags;
            }
        }
        if (first) {
            if (result.length 
                  && !allResult.includes(result[0])) {
                allResult.push(result[0]);
                break;
            }
        }
        else {
            for (const value of result) {
                if (!allResult.includes(value)) {
                    allResult.push(value);
                }
            }
        }
    }
    if (first) {
        return allResult.length ? allResult[0] : null;
    }
    else {
        return allResult;
    } 
}

export function deepFirstTag(from, selector) {
    return deepTag(from, selector, true);
}
    
export function deepFindTags(from, selector) {
    return deepTag(from, selector, false);
}
    
export function deepWalkTag(from, consumer) {
    for (const child of allChildren(from)) {
        if (consumer(child) !== false) {
            walk(child, consumer);
        }
    }
}

/*
 * attribute
 */
export function isEmptyAttribute(tag, attributeName) {
    let result = false;
    if (tag && attributeName) {
        if (tag.hasAttribute(attributeName)) {
            if (!tag.getAttribute(attributeName)) {
                result = true;
            }
        }
    }
    return result;
}
