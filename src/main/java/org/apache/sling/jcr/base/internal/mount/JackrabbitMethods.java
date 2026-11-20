/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.base.internal.mount;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.apache.jackrabbit.api.JackrabbitNode;
import org.apache.jackrabbit.api.JackrabbitSession;

/**
 * The single-lookup {@code get*OrNull} methods of newer Jackrabbit APIs ({@code JackrabbitSession.getItemOrNull},
 * {@code JackrabbitNode.getNodeOrNull}, ...), looked up at runtime because this bundle compiles against an API that
 * predates them. Where the running API has them, the proxies forward to the serving side's own implementation
 * (one repository lookup) instead of an exists-then-get pair.
 */
final class JackrabbitMethods {

    /** Returned by {@link #invoke} when the method is not available for the target. */
    static final Object UNAVAILABLE = new Object();

    static final MethodHandle SESSION_GET_ITEM_OR_NULL = find(JackrabbitSession.class, "getItemOrNull", Item.class);
    static final MethodHandle SESSION_GET_NODE_OR_NULL = find(JackrabbitSession.class, "getNodeOrNull", Node.class);
    static final MethodHandle SESSION_GET_PROPERTY_OR_NULL =
            find(JackrabbitSession.class, "getPropertyOrNull", Property.class);
    // JackrabbitNode.getNodeOrNull returns a JackrabbitNode
    static final MethodHandle NODE_GET_NODE_OR_NULL =
            find(JackrabbitNode.class, "getNodeOrNull", JackrabbitNode.class, Node.class);
    static final MethodHandle NODE_GET_PROPERTY_OR_NULL =
            find(JackrabbitNode.class, "getPropertyOrNull", Property.class);

    private JackrabbitMethods() {}

    private static MethodHandle find(Class<?> type, String name, Class<?>... returnTypes) {
        for (Class<?> rt : returnTypes) {
            try {
                return MethodHandles.publicLookup()
                        .findVirtual(type, name, MethodType.methodType(rt, String.class))
                        .asType(MethodType.methodType(Object.class, Object.class, String.class));
            } catch (ReflectiveOperationException | LinkageError e) {
                // not in the running API with this signature
            }
        }
        return null;
    }

    /**
     * Calls {@code method}, a {@code JackrabbitSession} method, on {@code session}; {@link #UNAVAILABLE} when the
     * running API lacks it or the session is not a {@code JackrabbitSession}.
     */
    static Object onSession(MethodHandle method, Object session, String path) throws RepositoryException {
        return method != null && session instanceof JackrabbitSession ? call(method, session, path) : UNAVAILABLE;
    }

    /** As {@link #onSession}, for a {@code JackrabbitNode} method. */
    static Object onNode(MethodHandle method, Object node, String path) throws RepositoryException {
        return method != null && node instanceof JackrabbitNode ? call(method, node, path) : UNAVAILABLE;
    }

    private static Object call(MethodHandle method, Object target, String path) throws RepositoryException {
        try {
            return method.invokeExact(target, path);
        } catch (RepositoryException | RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RepositoryException(t);
        }
    }
}
