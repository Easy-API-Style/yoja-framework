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

import './ywAssert.js'

export function fail(error) {
    window.ywAssert.fail(error)
}

export function assertEquals(expected, actual, message) {
    window.ywAssert.assertEquals(expected, actual, message)
}

export function assertTrue(value, message) {
    window.ywAssert.assertTrue(value, message)
}

export function assertFalse(value, message) {
    window.ywAssert.assertFalse(value, message)
}

export function assertNull(value, message) {
    window.ywAssert.assertNull(value, message)
}

export function assertUndefined(value, message) {
    window.ywAssert.assertUndefined(value, message)
}

export function assertNotNull(value, message) {
    window.ywAssert.assertNotNull(value, message)
}

export function assertNotUndefined(value, message) {
    window.ywAssert.assertNotUndefined(value, message)
}

export function assertArrayEquals(expected, actual, message) {
    window.ywAssert.assertArrayEquals(expected, actual, message)
}
