package nxt.http;

import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.rewrite.handler.HeaderPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class API {

    private static final int TESTNET_API_PORT = 6876;

    static final Set<String> allowedBotHosts;

    private static final Server apiServer;

    static {
        List<String> allowedBotHostsList = Nxt.getStringListProperty("nxt.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            allowedBotHosts = Collections.unmodifiableSet(new HashSet<>(allowedBotHostsList));
        } else {
            allowedBotHosts = null;
        }

        boolean enableAPIServer = Nxt.getBooleanProperty("nxt.enableAPIServer");
        if (enableAPIServer) {
            final int port = Constants.isTestnet ? TESTNET_API_PORT : Nxt.getIntProperty("nxt.apiServerPort");
            final String host = Nxt.getStringProperty("nxt.apiServerHost");
            apiServer = new Server();
            ServerConnector connector;
            ConstraintSecurityHandler security = null;
            HandlerList apiHandlers = new HandlerList();

            boolean enableSSL = Nxt.getBooleanProperty("nxt.apiSSL");
            if (enableSSL) {
                Logger.logMessage("Using SSL (https) for the API server");
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSecureScheme("https");
                https_config.setSecurePort(port);
                https_config.addCustomizer(new SecureRequestCustomizer());
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setTrustAll(true);
                sslContextFactory.setNeedClientAuth(false);
                sslContextFactory.setWantClientAuth(false);
                sslContextFactory.setValidateCerts(false);
                sslContextFactory.setRenegotiationAllowed(false);
                sslContextFactory.setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
                sslContextFactory.setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
                sslContextFactory.setIncludeCipherSuites("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                                                         "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                                                         "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
                                                         "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA");
                sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                
                connector = new ServerConnector(apiServer,
                                                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                                                new HttpConnectionFactory(https_config));
                
                boolean enableRedirectionToSSL = Nxt.getBooleanProperty("nxt.apiRedirectToSSL");
                if(enableRedirectionToSSL){
                    int nonSSLSourcePort = Nxt.getIntProperty("nxt.apiRedirectToSSLSourcePort");
                    HttpConfiguration http_config = new HttpConfiguration();
                    http_config.addCustomizer(new SecureRequestCustomizer());
                    http_config.setSecureScheme("https");
                    http_config.setSecurePort(port);
                    
                    ServerConnector httpConnector = new ServerConnector(apiServer);
                    httpConnector.addConnectionFactory(new HttpConnectionFactory(http_config));
                    httpConnector.setPort(nonSSLSourcePort);
                    httpConnector.setHost(host);
                    httpConnector.setReuseAddress(true);
                    
                    Constraint constraint = new Constraint();
                    constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
                    
                    ConstraintMapping mapping = new ConstraintMapping();
                    mapping.setPathSpec( "/*" );
                    mapping.setConstraint( constraint );
                    
                    security = new ConstraintSecurityHandler();
                    security.addConstraintMapping(mapping);
                    apiServer.addConnector(httpConnector);
                }
                
                boolean enableStrictTransportSecurity = Nxt.getBooleanProperty("nxt.enableStrictTransportSecurity");
                
                if(enableStrictTransportSecurity){
                    HeaderPatternRule headerPatternRule = new HeaderPatternRule();
                    headerPatternRule.setName("Strict-Transport-Security");
                    headerPatternRule.setValue("max-age=15768000; includeSubDomains");
                    headerPatternRule.setPattern("*");
                    
                    RewriteHandler rewrite = new RewriteHandler();
                    rewrite.addRule(headerPatternRule);
                    
                    apiHandlers.addHandler(rewrite);
                }
                
            } else {
                connector = new ServerConnector(apiServer);
            }

            connector.setPort(port);
            connector.setHost(host);
            connector.setIdleTimeout(Nxt.getIntProperty("nxt.apiServerIdleTimeout"));
            connector.setReuseAddress(true);
            apiServer.addConnector(connector);

            ServletContextHandler apiHandler = new ServletContextHandler();
            String apiResourceBase = Nxt.getStringProperty("nxt.apiResourceBase");
            if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                apiHandler.addServlet(defaultServletHolder, "/*");
                apiHandler.setWelcomeFiles(new String[]{"index.html"});
            }

            String javadocResourceBase = Nxt.getStringProperty("nxt.javadocResourceBase");
            if (javadocResourceBase != null) {
                ContextHandler contextHandler = new ContextHandler("/doc");
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            apiHandler.addServlet(APIServlet.class, "/burst");
            if (Nxt.getBooleanProperty("nxt.enableAPIServerGZIPFilter")) {
                FilterHolder gzipFilterHolder = apiHandler.addFilter(GzipFilter.class, "/burst", null);
                gzipFilterHolder.setInitParameter("methods", "GET,POST");
                gzipFilterHolder.setAsyncSupported(true);
            }

            apiHandler.addServlet(APITestServlet.class, "/test");

            if (Nxt.getBooleanProperty("nxt.apiServerCORS")) {
                FilterHolder filterHolder = apiHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            if(security != null) {
                security.setHandler(apiHandler);
                apiHandlers.addHandler(security);
            }
            else {
                apiHandlers.addHandler(apiHandler);
            }
            apiHandlers.addHandler(new DefaultHandler());

            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);

            ThreadPool.runBeforeStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        apiServer.start();
                        Logger.logMessage("Started API server at " + host + ":" + port);
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start API server", e);
                        throw new RuntimeException(e.toString(), e);
                    }

                }
            }, true);

        } else {
            apiServer = null;
            Logger.logMessage("API server not enabled");
        }

    }

    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
            } catch (Exception e) {
                Logger.logDebugMessage("Failed to stop API server", e);
            }
        }
    }

    private API() {} // never

}
