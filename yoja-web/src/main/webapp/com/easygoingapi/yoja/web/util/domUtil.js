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

const domParser = new DOMParser();
const xmlSerializer = new XMLSerializer();

/*
 * PARSE
 */
function parse(value, type) {
    return domParser.parseFromString(value, type);
}

/**
 * @return the document -- an xml not well formed is not refused: the document then holds a {@code <parsererror>}
 *         (DOMParser's way), to look for
 */
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
 * PARENT
 */
function getParent(element, selector) {
    let parent = element.parentElement;
    while (parent) {
        if (!selector || parent.matches(selector)) {
            return parent;
        }
        parent = parent.parentElement;
    }
    return null;
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

/** @return the shadow host of the tag's tree, or null when it is in the document's */
function hostOf(tag) {
    const root = tag.getRootNode();
    return root instanceof ShadowRoot ? root.host : null;
}

function parentsRecursively(tag, selector, result) {
    result.push(...getParents(tag, selector));
    const host = hostOf(tag);
    if (!host) {
        return;
    }
    if (!selector || host.matches(selector)) {
        result.push(host);
    }
    parentsRecursively(host, selector, result);
}

/** @return its parents matching the selector -- every one when none --, the nearest first, through the shadow hosts */
export function parents(tag, selector) {
    const result = [];
    parentsRecursively(tag, selector, result);
    return result;
}

/** @return its nearest parent matching the selector -- its parent when none --, through the shadow hosts */
export function parent(tag, selector) {
    const found = getParent(tag, selector);
    if (found) {
        return found;
    }
    const host = hostOf(tag);
    if (!host) {
        return null;
    }
    if (!selector || host.matches(selector)) {
        return host;
    }
    return parent(host, selector);
}

/** @return the tag itself, or its nearest parent, matching the selector -- through the shadow hosts */
export function closest(tag, selector) {
    const found = tag.closest(selector);
    if (found) {
        return found;
    }
    const host = hostOf(tag);
    return host ? closest(host, selector) : null;
}

/*
 * SECTION
 */
const sectionAttributeNames = ['yw-slot',
                               'yw-controler',
                               'yw-language',
                               'yw-css'];

const SECTION_TAG_SELECTOR = sectionAttributeNames.map(name => '[' + name + ']').join(',');

const YOJAWEB_TAG_TAG_SELECTOR = SECTION_TAG_SELECTOR + ',[yw-include]';

function toSection(tag) {
    return tag?.ywSection;
}

function toSections(tags) {
    const result = [];
    for (const tag of tags || []) {
        const section = toSection(tag);
        if (section) {
            result.push(section);
        }
    }
    return result;
}

/** @return the sections right under the tag: in its tree, and its shadow one -- not the sections of those */
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

/** @return {@code false} once the consumer asked to stop: the whole walk stops */
function walkSectionRecursively(section, consumer, deep) {
    let index = 0;
    for (const sectionChild of childrenSectionOfTag(section.tag)) {
        if (consumer(sectionChild, {deep: deep, index: index}) === false
              || walkSectionRecursively(sectionChild, consumer, deep + 1) === false) {
            return false;
        }
        index++;
    }
    return true;
}

export function isSection(tag) {
    return Boolean(tag && tag.matches && tag.matches(SECTION_TAG_SELECTOR));
}

export function isInclude(tag) {
    return Boolean(tag && tag.matches && tag.matches('[yw-include]'));
}

/** @return the tag, when it is a section or an include, then the sections and includes of its tree */
export function findSection(tag) {
    const result = [];
    if (tag) {
        if (tag.matches && tag.matches(YOJAWEB_TAG_TAG_SELECTOR)) {
            result.push(tag);
        }
        if (tag.querySelectorAll) {
            result.push(...tag.querySelectorAll(YOJAWEB_TAG_TAG_SELECTOR));
        }
    }
    return result;
}

export function childrenSection(tag) {
    return childrenSectionOfTag(tag);
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

/**
 * Walks the sections under the tag, each before its own: consumer(section, {deep, index}). It returning
 * {@code false} stops the whole walk.
 */
export function walkSection(tag, consumer) {
    let index = 0;
    for (const section of childrenSectionOfTag(tag)) {
        if (consumer(section, {deep: 1, index: index}) === false
              || walkSectionRecursively(section, consumer, 2) === false) {
            return;
        }
        index++;
    }
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

/** @return its children matching the selector -- every one when none --: its own, then its shadow root's */
export function allChildren(tag, selector) {
    const children = getChildren(tag, selector);
    if (tag.shadowRoot) {
        children.push(...getChildren(tag.shadowRoot, selector));
    }
    return children;
}

/**
 * Walks the tags under one, each before its own, through the shadow roots.
 *
 * @param skip a tag it is given is visited, its own are not (null: every one)
 * @return {@code false} once the consumer asked to stop
 */
function walkUntil(tag, consumer, selector, skip) {
    for (const child of allChildren(tag, selector)) {
        if (consumer(child) === false) {
            return false;
        }
        if (!(skip && skip(child)) && walkUntil(child, consumer, selector, skip) === false) {
            return false;
        }
    }
    return true;
}

/**
 * Walks the tags under one, each before its own, through the shadow roots -- only those matching the selector,
 * when given, and under them. The consumer returning {@code false} stops the whole walk.
 */
export function walk(tag, consumer, selector) {
    walkUntil(tag, consumer, selector, null);
}

export function getTagPath(tag) {
    return getParents(tag).map(parent => parent.localName).reverse();
}

/*
 * FIND
 */

/**
 * Looks for the tags matching the selector under one, through the shadow roots: at each tag, its children first --
 * the nearest found first: not the document's order -- then under each of them.
 *
 * @param options {first: the first one found only, deep: into the sections too (else neither a section nor its
 *                tags are found)}
 * @return {@code true} once the first one is found (options.first)
 */
function findRecursively(selector, parent, result, options) {
    const children = allChildren(parent).filter(child => options.deep || !isSection(child));
    for (const child of children) {
        if (child.matches(selector)) {
            result.push(child);
            if (options.first) {
                return true;
            }
        }
    }
    for (const child of children) {
        if (findRecursively(selector, child, result, options) && options.first) {
            return true;
        }
    }
    return false;
}

/** @return the first tag matching the selector under one, not in its sections (see findRecursively's order) */
export function firstTag(from, selector) {
    const tags = [];
    findRecursively(selector, from, tags, {first: true, deep: false});
    return tags.length ? tags[0] : null;
}

/** @return the tags matching the selector under one, not in its sections */
export function findTags(from, selector) {
    const tags = [];
    findRecursively(selector, from, tags, {first: false, deep: false});
    return tags;
}

/**
 * Walks the tags under one, each before its own, not into its sections: a section is neither given nor entered.
 * The consumer returning {@code false} stops the whole walk.
 */
export function walkTag(from, consumer) {
    walkUntil(from, tag => isSection(tag) ? undefined : consumer(tag), undefined, isSection);
}

/**
 * Splits a selector at its top level -- outside [...], (...) and quotes --: on ',' into its selectors, each of them
 * into its compounds and its '>' combinators (a space: a descendant, through the shadow roots).
 *
 * @return [[compound or '>', ...], ...]
 * @throws Error for the combinators '+' and '~': not supported
 */
function splitSelector(selector) {
    const selectors = [];
    let tokens = [];
    let current = '';
    let depth = 0;
    let quote = null;
    const flush = () => {
        if (current.trim()) {
            tokens.push(current.trim());
        }
        current = '';
    };
    for (const character of selector) {
        if (quote) {
            current += character;
            if (character === quote) {
                quote = null;
            }
        }
        else if (character === '"' || character === "'") {
            quote = character;
            current += character;
        }
        else if (character === '[' || character === '(') {
            depth++;
            current += character;
        }
        else if (character === ']' || character === ')') {
            depth--;
            current += character;
        }
        else if (depth > 0) {
            current += character;
        }
        else if (character === ',') {
            flush();
            selectors.push(tokens);
            tokens = [];
        }
        else if (character === '>') {
            flush();
            tokens.push('>');
        }
        else if (character === '+' || character === '~') {
            throw new Error('the combinator "' + character + '" is not supported: ' + selector);
        }
        else if (/\s/.test(character)) {
            flush();
        }
        else {
            current += character;
        }
    }
    flush();
    selectors.push(tokens);
    return selectors.filter(compounds => compounds.length);
}

/**
 * Looks for the tags matching a selector under one, into the sections and through the shadow roots: each compound
 * of it looked for under the tags the one before it found (a space), or among their children ('>').
 *
 * @param first the first one only: of the first selector of a list (a, b) that finds one -- not the document's
 *              first
 */
function deepTag(from, selector, first) {
    const allResult = [];
    for (const compounds of splitSelector(selector)) {
        let fromTags = [from];
        for (let i = 0; i < compounds.length; i++) {
            const child = compounds[i] === '>';
            if (child) {
                i++;
            }
            const isLast = i === compounds.length - 1;
            const tags = [];
            for (const fromTag of fromTags) {
                if (child) {
                    tags.push(...allChildren(fromTag, compounds[i]));
                }
                else {
                    findRecursively(compounds[i], fromTag, tags, {first: isLast && first, deep: true});
                }
            }
            fromTags = tags;
        }
        if (first) {
            if (fromTags.length) {
                return fromTags[0];
            }
        }
        else {
            for (const tag of fromTags) {
                if (!allResult.includes(tag)) {
                    allResult.push(tag);
                }
            }
        }
    }
    return first ? null : allResult;
}

export function deepFirstTag(from, selector) {
    return deepTag(from, selector, true);
}

export function deepFindTags(from, selector) {
    return deepTag(from, selector, false);
}

/**
 * Walks the tags under one, each before its own, into its sections and through the shadow roots. The consumer
 * returning {@code false} stops the whole walk.
 */
export function deepWalkTag(from, consumer) {
    walkUntil(from, consumer, undefined, null);
}

/*
 * attribute
 */
export function isEmptyAttribute(tag, attributeName) {
    return Boolean(tag && attributeName && tag.hasAttribute(attributeName) && !tag.getAttribute(attributeName));
}
