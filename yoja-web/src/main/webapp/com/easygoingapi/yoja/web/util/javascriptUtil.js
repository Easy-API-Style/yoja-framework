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

/*
 *   parse/stringify
 */
export function parse(value) {
    let result;
    try {
        if (value instanceof String
               || typeof value === 'string') {
            result = JSON.parse(value);
        }
        else {
            result = value;
        }
    }
    catch(error) {
        result = value;
    }
    return result;
}

export function stringify(value) {
    let result;
    if (value instanceof Date) {
        result = value.toJSON();
    }
    else if (value instanceof Object
                && value.toJson instanceof Function) {
        result = JSON.stringify(value.toJson());
    }
    else if (value instanceof Number 
               || value instanceof Boolean
               || value instanceof String) {
        result = value;
    }
    else if (value instanceof Object) {
        result = JSON.stringify(value);
    }
    else {
        result = value;
    }
    return result;
}

export function isStringJson(value) {
    let result = false;
    try {
        if (value instanceof String 
               || typeof value === 'string') {
            JSON.parse(value)
            result = true;
        }
    }
    catch(error) {
        result = false;
    }
    return result;
}

export function isStringArray(value) {
    let result = false;
    try {
        if (value instanceof String 
               || typeof value === 'string') {
            const v = JSON.parse(value);
            result = v instanceof Array;
        }
    }
    catch(error) {
        result = false;
    }
    return result;
}

/*
 *   base64/blob
 */
export function blobTobase64(blob) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    })
}
  
export function base64ToBlob(base64) {
    const base64Entry = splitBase64(base64);
    const byteArrays = base64ToUint8BytedArray(base64Entry.data);
    let result ;
    if (base64Entry.contentType) {
        result = new Blob(byteArrays, {type: base64Entry.contentType});
    }
    else {
        result = new Blob(byteArrays);
    }
    return result;
}

export function splitBase64(base64) {
    let data = base64;
    let contentType = null;
    if (base64.includes(',')) {
        const splitBase64 = base64.split(',');
        data = splitBase64[1];
        if (splitBase64[0].includes(';')) {
            const splitPrefixies = splitBase64[0].split(';')
                                                 .map(item => item.trim());
            for (const prefix of splitPrefixies) {
                if (prefix.startsWith('data:')) {
                    contentType = prefix.split('data:')[1].trim();
                    break;
                }
            }
        }
    }
    return {data: data, 
            contentType: contentType};
}

export function base64ToUint8BytedArray(base64) {
    if (base64) {
        const characterArray = atob(base64);
        return characterArrayToUint8BytedArray(characterArray);
    }
    return [];
}

/*
 *   byte
 */
export function characterArrayToUint8BytedArray(characterArray, sliceSize = 512) {
    const uint8BytedArray = [];
    if (characterArray && characterArray.length) {
        for (let offset = 0; offset < characterArray.length; offset += sliceSize) {
            const slice = characterArray.slice(offset, offset + sliceSize);
            const byteNumbers = new Array(slice.length);
            for (let i = 0; i < slice.length; i++) {
                byteNumbers[i] = slice.charCodeAt(i);
            }
            const byteArray = new Uint8Array(byteNumbers);
            uint8BytedArray.push(byteArray);
        }
    }
    return uint8BytedArray;
}

/*
 *   value
 */
export function requireValue(value, message) {
    if (value === null || value === undefined) {
        if (message) {
            throw new Error(message);
        }
        else {
            throw new Error('required value');
        }
    }
}

export function undefinedToNull(value) {
    let result = value;
    if (result === undefined) {
        result = null;
    }
    return result;
}

export function valueOf(value) {
    let result;
    if (value instanceof Function) {
        result = value();
    }
    else {
        result = value;
    }
    return result;
}

/*
 *  class
 */
export function isInstanceOf(object, className) {
    return className === getClassName(object);
}

export function getClassName(object) {
    let result;
    if (object && object.constructor) {
        result = object.constructor.name;
    }
    return result;
}

/*
 *  file
 */
export function getFileName(path) {
    let result;
    if (path) {
        if (path.includes('/')) {
            const lastIndex = path.lastIndexOf('/');
            result = path.substring(lastIndex + 1);
        }
    }
    return result;
}

export function getOnlyFileName(path) {
    let result;
    if (path) {
        result = getFileName(path);
        if (result.includes('.')) {
            const lastIndex = result.lastIndexOf('.');
            result = result.substring(0, lastIndex);
        }
    }
    return result;
}

export function onlyUrlPath(url) {
    let result = url;
    if (url) {
        if (URL.canParse(url)) {
            const urlObject = URL.parse(url);
            result = urlObject.pathname;
        }
        else if (url.includes('?')) {
            result = url.substring(0, url.indexOf('?'));
        }
    }
    return result;
}

/*
 *  typeof
 */
export function isString(value)  {
    return value instanceof String || typeof value === 'string';
}

export function isNumber(value)  {
    return value instanceof Number || typeof value === 'number';
}

export function isFunction(value)  {
    return value instanceof Function || typeof value === 'function';
}

export function isBoolean(value)  {
    return value instanceof Boolean || typeof value === 'boolean';
}

export function isArray(values)  {
    return Array.isArray(values);
}

export function isNotEmptyArray(values)  {
    return Array.isArray(values) && values.length > 0;
}

/*
 *  array
 */

/*
 * 
 *  values: String[]
 * 
 *  query: String 
 *             | RegExp
 *             | { startsWith: String | String[], 
 *                 endsWith: String | String[], 
 *                 contains: String | String[], 
 *                 equals: String | String[],
 *                 matches: String | RegExp } 
 * 
 */
export function filterStringArray(values, query) {
    let result = [];
    if (values && values.length > 0) {
        if (isString(query)) {
            for (const value of values) {
                if (value == query) {
                    result.push(value);
                }
            }
        }
        else if (query instanceof RegExp) {
            for (const value of values) {
                if (query.test(value)) {
                    result.push(value);
                }
            }
        }
        else if (query instanceof Object) {
            for (const value of values) {
                // startsWith
                if (isArray(query.startsWith)) {
                    let found = false;
                    for (const startsWithValue of query.startsWith) {
                        if (value.startsWith(startsWithValue)) {
                            result.push(value);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        continue;
                    }
                }
                else if (isString(query.startsWith)) {
                    if (value.startsWith(query.startsWith)) {
                        result.push(value);
                        continue;
                    }
                }
                // endsWith
                if (isArray(query.endsWith)) {
                    let found = false;
                    for (const endsWithValue of query.endsWith) {
                        if (value.endsWith(endsWithValue)) {
                            result.push(value);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        continue;
                    }
                }
                else if (isString(query.endsWith)) {
                    if (value.endsWith(query.endsWith)) {
                        result.push(value);
                        continue;
                    }
                }
                // contains
                if (isArray(query.contains)) {
                    let found = false;
                    for (const containsValue of query.contains) {
                        if (value.includes(containsValue)) {
                            result.push(value);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        continue;
                    }
                }
                else if (isString(query.contains)) {
                    if (value.includes(query.contains)) {
                        result.push(value);
                        continue;
                    }
                }
                // equals
                if (isArray(query.equals)) {
                    if (query.equals.includes(value)) {
                        result.push(value);
                        continue;
                    }
                }
                else if (isString(query.equals)) {
                    if (value === query.equals) {
                        result.push(value);
                        continue;
                    }
                }
                // matches
                if (query.matches instanceof RegExp) {
                    if (query.matches.test(value)) {
                        result.push(value);
                        continue;
                    }
                }
                else if (isString(query.matches)) {
                    if (value.match(new RegExp(query.matches))) {
                        result.push(value);
                        continue;
                    }
                }
            }
        }
    }
    return result;
}