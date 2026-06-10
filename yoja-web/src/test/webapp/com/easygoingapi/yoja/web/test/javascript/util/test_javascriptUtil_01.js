'use strict'

const jsUtil = await import(yojaWeb.path('/yoja/util/javascriptUtil.js'))

class Person {
    
    #name
    
    constructor(name) {
        this.#name = name
    }
    
    toJson() {
        return { name: this.#name }
    }
}

export function test_01() {
    const tom = new Person('Tom')
    ywAssert.assertEquals("2011-10-05T14:48:00.000Z", jsUtil.stringify(new Date("05 October 2011 14:48 UTC")))
    ywAssert.assertEquals("{\"name\":\"Tom\"}", jsUtil.stringify(tom))
    ywAssert.assertEquals(33, jsUtil.stringify(33))
    ywAssert.assertEquals(99, jsUtil.stringify(new Number(99)))
    ywAssert.assertEquals(true, jsUtil.stringify(true))
    ywAssert.assertEquals(true, jsUtil.stringify(new Boolean(true)))
    ywAssert.assertEquals("aaaa", jsUtil.stringify("aaaa"))
    ywAssert.assertEquals("aaaa", jsUtil.stringify(new String("aaaa")))
    ywAssert.assertEquals("{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}",
                        jsUtil.stringify({key_1: "value_1", key_2: "value_2" }))
    ywAssert.assertEquals("[{\"key_1\":\"value_1\",\"key_2\":\"value_2\"},{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}]",
                        jsUtil.stringify([{key_1: "value_1", key_2: "value_2" },
                                          {key_1: "value_1", key_2: "value_2" }]))
}

export function test_02() {
    const tom = new Person('Tom')
    ywAssert.assertEquals(true, jsUtil.parse(new Date("05 October 2011 14:48 UTC")) instanceof Date)
    ywAssert.assertEquals(true, jsUtil.parse(tom) instanceof Person)
    ywAssert.assertEquals(33, jsUtil.parse(33))
    ywAssert.assertEquals("aaaa", jsUtil.parse("aaaa"))
    ywAssert.assertEquals({key_1: "value_1", key_2: "value_2"}, 
                        jsUtil.parse({key_1: "value_1", key_2: "value_2" }))
    ywAssert.assertEquals([{key_1: "value_1", key_2: "value_2" }, {key_1: "value_1", key_2: "value_2" }],
                        jsUtil.parse([{key_1: "value_1", key_2: "value_2" }, {key_1: "value_1", key_2: "value_2" }]))
    ywAssert.assertEquals({key_1: "value_1", key_2: "value_2" },
                        jsUtil.parse("{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}"))
    ywAssert.assertEquals([{key_1: "value_1", key_2: "value_2" }, {key_1: "value_1", key_2: "value_2" }], 
                        jsUtil.parse("[{\"key_1\":\"value_1\",\"key_2\":\"value_2\"},{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}]"))
}

export function test_03() {
    const Tom = new Person('Tom')
    ywAssert.assertEquals(false, jsUtil.isStringJson(new Date("05 October 2011 14:48 UTC")))
    ywAssert.assertEquals(false, jsUtil.isStringJson(Tom))
    ywAssert.assertEquals(false, jsUtil.isStringJson(33))
    ywAssert.assertEquals(false, jsUtil.isStringJson(true))
    ywAssert.assertEquals(false, jsUtil.isStringJson("aaaa"))
    ywAssert.assertEquals(false, jsUtil.isStringJson({key_1: "value_1", key_2: "value_2" }))
    ywAssert.assertEquals(false, jsUtil.isStringJson([{key_1: "value_1", key_2: "value_2" },
                                                   {key_1: "value_1", key_2: "value_2" }]))
    ywAssert.assertEquals(true, jsUtil.isStringJson("{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}"))
    ywAssert.assertEquals(true, jsUtil.isStringJson("[{\"key_1\":\"value_1\",\"key_2\":\"value_2\"},{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}]"))
    
    ywAssert.assertEquals(false, jsUtil.isStringArray([{key_1: "value_1", key_2: "value_2" },
                                                    {key_1: "value_1", key_2: "value_2" }]))
    ywAssert.assertEquals(false, jsUtil.isStringArray("{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}"))
    ywAssert.assertEquals(true, jsUtil.isStringArray("[{\"key_1\":\"value_1\",\"key_2\":\"value_2\"},{\"key_1\":\"value_1\",\"key_2\":\"value_2\"}]"))
}

export function test_04() {
    class Person {
        #name
        constructor(name) {
            this.#name = name
        }
    }
    const Tom = new Person('Tom')
    ywAssert.assertEquals('{}', jsUtil.stringify(Tom))
}
