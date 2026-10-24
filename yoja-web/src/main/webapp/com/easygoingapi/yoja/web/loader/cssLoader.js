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

const httpClient = await import(yojaWeb.path('../api/httpClient.js'));
const Graph = await import(yojaWeb.path('../util/Graph.js'));

/**
 * Loads a css file and the files it imports ({@code @import}): a constructed sheet takes no import, so each file is
 * a sheet of its own -- its relative url()s resolved from it --, the imports taken out of it and given in the
 * cascade's order: each import before its file, so that what comes later wins.
 *
 * An import is {@code @import "x.css"}, {@code 'x.css'} or {@code url(x.css)} -- quoted or not --, then optionally
 * {@code layer} / {@code layer(name)}, {@code supports(condition)} and a media query list. Its media holds along its
 * path: a file imported with "print" by a file imported with "screen" applies when both do (its {@code medias}).
 * An import whose supports() condition the browser does not meet is left out; a layer is not applied (a sheet is
 * shared by every import of its file): it is said so.
 *
 * A file is fetched and parsed once, however many load it -- at the same time too; an import that comes back to a
 * file on its own path is not followed again.
 */
class CssLoader {

    // the files and their imports: path -> paths, each link with its import's {media, supports, layer}
    #fileGraph = Graph.newInstance();
    // path -> a promise of the file: {sheet, imports}
    #files = new Map();
    // path -> the file, once read
    #loaded = new Map();

    /** @return the sheets of the files read, by path */
    get cssStyleSheets() {
        const result = {};
        for (const [path, file] of this.#loaded) {
            result[path] = file.sheet;
        }
        return result;
    }

    /**
     * @return the sheets to adopt, in the cascade's order -- each import before its file --: {path, media (its own
     *         import's), medias (every media along its path: it applies when all do)}
     */
    #importPaths(cssPath) {
        const result = [{path: cssPath, media: null, medias: []}];
        const recursiveService = this.#fileGraph.newRecursiveService(cssPath);
        this.#importPathsRecursively(recursiveService, cssPath, [], result);
        result.reverse();
        return result;
    }

    #importPathsRecursively(recursiveService, path, medias, result) {
        const links = this.#fileGraph.links(path);
        links.reverse();
        for (const link of links) {
            const data = link.data || {};
            if (recursiveService.isRecursive(path, link.node)) {
                continue;
            }
            if (data.supports && !CSS.supports(data.supports)) {
                continue;   // not met by this browser: left out, and what it imports with it
            }
            const linkMedias = data.media ? [...medias, data.media] : medias;
            result.push({path: link.node.path, media: data.media ?? null, medias: linkMedias});
            this.#importPathsRecursively(recursiveService, link.node.path, linkMedias, result);
        }
    }

    /** @return the absolute path of an import -- a relative one from the file's folder, '.' and '..' resolved */
    #toImportPath(cssPath, importPath) {
        const folder = cssPath.substring(0, cssPath.lastIndexOf('/') + 1);
        const url = new URL(importPath.startsWith('/') ? importPath : folder + importPath, window.location.origin);
        return url.pathname + url.search;
    }

    /**
     * Reads a css file: its imports taken out of it -- comments and strings respected.
     *
     * @return {importStatements: [{path, media, supports, layer}], css: the file without its imports}
     */
    searchCssImport(css) {
        const importStatements = [];
        if (!css || !css.trim()) {
            return {importStatements: importStatements, css: ''};
        }
        let result = '';
        let i = 0;
        while (i < css.length) {
            const character = css[i];
            if (character === '/' && css[i + 1] === '*') {   // a comment: dropped
                const end = css.indexOf('*/', i + 2);
                i = end < 0 ? css.length : end + 2;
                result += ' ';
            }
            else if (character === '"' || character === "'") {   // a string: kept as it is
                const end = endOfString(css, i);
                result += css.substring(i, end);
                i = end;
            }
            else if (css.startsWith('@import', i) && /[\s"'u]/.test(css[i + 7] || '')) {
                const end = endOfStatement(css, i + 7);
                // its comments and line breaks are spaces
                const statement = parseImport(withoutComments(css.substring(i + 7, end)).replace(/[\n\r]/g, ' '));
                if (statement) {
                    importStatements.push(statement);
                }
                i = end + 1;   // its ';' with it
                result += ' ';
            }
            else {
                result += character;
                i++;
            }
        }
        return {importStatements: importStatements, css: result};
    }

    /** @return a promise of the file and its imports: {rootPath, graph, sheetEntities: [{path, media, medias, sheet}]} */
    load(cssPath) {
        return this.#loadTree(cssPath, new Set()).then(() => ({
            rootPath: cssPath,
            graph: this.#fileGraph,
            sheetEntities: this.#importPaths(cssPath).map(entity => ({...entity, sheet: this.#loaded.get(entity.path).sheet}))
        }));
    }

    /**
     * Reads a file, then its imports -- not one already on its path (an import coming back to it).
     *
     * @param path      the file
     * @param ancestors the files on its path
     */
    #loadTree(path, ancestors) {
        return this.#loadFile(path).then(file => {
            const onPath = new Set(ancestors).add(path);
            const children = [];
            for (const statement of file.imports) {
                this.#fileGraph.addLink(path, {id: statement.path, path: statement.path},
                                        {media: statement.media, supports: statement.supports, layer: statement.layer});
                if (!onPath.has(statement.path)) {
                    children.push(this.#loadTree(statement.path, onPath));
                }
            }
            return Promise.all(children);
        });
    }

    /** @return a promise of the file read -- fetched and parsed once; a failure not kept: read again next time */
    #loadFile(cssPath) {
        let file = this.#files.get(cssPath);
        if (!file) {
            file = httpClient.get({url: yojaWeb.path(cssPath), fetchAs: 'text'})
                             .then(cssResponse => {
                                 if (cssResponse.status !== 200) {
                                     throw new Error('load yojaWeb css failed: [' + cssResponse.status + '] ' + cssPath,
                                                     {cause: {cssPath: cssPath,
                                                              httpStatus: cssResponse.status,
                                                              httpBody: cssResponse.body}});
                                 }
                                 const searchResult = this.searchCssImport(cssResponse.body);
                                 // its relative url()s resolved from the file, not from the page: written absolute
                                 // (Chrome does not apply a constructed sheet's baseURL to them)
                                 const fileUrl = new URL(cssPath, window.location.origin).href;
                                 const sheet = new CSSStyleSheet({baseURL: fileUrl});
                                 sheet.replaceSync(absoluteUrls(searchResult.css, fileUrl));
                                 const imports = searchResult.importStatements.map(statement => {
                                     if (statement.layer) {
                                         console.warn('css layer not applied: ' + statement.path + ' imported by ' + cssPath);
                                     }
                                     return {...statement, path: this.#toImportPath(cssPath, statement.path)};
                                 });
                                 const read = {sheet: sheet, imports: imports};
                                 this.#loaded.set(cssPath, read);
                                 return read;
                             })
                             .catch(error => {
                                 this.#files.delete(cssPath);
                                 console.error('load yojaWeb css failed: ' + cssPath, {cause: error});
                                 throw error;
                             });
            this.#files.set(cssPath, file);
        }
        return file;
    }

}

/**
 * @param css     a file's css, its comments out
 * @param fileUrl the file's url
 * @return the css, its relative url()s written absolute -- resolved from the file; a data:, a #fragment, an
 *         absolute url or one from the site's root left as it is
 */
function absoluteUrls(css, fileUrl) {
    let result = '';
    let i = 0;
    while (i < css.length) {
        if (css[i] === '"' || css[i] === "'") {
            const end = endOfString(css, i);
            result += css.substring(i, end);
            i = end;
        }
        else if (/^url\(/i.test(css.substring(i, i + 4))) {
            const end = closingParenthesis(css, i + 3);
            const raw = css.substring(i + 4, end).trim();
            const value = raw.length > 1 && (raw[0] === '"' || raw[0] === "'") && raw.endsWith(raw[0])
                              ? raw.substring(1, raw.length - 1)
                              : raw;
            const relative = value && !/^([a-z][a-z0-9+.-]*:|\/|#)/i.test(value);
            result += relative ? 'url("' + new URL(value, fileUrl).href.replace(/"/g, '%22') + '")'
                               : css.substring(i, end + 1);
            i = end + 1;
        }
        else {
            result += css[i];
            i++;
        }
    }
    return result;
}

/** @return the index just after the string starting at {@code start} (its quote included) */
function endOfString(css, start) {
    const quote = css[start];
    let i = start + 1;
    while (i < css.length && css[i] !== quote) {
        i += css[i] === '\\' ? 2 : 1;
    }
    return Math.min(i + 1, css.length);
}

/** @return the text, its comments each a space -- not a comment's look-alike in a string */
function withoutComments(text) {
    let result = '';
    let i = 0;
    while (i < text.length) {
        if (text[i] === '/' && text[i + 1] === '*') {
            const end = text.indexOf('*/', i + 2);
            i = end < 0 ? text.length : end + 2;
            result += ' ';
        }
        else if (text[i] === '"' || text[i] === "'") {
            const end = endOfString(text, i);
            result += text.substring(i, end);
            i = end;
        }
        else {
            result += text[i];
            i++;
        }
    }
    return result;
}

/**
 * @return the index of the ';' ending the statement started at {@code start} -- not one in a string, a comment or
 *         (...)
 */
function endOfStatement(css, start) {
    let depth = 0;
    let i = start;
    while (i < css.length) {
        const character = css[i];
        if (character === '"' || character === "'") {
            i = endOfString(css, i);
            continue;
        }
        if (character === '/' && css[i + 1] === '*') {
            const end = css.indexOf('*/', i + 2);
            i = end < 0 ? css.length : end + 2;
            continue;
        }
        if (character === '(') {
            depth++;
        }
        else if (character === ')') {
            depth--;
        }
        else if (character === ';' && depth <= 0) {
            return i;
        }
        i++;
    }
    return css.length;
}

/**
 * @param statement what follows "@import": its url, then its optional layer, supports() and media list
 * @return {path, media, supports, layer}, or null when it has no url
 */
function parseImport(statement) {
    let rest = statement.trim();
    let path;
    const unquote = value => value.length > 1 && (value[0] === '"' || value[0] === "'") && value.endsWith(value[0])
                                 ? value.substring(1, value.length - 1)
                                 : value;
    if (/^url\(/i.test(rest)) {
        const end = closingParenthesis(rest, 3);
        path = unquote(rest.substring(4, end).trim());
        rest = rest.substring(end + 1).trim();
    }
    else if (rest[0] === '"' || rest[0] === "'") {
        const end = endOfString(rest, 0);
        path = unquote(rest.substring(0, end));
        rest = rest.substring(end).trim();
    }
    else {
        return null;
    }
    let layer = null;
    const layerMatch = rest.match(/^layer(\s|$|\()/i);
    if (layerMatch) {
        if (layerMatch[1] === '(') {
            const end = closingParenthesis(rest, 5);
            layer = rest.substring(6, end).trim();
            rest = rest.substring(end + 1).trim();
        }
        else {
            layer = '';   // anonymous
            rest = rest.substring(5).trim();
        }
    }
    let supports = null;
    if (/^supports\(/i.test(rest)) {
        const end = closingParenthesis(rest, 8);
        const condition = rest.substring(9, end).trim();
        supports = condition.startsWith('(') || /^(selector|font-tech|font-format)\(/i.test(condition)
                       ? condition
                       : '(' + condition + ')';
        rest = rest.substring(end + 1).trim();
    }
    return {path: path, media: rest || null, supports: supports, layer: layer};
}

/** @return the index of the ')' closing the '(' at {@code open} -- not one in a string */
function closingParenthesis(value, open) {
    let depth = 0;
    for (let i = open; i < value.length; i++) {
        if (value[i] === '"' || value[i] === "'") {
            i = endOfString(value, i) - 1;
        }
        else if (value[i] === '(') {
            depth++;
        }
        else if (value[i] === ')' && --depth === 0) {
            return i;
        }
    }
    return value.length;
}

const cssLoader = new CssLoader();

export function load(cssPath) {
    return cssLoader.load(cssPath);
}
