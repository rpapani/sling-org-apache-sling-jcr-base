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
package org.apache.sling.jcr.base;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.jcr.base.internal.mount.ProxyNode;
import org.apache.sling.jcr.base.internal.mount.ProxySession;
import org.apache.sling.jcr.base.spi.RepositoryMount;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A mount decides per session whether it applies ({@link RepositoryMount#appliesTo(Session)}), and relative
 * navigation through the proxy resolves the same items as absolute lookups.
 */
public class RepositoryMountAppliesToTest {
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private Repository rootRepository;
    private Repository mountRepository;
    private MockSlingRepositoryManager manager;
    private final AtomicBoolean applies = new AtomicBoolean(true);
    private final AtomicInteger asked = new AtomicInteger();

    @Before
    public void setup() throws RepositoryException {
        rootRepository = MockJcr.newRepository();
        mountRepository = MockJcr.newRepository();
        manager = new MockSlingRepositoryManager(rootRepository);

        Session root = rootRepository.login();
        Node r = root.getRootNode().addNode("root", NodeType.NT_UNSTRUCTURED);
        r.addNode("a", NodeType.NT_UNSTRUCTURED)
                .addNode("b", NodeType.NT_UNSTRUCTURED)
                .setProperty("p", "root-b");
        root.logout();

        Session mount = mountRepository.login();
        Node m = mount.getRootNode().addNode("mount", NodeType.NT_UNSTRUCTURED);
        m.addNode("a", NodeType.NT_UNSTRUCTURED)
                .addNode("b", NodeType.NT_UNSTRUCTURED)
                .setProperty("p", "mount-b");
        mount.logout();
    }

    private Repository startWithMount() {
        Assert.assertTrue(manager.start(context.bundleContext(), "ws", false));
        Properties props = new Properties();
        props.put(RepositoryMount.MOUNT_POINTS_KEY, "/mount");
        context.bundleContext().registerService(RepositoryMount.class.getName(), new TestMount(), (Dictionary) props);
        return context.getService(Repository.class);
    }

    @Test
    public void sessionsTheMountDeclinesAreNotProxied() throws RepositoryException {
        Repository repository = startWithMount();

        applies.set(false);
        Session plain = repository.login();
        Assert.assertFalse(plain instanceof ProxySession);
        Assert.assertFalse(plain.nodeExists("/mount"));
        Assert.assertEquals("root-b", plain.getProperty("/root/a/b/p").getString());
        Assert.assertEquals(1, asked.get());
        plain.logout();

        applies.set(true);
        Session mounted = repository.login();
        Assert.assertTrue(mounted instanceof ProxySession);
        Assert.assertEquals("mount-b", mounted.getProperty("/mount/a/b/p").getString());
        Assert.assertEquals(2, asked.get());
        mounted.logout();
    }

    @Test
    public void relativeNavigationMatchesAbsoluteLookups() throws RepositoryException {
        Repository repository = startWithMount();
        Session session = repository.login();
        Assert.assertTrue(session instanceof ProxySession);

        for (String top : new String[] {"/root", "/mount"}) {
            Node start = session.getNode(top);
            Node b = start.getNode("a").getNode("b");
            Assert.assertEquals(top + "/a/b", b.getPath());
            Assert.assertEquals(
                    session.getProperty(top + "/a/b/p").getString(),
                    b.getProperty("p").getString());
            Assert.assertTrue(start.hasNode("a/b"));
            Assert.assertTrue(start.hasProperty("a/b/p"));
            Assert.assertFalse(start.hasNode("a/missing"));
            Assert.assertEquals(top + "/a/b", start.getNode("a/b").getPath());
            Assert.assertEquals(top + "/a", b.getParent().getPath());
            Assert.assertEquals(top, b.getParent().getParent().getPath());
            ProxyNode proxy = (ProxyNode) start;
            Assert.assertEquals(top + "/a/b", ((Node) proxy.getNodeOrNull("a/b")).getPath());
            Assert.assertNull(proxy.getNodeOrNull("a/missing"));
            Assert.assertEquals(
                    session.getProperty(top + "/a/b/p").getString(),
                    proxy.getPropertyOrNull("a/b/p").getString());
            Assert.assertNull(proxy.getPropertyOrNull("a/b/missing"));
            // paths the fast path must leave to the session
            Assert.assertEquals(top, b.getNode("../..").getPath());
            Assert.assertTrue(b.hasNode(".."));
        }

        // crossing into the mount from the repository side still lands in the mount
        Node rootNode = session.getRootNode();
        Assert.assertEquals(
                "mount-b", rootNode.getNode("mount/a/b").getProperty("p").getString());
        Assert.assertTrue(rootNode.hasProperty("mount/a/b/p"));
        Assert.assertFalse(rootNode.hasNode("mount/a/b/c"));
        // and leaving the mount upwards lands back in the repository
        Assert.assertEquals("/", session.getNode("/mount").getParent().getPath());
        session.logout();
    }

    /** A mount backed by a separate mock repository whose decision is controlled by the test. */
    private final class TestMount implements RepositoryMount {
        @Override
        public boolean appliesTo(Session session) {
            asked.incrementAndGet();
            return applies.get();
        }

        @Override
        public Session login(Credentials credentials, String workspaceName, Map<String, Object> attributes)
                throws RepositoryException {
            return mountRepository.login(credentials, workspaceName);
        }

        @Override
        public void shutdown() {}

        @Override
        public Session login(Credentials credentials, String workspaceName) throws RepositoryException {
            return mountRepository.login(credentials, workspaceName);
        }

        @Override
        public Session login(Credentials credentials) throws RepositoryException {
            return mountRepository.login(credentials);
        }

        @Override
        public Session login(String workspaceName) throws RepositoryException {
            return mountRepository.login(workspaceName);
        }

        @Override
        public Session login() throws RepositoryException {
            return mountRepository.login();
        }

        @Override
        public String[] getDescriptorKeys() {
            return mountRepository.getDescriptorKeys();
        }

        @Override
        public boolean isStandardDescriptor(String key) {
            return mountRepository.isStandardDescriptor(key);
        }

        @Override
        public boolean isSingleValueDescriptor(String key) {
            return mountRepository.isSingleValueDescriptor(key);
        }

        @Override
        public Value getDescriptorValue(String key) {
            return mountRepository.getDescriptorValue(key);
        }

        @Override
        public Value[] getDescriptorValues(String key) {
            return mountRepository.getDescriptorValues(key);
        }

        @Override
        public String getDescriptor(String key) {
            return mountRepository.getDescriptor(key);
        }
    }
}
