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

class UrlParameter {
    
    #urlParameter;
    
    constructor(urlParameter) {
        this.#urlParameter = parseUrlParameterAsArray(urlParameter);
    }
    
    #emptyToNull(value) {
        let result = value;
        if (result === null 
             || result === undefined 
             || result == '') {
            result = null;
        }
        return result;
    }
     
    has(key, value) {
        let result = false;
        const _value = this.#emptyToNull(value);
        for (const parameter of this.#urlParameter) {
            if (value === undefined) {
                if (key === parameter.key) {
                    result = true;
                    break;
                }
            }
            else {
                if (key === parameter.key 
                      && _value === parameter.value) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    keys() {
        const result = [];
        for (const parameter of this.#urlParameter) {
            const key = parameter.key;
            if (!result.includes(key)) {
                result.push(key);
            }
        }
        return result;
    }

    get(key) {
        let result = null;
        for (const parameter of this.#urlParameter) {
            if (key === parameter.key) {
                result = parameter.value;
                break;
            }
        }
        return result;
    }
    
    getAll(key) {
        const result = [];
        for (const parameter of this.#urlParameter) {
            if (key === parameter.key) {
                result.push(parameter.value);
            }
        }
        return result;
    }

    entries() {
        const result = [];
        for (const parameter of this.#urlParameter) {
           result.push(parameter);
        }
        return result;
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
        const _value = this.#emptyToNull(value);
        this.#urlParameter.push({key: key, value: _value});
    }
    
    set(key, value) {
        const _value = this.#emptyToNull(value);
        let done = false;
        const result = [];
        for (const parameter of this.#urlParameter) {
            if (key === parameter.key) {
                if (!done) {
                    result.push({key: key, value: _value});
                    done = true;
                }
            }
            else {
                result.push(parameter);
            }
        }
        if (!done) {
            result.push({key: key, value: _value});
        }
        this.#urlParameter = result;
    }

    remove(key, value) {
        const _value = this.#emptyToNull(value);
        const removeAll = value === undefined
        const result = [];
        for (const parameter of this.#urlParameter) {
            if (key === parameter.key) {
                if (!removeAll  && parameter.value !== _value) {
                    result.push(parameter);
                }
            }
            else {
                result.push(parameter);
            }
        }
        this.#urlParameter = result;
    }
    
    equals(object) {
        if (object instanceof UrlParameter) {
            return JSON.stringify(this.entries()) === JSON.stringify(object.entries());
        }
        return false;
    }
    
    toJson() {
        return {...this.#urlParameter};
    }
    
    clone() {
        return new UrlParameter(this.#urlParameter);
    }
    
    toString() {
       return JSON.stringify(this.toJson());
    }
     
}

function emptyToNull(value) {
    let result = value
    if (result === null 
         || result === undefined 
         || result == '') {
        result = null;
    }
    return result;
}

/*
   managed urlParameter formats:
   ->   UrlParameter class
   ->   URLSearchParams class
   ->   [ {key: "key_1", value: "value_11"}, 
          {key: "key_1", value: "value_12"}, 
          {key: "key_2", value: "value_21"} ]
          
   ->   [ {key: "key_1", value: ["value_11", "value_12"]}, 
          {key: "key_2", value: "value_21"} ]
          
   ->   { key_1: ["value_11", "value_12"],
          key_2: "value_12" }
   
   ->   key_1=value_11&key_1=value_12&key_2=value_21     
  
   result: [ {key: "key_1", value: "value_11"}, 
             {key: "key_1", value: "value_12"}, 
             {key: "key_2", value: "value_21"} ]
   
*/
function parseUrlParameterAsArray(urlParameter) {
    let result = [];
    if (urlParameter) {
        if (urlParameter instanceof UrlParameter) {
            result = urlParameter.entries();
        }
        else if (urlParameter instanceof URLSearchParams) {
            for (const [key, value] of urlParameter.entries()) {
                result.push({key: key, value: emptyToNull(value)});
            }
        }
        else if (Array.isArray(urlParameter)) {
            for (const parameter of urlParameter) {
                if (Array.isArray(parameter.value)) {
                    for (const value of parameter.value) {
                        result.push({key: parameter.key, value: emptyToNull(value)});
                    }
                }
                else {
                    result.push({key: parameter.key, value: emptyToNull(parameter.value)});
                }
            }
        } 
        else if (typeof urlParameter === 'object') {
            for (const key in urlParameter) {
                if (Array.isArray(urlParameter[key])) {
                    for (const value of urlParameter[key]) {
                        result.push({key: key, value: emptyToNull(value)});
                    }
                }
                else {
                    result.push({key: key, value: emptyToNull(urlParameter[key])});
                }
            }
        }
        else if (typeof urlParameter === 'string') {
            result = parseUrlParameterAsArray(new URLSearchParams(decodeURI(urlParameter)));
        }
    }
    return result;
}

export function parseUrlParameter(urlParameter) {
    return new UrlParameter(urlParameter);
}

export function toUrlQuery(urlParameter) {
    const allEntries = parseUrlParameterAsArray(urlParameter);
    const keyValues = [];
    if (allEntries.length) {
        for (const entry of allEntries) {
            if (entry.value === null) {
                keyValues.push(encodeURIComponent(entry.key));
            }
            else {
                keyValues.push(encodeURIComponent(entry.key) + '=' + encodeURIComponent(entry.value));
            }
        }
    }
    return keyValues.length 
              ? keyValues.join('&')
              : undefined;
}

export function toUrl(url, urlParameter) {
    const urlQuery = toUrlQuery(urlParameter);
    //url = encodeURIComponent(url)
    if (urlQuery) {
        if (!url.includes('?')) {
            url = url + '?';
        }
        else {
            url = url + '&';
        }
        url = url + urlQuery;
    }
    return url;
}


