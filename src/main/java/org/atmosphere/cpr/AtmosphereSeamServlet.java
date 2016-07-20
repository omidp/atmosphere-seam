package org.atmosphere.cpr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.container.JBossAsyncSupportWithWebSocket;
import org.atmosphere.container.JBossWebCometSupport;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.jboss.seam.servlet.ServletApplicationMap;
import org.jboss.servlet.http.HttpEvent;
import org.jboss.servlet.http.HttpEventServlet;

/**
 * This servlet supports Seam Component in Atmosphere Managedserivce it has been
 * tested on JBoss eap6.4 and seam 2.x
 * 
 * @author Omid Pourhadi
 *
 */
public class AtmosphereSeamServlet extends HttpServlet implements HttpEventServlet
{

    private ServletContext context;

    AtmosphereFramework framework;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        context = config.getServletContext();
        try
        {
            Lifecycle.setupApplication(new ServletApplicationMap(context));
            framework = new AtmosphereFramework(config);
            super.init(config);
        }
        finally
        {
            Lifecycle.cleanupApplication();
        }
    }

    @Override
    public void destroy()
    {
        if (framework != null)
        {
            framework.destroy();
            framework = null;
        }
        Lifecycle.cleanupApplication();
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doHead(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doOptions(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doTrace(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        doPost(req, res);
    }

    /**
     * Delegate the request processing to an instance of
     * {@link org.atmosphere.cpr.AsyncSupport}.
     *
     * @param req
     *            the {@link javax.servlet.http.HttpServletRequest}
     * @param res
     *            the {@link javax.servlet.http.HttpServletResponse}
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse res) throws IOException, ServletException
    {
        new AtmosphereContextualServlet(context, request) {

            @Override
            public void process() throws Exception
            {
                framework.doCometSupport(AtmosphereRequestImpl.wrap(request), AtmosphereResponseImpl.wrap(res));
            }
        }.run();

    }

    public void event(final HttpEvent httpEvent) throws IOException, ServletException
    {
        final HttpServletRequest req = httpEvent.getHttpServletRequest();
        final HttpServletResponse res = httpEvent.getHttpServletResponse();
        req.setAttribute(JBossWebCometSupport.HTTP_EVENT, httpEvent);
        
        try
        {
            ServletLifecycle.beginReinitialization(req, context);
            boolean isWebSocket = req.getHeader("Upgrade") == null ? false : true;
            if (isWebSocket && framework.asyncSupport.getClass().equals(JBossAsyncSupportWithWebSocket.class))
            {
                ((JBossAsyncSupportWithWebSocket) framework.asyncSupport).dispatch(httpEvent);
            }
            else
            {
                framework.doCometSupport(AtmosphereRequestImpl.wrap(req), AtmosphereResponseImpl.wrap(res));
            }
        }
        finally
        {
            ServletLifecycle.endReinitialization();
        }

    }

}
