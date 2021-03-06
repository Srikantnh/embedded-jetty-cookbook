//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.cookbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.AppLifeCycle;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.graph.Node;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;

@SuppressWarnings("Duplicates")
public class DeployWebApps
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        HandlerCollection handlers = new HandlerCollection();

        handlers.setHandlers(new Handler[]{contexts, new DefaultHandler()});

        server.setHandler(handlers);

        Path confFile = Paths.get(System.getProperty("user.dir"), "example.conf");

        ContextAttributeCustomizer contextAttributeCustomizer = new ContextAttributeCustomizer();
        contextAttributeCustomizer.setAttribute("common.conf", confFile);

        DeploymentManager deploymentManager = new DeploymentManager();
        deploymentManager.setContexts(contexts);
        deploymentManager.addLifeCycleBinding(contextAttributeCustomizer);

        String jettyBaseProp = System.getProperty("jetty.base");
        if (jettyBaseProp == null)
        {
            throw new FileNotFoundException("Missing System Property 'jetty.base'");
        }
        Path jettyBase = new File(jettyBaseProp).toPath().toAbsolutePath();

        WebAppProvider webAppProvider = new WebAppProvider();
        webAppProvider.setMonitoredDirName(jettyBase.resolve("webapps").toString());

        deploymentManager.addAppProvider(webAppProvider);
        server.addBean(deploymentManager);

        // Lets dump the server after start.
        // We can look for the deployed contexts, along with an example of the
        // result of ContextAttributesCustomizer in the dump section for "Handler attributes"
        server.setDumpAfterStart(true);
        server.start();
        server.join();
    }

    public static class ContextAttributeCustomizer implements AppLifeCycle.Binding
    {
        public final Map<String, Object> attributes = new HashMap<>();

        public void setAttribute(String name, Object value)
        {
            this.attributes.put(name, value);
        }

        @Override
        public String[] getBindingTargets()
        {
            return new String[]{AppLifeCycle.DEPLOYING};
        }

        @Override
        public void processBinding(Node node, App app) throws Exception
        {
            ContextHandler handler = app.getContextHandler();
            if (handler == null)
            {
                throw new NullPointerException("No Handler created for App: " + app);
            }
            attributes.forEach((name, value) -> handler.setAttribute(name, value));
        }
    }
}
