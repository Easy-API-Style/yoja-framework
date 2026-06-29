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

function unwrapPrimitive(value) {
    // Boxed primitives (new Number(99), new String("x"), new Boolean(true), ...)
    // are compared by their primitive value, so 99 equals new Number(99).
    if (value instanceof Number
          || value instanceof String
          || value instanceof Boolean
          || value instanceof BigInt
          || value instanceof Symbol) {
        return value.valueOf()
    }
    return value
}

function deepEquals(expected, actual) {
    expected = unwrapPrimitive(expected)
    actual = unwrapPrimitive(actual)
    // Same reference, or strictly equal primitives (also matches NaN === NaN).
    if (Object.is(expected, actual)) {
        return true
    }
    // From here at least one is a non-null object; bail out on any mismatch.
    if (typeof expected !== "object"
          || expected === null
          || typeof actual !== "object" 
          || actual === null) {
        return false
    }
    // Compare by the same underlying type (Array, Date, RegExp, plain object, ...).
    if (Object.prototype.toString.call(expected) !== Object.prototype.toString.call(actual)) {
        return false
    }
    if (expected instanceof Date) {
        return expected.getTime() === actual.getTime()
    }
    if (expected instanceof RegExp) {
        return expected.source === actual.source
                   && expected.flags === actual.flags
    }
    if (Array.isArray(expected)) {
        if (expected.length !== actual.length) {
            return false
        }
        for (let i = 0; i < expected.length; i++) {
            if (!deepEquals(expected[i], actual[i])) {
                return false
            }
        }
        return true
    }
    if (expected instanceof Map) {
        if (expected.size !== actual.size) {
            return false
        }
        for (const [key, value] of expected) {
            if (!actual.has(key) || !deepEquals(value, actual.get(key))) {
                return false
            }
        }
        return true
    }
    if (expected instanceof Set) {
        if (expected.size !== actual.size) {
            return false
        }
        for (const value of expected) {
            if (!actual.has(value)) {
                return false
            }
        }
        return true
    }
    // Plain objects: same set of own enumerable keys, regardless of order.
    const expectedKeys = Object.keys(expected)
    const actualKeys = Object.keys(actual)
    if (expectedKeys.length !== actualKeys.length) {
        return false
    }
    for (const key of expectedKeys) {
        if (!Object.prototype.hasOwnProperty.call(actual, key)
               || !deepEquals(expected[key], actual[key])) {
            return false
        }
    }
    return true
}

class YojaWebAssert {

    constructor() {
    }

    fail(error) {
        throw new Error(error)
    }

    assertEquals(expected, actual, message) {
        if (!deepEquals(expected, actual)) {
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
                    if (!deepEquals(expected[i], actual[i])) {
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

