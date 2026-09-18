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
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;

public class ProxyPrivilegeManager extends ProxyWrapper<PrivilegeManager> implements PrivilegeManager {
    private final PrivilegeManager mount;

    public ProxyPrivilegeManager(ProxySession<?> mountSession, PrivilegeManager delegate, PrivilegeManager mount) {
        super(mountSession, delegate);
        this.mount = mount;
    }

    // NOTE (ASO cloneless-preview): all methods are delegate-ONLY (the `mount` PrivilegeManager is not
    // called). The mount shares the same underlying session, so it is the SAME privilege registry as
    // `delegate`: combining both in getRegisteredPrivileges returned every privilege TWICE, and replaying
    // registerPrivilege re-registered the SAME privilege on the SAME session -> "privilege already exists"
    // -> content-package installs / custom privilege registration fail. Same shared-session rationale as
    // ProxyUserManager / ProxyNodeTypeManager / ProxyNamespaceRegistry.
    public Privilege[] getRegisteredPrivileges() throws RepositoryException {
        return delegate.getRegisteredPrivileges();
    }

    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        return delegate.getPrivilege(privilegeName);
    }

    public Privilege registerPrivilege(String privilegeName, boolean isAbstract, String[] declaredAggregateNames)
            throws AccessDeniedException, NamespaceException, RepositoryException {
        return delegate.registerPrivilege(privilegeName, isAbstract, declaredAggregateNames);
    }
}
