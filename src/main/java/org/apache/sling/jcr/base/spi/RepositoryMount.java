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
package org.apache.sling.jcr.base.spi;

import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitRepository;

/**
 * A {@code RepositoryMount} works similar to a resource provider and allows to connect custom data.
 * But it works on the lower JCR API level. This way legacy code using JCR API can be supported as
 * well. However, implementors of this interface need to implement the full JCR API - which is more
 * complex than the resource provider. Therefore a repository mount should only be used for special
 * cases where legacy code using JCR API is used.
 *
 * <p>The JCR base implementation supports only a single {@code RepositoryMount}. In case of several
 * registrations, the one with the highest service ranking will be used.
 *
 * <p>The {@code RepositoryMount} must implement {@link
 * JackrabbitRepository#login(javax.jcr.Credentials, String, java.util.Map)} in order to login
 * against the custom data provider. It will get access to the JCR session through {@link
 * #PARENT_SESSION_KEY}.
 *
 * <p>A mount may restrict itself to some sessions only, see {@link #appliesTo(Session)}. Sessions it
 * does not apply to are handed out unwrapped and do not pay for the proxy layer.
 */
public interface RepositoryMount extends JackrabbitRepository {
    /**
     * The key of the attribute holding the parent session when {@link
     * JackrabbitRepository#login(javax.jcr.Credentials, String, java.util.Map)} is called.
     */
    String PARENT_SESSION_KEY = "org.apache.sling.jcr.base.RepositoryMount.PARENT_SESSION";

    /**
     * The repository needs to register itself with this property which is a String+ property defining
     * the paths in the JCR tree where the handling of the nodes is delegated to the mounter. The
     * mounter can mount itself at various points in the JCR repository.
     */
    String MOUNT_POINTS_KEY = "org.apache.sling.jcr.base.RepositoryMount.MOUNT_POINTS";

    /**
     * Decides whether a session created by the repository is combined with this mount. It is called once for
     * every login, and for every service session, while this mount is registered, with the session the
     * repository has just created.
     *
     * <p>Returning {@code false} hands out that session unchanged: it does not see the mount and does not
     * go through the proxy layer for its whole lifetime. Sessions derived from a session that is combined
     * with the mount (for example by {@link Session#impersonate(javax.jcr.Credentials)}) stay combined.
     *
     * <p>Implementations must be fast and must not throw; a {@code RuntimeException} is treated as
     * {@code false}. The default applies the mount to every session.
     *
     * @param session the session just created by the underlying repository
     * @return {@code true} to combine the session with this mount
     * @since 0.2.0
     */
    default boolean appliesTo(Session session) {
        return true;
    }
}
