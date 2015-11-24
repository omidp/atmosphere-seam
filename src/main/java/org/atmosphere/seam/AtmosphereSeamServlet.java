package org.atmosphere.seam;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequestImpl;
import org.atmosphere.cpr.AtmosphereResponseImpl;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.jboss.seam.servlet.ServletApplicationMap;

/**
 * This servlet supports Seam Component in Atmosphere Managedserivce 
 * it has been tested on JBoss eap6.2 and seam 2.3 
 * @author Omid Pourhadi
 *
 */
public class AtmosphereSeamServlet extends HttpServlet
{

    private ServletContext context;

    AtmosphereFramework seamAtmosphereFramework;

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        context = config.getServletContext();
        try
        {
            Lifecycle.setupApplication(new ServletApplicationMap(context));
            seamAtmosphereFramework = new AtmosphereFramework(false, true);
            seamAtmosphereFramework.setUseNativeImplementation(true);
            seamAtmosphereFramework.init(config);
        }
        finally
        {
            Lifecycle.cleanupApplication();
        }
    }

    @Override
    public void destroy()
    {
        seamAtmosphereFramework.destroy();
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
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException
    {
        final HttpServletRequest request = req;
        final HttpServletResponse resp = res;
        new ContextualHttpServletRequest(request) {
            
            @Override
            public void process() throws Exception
            {
                seamAtmosphereFramework.doCometSupport(AtmosphereRequestImpl.wrap(request), AtmosphereResponseImpl.wrap(resp));
            }
        }.run();
    }

}
