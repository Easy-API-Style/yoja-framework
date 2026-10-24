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

/*
 * URL parameters, as a list of {key, value}: a key may come several times; a value null, undefined or '' is no
 * value (written as the key alone: "b", not "b=").
 */
class UrlParameter {

    #urlParameter;

    constructor(urlParameter) {
        this.#urlParameter = parseUrlParameterAsArray(urlParameter);
    }

    has(key, value) {
        if (value === undefined) {
            return this.#urlParameter.some(parameter => parameter.key === key);
        }
        const _value = emptyToNull(value);
        return this.#urlParameter.some(parameter => parameter.key === key && parameter.value === _value);
    }

    keys() {
        const result = [];
        for (const parameter of this.#urlParameter) {
            if (!result.includes(parameter.key)) {
                result.push(parameter.key);
            }
        }
        return result;
    }

    get(key) {
        const parameter = this.#urlParameter.find(candidate => candidate.key === key);
        return parameter ? parameter.value : null;
    }

    getAll(key) {
        return this.#urlParameter.filter(parameter => parameter.key === key)
                                 .map(parameter => parameter.value);
    }

    /** @return its {key, value}, copies: changing one does not change it */
    entries() {
        return this.#urlParameter.map(parameter => ({...parameter}));
    }

    size() {
        return this.#urlParameter.length;
    }

    toUrlQuery() {
        return toUrlQuery(this.#urlParameter);
    }

    clear() {
        this.#urlParameter = [];
    }

    append(key, value) {
        this.#urlParameter.push({key: key, value: emptyToNull(value)});
    }

    /** Sets a key's value: at its first place, its other values dropped -- last when it had none. */
    set(key, value) {
        const _value = emptyToNull(value);
        let done = false;
        const result = [];
        for (const parameter of this.#urlParameter) {
            if (key !== parameter.key) {
                result.push(parameter);
            }
            else if (!done) {
                result.push({key: key, value: _value});
                done = true;
            }
        }
        if (!done) {
            result.push({key: key, value: _value});
        }
        this.#urlParameter = result;
    }

    /** Removes a key -- every value of it, or the one given only. */
    remove(key, value) {
        const removeAll = value === undefined;
        const _value = emptyToNull(value);
        this.#urlParameter = this.#urlParameter.filter(parameter => parameter.key !== key
                                                                    || (!removeAll && parameter.value !== _value));
    }

    /** @return whether it holds the same values for the same keys -- in the same order for a key */
    equals(object) {
        if (!(object instanceof UrlParameter) || this.size() !== object.size()) {
            return false;
        }
        const keys = this.keys();
        if (keys.length !== object.keys().length) {
            return false;
        }
        for (const key of keys) {
            const values = this.getAll(key);
            const otherValues = object.getAll(key);
            if (values.length !== otherValues.length
                  || values.some((value, index) => value !== otherValues[index])) {
                return false;
            }
        }
        return true;
    }

    /** @return its {key, value}, as a list: what parseUrlParameter reads back */
    toJson() {
        return this.entries();
    }

    clone() {
        return new UrlParameter(this.#urlParameter);
    }

    toString() {
        return JSON.stringify(this.toJson());
    }

}

export function cleanHash(hash) {
    let result = null;
    if (typeof hash === 'string'
          && hash.trim() !== ''
          && hash.trim() !== '#') {
        result = hash.trim();
        if (result.startsWith('#')) {
            result = result.substring(1);
        }
    }
    return result;
}

/** @return the value, or null when it is none: null, undefined or '' */
function emptyToNull(value) {
    return value === null || value === undefined || value === '' ? null : value;
}

/**
 * @param key   a key
 * @param value its value -- or a list of them: the key once for each
 * @param result the {key, value} list they are added to
 */
function pushValues(key, value, result) {
    for (const each of Array.isArray(value) ? value : [value]) {
        result.push({key: key, value: emptyToNull(each)});
    }
}

/*
   managed urlParameter formats:
   ->   UrlParameter class
   ->   URLSearchParams class
   ->   Map: key -> value, or [values]
   ->   [ {key: "key_1", value: "value_11"},
          {key: "key_1", value: "value_12"},
          {key: "key_2", value: "value_21"} ]

   ->   [ {key: "key_1", value: ["value_11", "value_12"]},
          {key: "key_2", value: "value_21"} ]

   ->   { key_1: ["value_11", "value_12"],
          key_2: "value_12" }

   ->   key_1=value_11&key_1=value_12&key_2=value_21   (a leading '?' allowed; decoded once, as URLSearchParams does)

   result: [ {key: "key_1", value: "value_11"},
             {key: "key_1", value: "value_12"},
             {key: "key_2", value: "value_21"} ]
*/
function parseUrlParameterAsArray(urlParameter) {
    const result = [];
    if (!urlParameter) {
        return result;
    }
    if (urlParameter instanceof UrlParameter) {
        return urlParameter.entries();
    }
    if (urlParameter instanceof URLSearchParams || urlParameter instanceof Map) {
        for (const [key, value] of urlParameter) {
            pushValues(key, value, result);
        }
    }
    else if (Array.isArray(urlParameter)) {
        for (const parameter of urlParameter) {
            pushValues(parameter.key, parameter.value, result);
        }
    }
    else if (typeof urlParameter === 'object') {
        for (const [key, value] of Object.entries(urlParameter)) {   // its own keys only
            pushValues(key, value, result);
        }
    }
    else if (typeof urlParameter === 'string') {
        // URLSearchParams decodes it, once: decoding it before (decodeURI) decoded '%25' twice, and threw on a
        // lone '%'
        return parseUrlParameterAsArray(new URLSearchParams(urlParameter));
    }
    return result;
}

export function parseUrlParameter(urlParameter) {
    return new UrlParameter(urlParameter);
}

export function toUrlQuery(urlParameter) {
    const keyValues = parseUrlParameterAsArray(urlParameter)
                          .map(entry => entry.value === null
                                            ? encodeURIComponent(entry.key)
                                            : encodeURIComponent(entry.key) + '=' + encodeURIComponent(entry.value));
    return keyValues.length
              ? keyValues.join('&')
              : undefined;
}

/**
 * @param url          a url -- it may hold parameters already, and a hash
 * @param urlParameter the parameters added to it
 * @param hash         its hash -- in place of the one it holds; none: the one it holds kept
 * @return the url, the parameters added before its hash
 */
export function toUrl(url, urlParameter, hash) {
    const hashAt = url.indexOf('#');
    let result = hashAt < 0 ? url : url.substring(0, hashAt);
    const urlHash = hashAt < 0 ? null : url.substring(hashAt + 1);
    const urlQuery = toUrlQuery(urlParameter);
    if (urlQuery) {
        if (!result.includes('?')) {
            result = result + '?';
        }
        else if (!result.endsWith('?') && !result.endsWith('&')) {
            result = result + '&';
        }
        result = result + urlQuery;
    }
    const finalHash = hash ? cleanHash(hash) : cleanHash(urlHash);
    if (finalHash) {
        result = result + '#' + finalHash;
    }
    return result;
}
