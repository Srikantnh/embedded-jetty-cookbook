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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.cookbook.servlets.HelloServlet;
import org.eclipse.jetty.security.ConstraintAware;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;

@SuppressWarnings("Duplicates")
public class ServletTransportGuaranteeExample
{
    public static void main(String[] args) throws Exception
    {
        Server server = new Server();
        int httpPort = 8080;
        int httpsPort = 8443;
        
        // Setup HTTP Connector
        HttpConfiguration httpConf = new HttpConfiguration();
        httpConf.setSecurePort(httpsPort);
        httpConf.setSecureScheme("https");
        
        // Establish the HTTP ServerConnector
        ServerConnector httpConnector = new ServerConnector(server,
                new HttpConnectionFactory(httpConf));
        httpConnector.setPort(httpPort);
        server.addConnector(httpConnector);
        
        // Setup SSL
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreResource(findKeyStore());
        sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
        sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
        
        // Setup HTTPS Configuration
        HttpConfiguration httpsConf = new HttpConfiguration();
        httpsConf.setSecurePort(httpsPort);
        httpsConf.setSecureScheme("https");
        httpsConf.addCustomizer(new SecureRequestCustomizer()); // adds ssl info to request object

        // Establish the HTTPS ServerConnector
        ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,"http/1.1"),
                new HttpConnectionFactory(httpsConf));
        httpsConnector.setPort(httpsPort);
        
        server.addConnector(httpsConnector);

        // Add a Handler for requests
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SECURITY);
        context.setContextPath("/");
        
        // Setup security constraint
        SecurityHandler security = context.getSecurityHandler();
        if (security instanceof ConstraintAware)
        {
            ConstraintAware constraint = (ConstraintAware)security;
            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec("/*");
            Constraint dc = new Constraint();
            dc.setDataConstraint(Constraint.DC_CONFIDENTIAL);
            mapping.setConstraint(dc);
            constraint.addConstraintMapping(mapping);
        }
        else
        {
            throw new RuntimeException("Not a ConstraintAware SecurityHandler: " + security);
        }
        
        // Add servlet to produce output
        ServletHolder helloHolder = context.addServlet(HelloServlet.class,"/*");
        helloHolder.setInitParameter("message","Hello Secure Servlet World");
        
        server.setHandler(context);

        server.start();
        server.join();
    }

    private static Resource findKeyStore() throws URISyntaxException, MalformedURLException
    {
        ClassLoader cl = ServletTransportGuaranteeExample.class.getClassLoader();
        String keystoreResource = "ssl/keystore";
        URL f = cl.getResource(keystoreResource);
        if (f == null)
        {
            throw new RuntimeException("Unable to find " + keystoreResource);
        }

        return Resource.newResource(f.toURI());
    }
}
