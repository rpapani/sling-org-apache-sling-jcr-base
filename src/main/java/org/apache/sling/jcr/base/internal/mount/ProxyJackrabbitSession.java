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

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;

import java.util.Set;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;

public class ProxyJackrabbitSession extends ProxySession<JackrabbitSession> implements JackrabbitSession {
    public ProxyJackrabbitSession(
            ProxyRepository repository, JackrabbitSession jcr, Session mount, Set<String> mountPoints) {
        super(repository, jcr, mount, mountPoints);
    }

    @Override
    public Workspace getWorkspace() {
        return new ProxyJackrabbitWorkspace(
                this, (JackrabbitWorkspace) this.jcr.getWorkspace(), (JackrabbitWorkspace) this.mount.getWorkspace());
    }

    public boolean hasPermission(String absPath, String... actions) throws RepositoryException {
        if (isMount(absPath)) {
            return ((JackrabbitSession) mount).hasPermission(absPath, actions);
        }
        return jcr.hasPermission(absPath, actions);
    }

    public PrincipalManager getPrincipalManager()
            throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return jcr.getPrincipalManager();
    }

    public UserManager getUserManager()
            throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return new ProxyUserManager(this, jcr.getUserManager(), ((JackrabbitSession) mount).getUserManager());
    }

    public Item getItemOrNull(String absPath) throws RepositoryException {
        Object item = JackrabbitMethods.onSession(JackrabbitMethods.SESSION_GET_ITEM_OR_NULL, side(absPath), absPath);
        if (item != JackrabbitMethods.UNAVAILABLE) {
            return item == null ? null : wrap((Item) item);
        }
        return super.itemExists(absPath) ? super.getItem(absPath) : null;
    }

    public Property getPropertyOrNull(String absPath) throws RepositoryException {
        Object property =
                JackrabbitMethods.onSession(JackrabbitMethods.SESSION_GET_PROPERTY_OR_NULL, side(absPath), absPath);
        if (property != JackrabbitMethods.UNAVAILABLE) {
            return property == null ? null : wrap((Property) property);
        }
        return super.propertyExists(absPath) ? super.getProperty(absPath) : null;
    }

    public Node getNodeOrNull(String absPath) throws RepositoryException {
        Object node = JackrabbitMethods.onSession(JackrabbitMethods.SESSION_GET_NODE_OR_NULL, side(absPath), absPath);
        if (node != JackrabbitMethods.UNAVAILABLE) {
            return node == null ? null : wrap((Node) node);
        }
        return super.nodeExists(absPath) ? super.getNode(absPath) : null;
    }

    /** The session that serves {@code absPath}: the mount inside a mount point, else the repository. */
    private Session side(String absPath) {
        return isMount(absPath) ? this.mount : this.jcr;
    }
}
