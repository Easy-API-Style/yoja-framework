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
 * jsUnit tests for the 'YojaWebAssert' API exposed as 'window.ywAssert'
 * (see ywAssert.js). Each exported function is invoked by TestYwAssert via
 * TestBuilder.testJsUnit; any thrown error fails the corresponding test.
 */

const ywAssert = window.ywAssert

/**
 * Asserts that running 'action' throws (i.e. the underlying ywAssert call
 * detected a mismatch). Used for the negative test cases, since ywAssert has
 * no built-in 'assertThrows'.
 */
function assertThrows(action, label) {
    let thrown = null
    try {
        action()
    }
    catch (error) {
        thrown = error
    }
    if (thrown === null) {
        throw new Error(label + ": expected the assertion to fail, but it passed")
    }
    if (!(thrown instanceof Error)) {
        throw new Error(label + ": expected an Error, but got '" + thrown + "'")
    }
}

export function test_assertEquals_primitives() {
    ywAssert.assertEquals(1, 1)
    ywAssert.assertEquals("yoja", "yoja")
    ywAssert.assertEquals(true, true)
    ywAssert.assertEquals(null, null)
    ywAssert.assertEquals(undefined, undefined)
}

export function test_assertEquals_deepStructures() {
    ywAssert.assertEquals({ a: 1, b: "two" }, { a: 1, b: "two" })
    ywAssert.assertEquals([1, 2, 3], [1, 2, 3])
    ywAssert.assertEquals({ x: [1, { y: 2 }] }, { x: [1, { y: 2 }] })
}

export function test_assertEquals_keyOrderIndependent() {
    // The previous JSON.stringify comparison failed this case.
    ywAssert.assertEquals({ a: 1, b: 2 }, { b: 2, a: 1 })
}

export function test_assertEquals_specialValues() {
    ywAssert.assertEquals(NaN, NaN)
    ywAssert.assertEquals(new Date(0), new Date(0))
    ywAssert.assertEquals(/abc/gi, /abc/gi)
    ywAssert.assertEquals(new Set([1, 2, 3]), new Set([3, 2, 1]))
    ywAssert.assertEquals(new Map([["a", 1]]), new Map([["a", 1]]))
}

export function test_assertEquals_failsOnDifference() {
    assertThrows(() => ywAssert.assertEquals(1, 2), "assertEquals(1, 2)")
    assertThrows(() => ywAssert.assertEquals("a", "b"), "assertEquals('a', 'b')")
    assertThrows(() => ywAssert.assertEquals({ a: 1 }, { a: 2 }), "assertEquals({a:1}, {a:2})")
    assertThrows(() => ywAssert.assertEquals({ a: 1 }, { a: 1, b: 2 }), "assertEquals with extra key")
    assertThrows(() => ywAssert.assertEquals([1, 2], [1, 2, 3]), "assertEquals arrays of different length")
}

export function test_assertTrue() {
    ywAssert.assertTrue(true)
    assertThrows(() => ywAssert.assertTrue(false), "assertTrue(false)")
    assertThrows(() => ywAssert.assertTrue(1), "assertTrue(1) is not strictly true")
}

export function test_assertFalse() {
    ywAssert.assertFalse(false)
    assertThrows(() => ywAssert.assertFalse(true), "assertFalse(true)")
    assertThrows(() => ywAssert.assertFalse(0), "assertFalse(0) is not strictly false")
}

export function test_assertNull() {
    ywAssert.assertNull(null)
    assertThrows(() => ywAssert.assertNull(undefined), "assertNull(undefined)")
    assertThrows(() => ywAssert.assertNull("x"), "assertNull('x')")
}

export function test_assertNotNull() {
    ywAssert.assertNotNull("x")
    ywAssert.assertNotNull(undefined)
    assertThrows(() => ywAssert.assertNotNull(null), "assertNotNull(null)")
}

export function test_assertUndefined() {
    ywAssert.assertUndefined(undefined)
    assertThrows(() => ywAssert.assertUndefined(null), "assertUndefined(null)")
    assertThrows(() => ywAssert.assertUndefined("x"), "assertUndefined('x')")
}

export function test_assertNotUndefined() {
    ywAssert.assertNotUndefined("x")
    ywAssert.assertNotUndefined(null)
    assertThrows(() => ywAssert.assertNotUndefined(undefined), "assertNotUndefined(undefined)")
}

export function test_assertEquals_boxedPrimitives() {
    ywAssert.assertEquals(99, new Number(99))
    ywAssert.assertEquals(new Number(99), 99)
    ywAssert.assertEquals("yoja", new String("yoja"))
    ywAssert.assertEquals(true, new Boolean(true))
    ywAssert.assertEquals({ a: new Number(1) }, { a: 1 })
    assertThrows(() => ywAssert.assertEquals(99, new Number(98)), "boxed number mismatch")
    assertThrows(() => ywAssert.assertEquals(false, new Boolean(true)), "boxed boolean mismatch")
}

export function test_assertArrayEquals() {
    ywAssert.assertArrayEquals([], [])
    ywAssert.assertArrayEquals([1, 2, 3], [1, 2, 3])
    ywAssert.assertArrayEquals([{ a: 1 }, { b: 2 }], [{ a: 1 }, { b: 2 }])
}

export function test_assertArrayEquals_deepElements() {
    // Same spirit as assertEquals: element comparison is a deep, structural one.
    ywAssert.assertArrayEquals([{ a: 1, b: 2 }], [{ b: 2, a: 1 }])
    ywAssert.assertArrayEquals([NaN], [NaN])
    ywAssert.assertArrayEquals([99], [new Number(99)])
    ywAssert.assertArrayEquals([new Date(0)], [new Date(0)])
    ywAssert.assertArrayEquals([[1, { x: 2 }]], [[1, { x: 2 }]])
}

export function test_assertArrayEquals_failsOnDifference() {
    assertThrows(() => ywAssert.assertArrayEquals([1, 2], [1, 3]), "assertArrayEquals different element")
    assertThrows(() => ywAssert.assertArrayEquals([1], [1, 2]), "assertArrayEquals different size")
    assertThrows(() => ywAssert.assertArrayEquals("nope", [1]), "assertArrayEquals non-array argument")
    assertThrows(() => ywAssert.assertArrayEquals([{ a: 1 }], [{ a: 2 }]), "assertArrayEquals deep element mismatch")
}
