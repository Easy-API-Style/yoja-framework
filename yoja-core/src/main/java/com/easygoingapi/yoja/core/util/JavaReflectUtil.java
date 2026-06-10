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
package com.easygoingapi.yoja.core.util;

import java.lang.reflect.Field;

import com.easygoingapi.yoja.core.YojaAppException;

/**
 * Utility interface for Java reflection helpers.
 */
public interface JavaReflectUtil {

    /**
     * Finds a {@link Field} by name, searching the given class and its superclasses.
     *
     * @param clazz     the class to start the search from; returns {@code null} if {@code null}
     * @param fieldName the name of the field to find
     * @return the {@link Field}, or {@code null} if the class hierarchy is exhausted
     * @throws com.easygoingapi.yoja.core.YojaAppException if reflection access fails
     */
    public static Field getField(final Class<?> clazz,
                                 final String fieldName) {
        Field field = null;
        if (clazz != null) {
            try {
                try {
                    field = clazz.getDeclaredField(fieldName);
                }
                catch (final NoSuchFieldException e) {
                    field = getField(clazz.getSuperclass(), fieldName);
                }
            } 
            catch (final Exception e) {
                throw new YojaAppException("find field reflect attribute failed fieldName = " + fieldName, e);
            }
        }
        return field;
    }
    
    /**
     * Reads the value of a named field from the given object, bypassing access modifiers.
     * The field's original accessibility is restored after reading.
     *
     * @param <O>       the expected return type
     * @param object    the object to read the field from
     * @param fieldName the name of the field
     * @return the field value cast to {@code O}, or {@code null} if the field is not found
     * @throws com.easygoingapi.yoja.core.YojaAppException if reflection access fails
     */
    @SuppressWarnings("unchecked")
	public static <O> O getFieldValue(final Object object,
                                      final String fieldName) {
        O result = null;
        try {
            final Field field = getField(object.getClass(), fieldName);
            if (field != null) {
                final boolean accessible = field.canAccess(object);
                field.setAccessible(true);
                result = (O) field.get(object);
                field.setAccessible(accessible);  
            }
        } 
        catch (final Exception e) {
            throw new YojaAppException("get reflect attribute failed fieldName = " + fieldName, e);
        }
        return result;
    }
    
}
