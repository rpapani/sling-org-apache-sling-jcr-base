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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import java.security.Principal;
import java.util.Iterator;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

public class ProxyUserManager extends ProxyWrapper<UserManager> implements UserManager {
    private final UserManager mount;

    public ProxyUserManager(ProxySession<JackrabbitSession> mountSession, UserManager delegate, UserManager mount) {
        super(mountSession, delegate);
        this.mount = mount;
    }

    public Authorizable getAuthorizable(String id) throws RepositoryException {
        return delegate.getAuthorizable(id);
    }

    public <T extends Authorizable> T getAuthorizable(String id, Class<T> authorizableClass)
            throws AuthorizableTypeException, RepositoryException {
        return delegate.getAuthorizable(id, authorizableClass);
    }

    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        return delegate.getAuthorizable(principal);
    }

    public Authorizable getAuthorizableByPath(String path)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getAuthorizableByPath(path);
    }

    public Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException {
        return delegate.findAuthorizables(relPath, value);
    }

    public Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType)
            throws RepositoryException {
        return delegate.findAuthorizables(relPath, value, searchType);
    }

    public Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException {
        return delegate.findAuthorizables(query);
    }

    // NOTE (ASO cloneless-preview): the create* methods below deliberately do NOT replay the creation
    // onto the `mount` UserManager. Sling's generic ProxyUserManager assumes the mount is a SEPARATE
    // repository backend and dual-writes so both stores get the authorizable. Our PreviewRepositoryMount
    // is NOT a separate backend — its OverlaySession wraps the SAME underlying JCR session as `delegate`
    // (ProxyRepository passes the real session as PARENT_SESSION). Replaying the create on `mount`
    // therefore creates the SAME authorizable a second time on the SAME session, which throws
    // AuthorizableExistsException — surfacing as a false HTTP 409 "Authorizable already exists" for EVERY
    // user/group creation (the audit log shows the user WAS created by the first call, then the replay
    // 409s). Authorizables live under /home, outside the mounted /content subtree, so the mount has no
    // authorizable store of its own to keep in sync. Reads (getAuthorizable/findAuthorizables above) are
    // already delegate-only, so delegating creates too keeps them consistent. Fixes create-user /
    // Create-Site-as-author / product-test CreatePageAsAuthorUserIT.
    public User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException {
        return delegate.createUser(userID, password);
    }

    public User createUser(String userID, String password, Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegate.createUser(userID, password, principal, intermediatePath);
    }

    public User createSystemUser(String userID, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegate.createSystemUser(userID, intermediatePath);
    }

    public Group createGroup(String groupID) throws AuthorizableExistsException, RepositoryException {
        return delegate.createGroup(groupID);
    }

    public Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException {
        return delegate.createGroup(principal);
    }

    public Group createGroup(Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegate.createGroup(principal, intermediatePath);
    }

    public Group createGroup(String groupID, Principal principal, String intermediatePath)
            throws AuthorizableExistsException, RepositoryException {
        return delegate.createGroup(groupID, principal, intermediatePath);
    }

    public boolean isAutoSave() {
        return delegate.isAutoSave();
    }

    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
        delegate.autoSave(enable);
        mount.autoSave(enable);
    }
}
