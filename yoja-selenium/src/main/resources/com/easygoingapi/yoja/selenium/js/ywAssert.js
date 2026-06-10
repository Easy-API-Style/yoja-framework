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

function merge(message, errorMessage) {
    let result = errorMessage
    if (message) {
        result = message + " -> " + errorMessage
    }
    return result
}

class YojaWebAssert {

    constructor() {
    }

    fail(error) {
        throw new Error(error)
    }

    assertEquals(expected, actual, message) {
        if (JSON.stringify(expected) !== JSON.stringify(actual)) {
            const errorMessage = "expected '" + JSON.stringify(expected) 
                               + "' but it was '" + JSON.stringify(actual) + "'"
            throw new Error(merge(message, errorMessage))
        }
    }

    assertTrue(value, message) {
        if (value !== true) {
            throw new Error(merge(message, "it is not true"))
        }
    }

    assertFalse(value, message) {
        if (value !== false) {
            throw new Error(merge(message, "it is not false"))
        }
    }

    assertNull(value, message) {
        if (value !== null) {
            throw new Error(merge(message, "it is not null"))
        }
    }

    assertUndefined(value, message) {
        if (value !== undefined) {
            throw new Error(merge(message, "it is not undefined"))
        }
    }

    assertNotNull(value, message) {
        if (value === null) {
            throw new Error(merge(message, "it is null"))
        }
    }

    assertNotUndefined(value, message) {
        if (value === undefined) {
            throw new Error(merge(message, "it is undefined"))
        }
    }

    assertArrayEquals(expected, actual, message) {
        if (Array.isArray(expected)
            && Array.isArray(actual)) {
            if (expected.length === actual.length) {
                for (let i = 0; i < expected.length; i++) {
                    if (JSON.stringify(expected[i]) !== JSON.stringify(actual[i])) {
                        const errorMessage = "index " + i
                                           + " expected '" + JSON.stringify(expected[i])
                                           + "' but it was '" + JSON.stringify(actual[i]) + "'"
                                           + "\nexpected:\n"
                                           + JSON.stringify(expected)
                                           + "\nactual:\n"
                                           + JSON.stringify(actual)
                        throw new Error(merge(message, errorMessage))
                    }
                }
            }
            else {
                const errorMessage = "array not the same size: expected '" + expected.length
                                   + "' but it was '" + actual.length + "'"
                                   + "\nexpected:\n"
                                   + JSON.stringify(expected)
                                   + "\nactual:\n"
                                   + JSON.stringify(actual)
                throw new Error(merge(message, errorMessage))
            }
        }
        else {
            const errorMessage = "assert array needs arrays"
                               + "\nexpected:\n"
                               + JSON.stringify(expected)
                               + "\nactual:\n"
                               + JSON.stringify(actual)
            throw new Error(merge(message, errorMessage))
        }
    }
    
}

window.ywAssert = new YojaWebAssert()

