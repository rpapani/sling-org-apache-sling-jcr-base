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
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

public class ProxyNodeTypeManager implements NodeTypeManager {
    private final NodeTypeManager nodeTypeManager;
    private final NodeTypeManager nodeTypeManager1;

    public ProxyNodeTypeManager(NodeTypeManager nodeTypeManager, NodeTypeManager nodeTypeManager1) {
        this.nodeTypeManager = nodeTypeManager;
        this.nodeTypeManager1 = nodeTypeManager1;
    }

    @Override
    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException {
        return nodeTypeManager.getNodeType(nodeTypeName);
    }

    @Override
    public boolean hasNodeType(String name) throws RepositoryException {
        return nodeTypeManager.hasNodeType(name);
    }

    @Override
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return nodeTypeManager.getAllNodeTypes();
    }

    @Override
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        return nodeTypeManager.getPrimaryNodeTypes();
    }

    @Override
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        return nodeTypeManager.getMixinNodeTypes();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeTypeTemplate();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeTypeTemplate(ntd);
    }

    @Override
    public NodeDefinitionTemplate createNodeDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeDefinitionTemplate();
    }

    @Override
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createPropertyDefinitionTemplate();
    }

    // NOTE (ASO cloneless-preview): register/unregister are delegate-ONLY (nodeTypeManager1 = the mount's
    // manager is intentionally NOT called). Sling's generic proxy assumes the mount is a SEPARATE repository
    // backend and replays the registration on both. Our PreviewRepositoryMount's OverlaySession shares the
    // SAME underlying JCR session as the delegate, so replaying registers the SAME node type a second time on
    // the SAME session -> NodeTypeExistsException (for allowUpdate=false) -> content-package installs and the
    // Create Site wizard (which register node types during content import) fail. Node types are global and
    // already registered by the delegate call; there is no separate mount store to sync. Same shared-session
    // rationale as ProxyUserManager.create* and ProxyNamespaceRegistry.
    @Override
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
                    RepositoryException {
        return nodeTypeManager.registerNodeType(ntd, allowUpdate);
    }

    @Override
    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate)
            throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
                    RepositoryException {
        return nodeTypeManager.registerNodeTypes(ntds, allowUpdate);
    }

    @Override
    public void unregisterNodeType(String name)
            throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        nodeTypeManager.unregisterNodeType(name);
    }

    @Override
    public void unregisterNodeTypes(String[] names)
            throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        nodeTypeManager.unregisterNodeTypes(names);
    }
}
