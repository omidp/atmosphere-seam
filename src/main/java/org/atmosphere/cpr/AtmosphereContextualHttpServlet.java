package org.atmosphere.cpr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.jboss.seam.servlet.ServletRequestSessionMap;
import org.jboss.seam.web.ServletContexts;

/**
 * prevents two hurdles 
 * Cannot create a session after the response has been committed
 * The Servlet did not read all available bytes during the processing of the read event
 * copied from {@link ContextualHttpServletRequest} with a little modification
 * @author Omid Pourhadi
 * 
 */
public abstract class AtmosphereContextualHttpServlet
{

    
    private static final LogProvider log = Logging.getLogProvider(AtmosphereContextualHttpServlet.class);

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private static ThreadLocal<AtomicInteger> count = new ThreadLocal<AtomicInteger>();

    public AtmosphereContextualHttpServlet(HttpServletRequest request)
    {
        this.request = request;
        this.response = null;
    }

    public AtmosphereContextualHttpServlet(HttpServletRequest request, HttpServletResponse response)
    {
        this.request = request;
        this.response = response;
    }

    public abstract void process() throws Exception;

    public void run() throws ServletException, IOException
    {
        log.debug("beginning request");

        // Force creation of the session
        if (response == null)
        {
            if (request.getSession(false) == null)
            {
                request.getSession(true);
            }
        }
        else
        {
            // Cannot create a session after the response has been committed
            if (response.isCommitted() == false)
            {
                if (request.getSession(false) == null)
                {

                    request.getSession(true);
                }
            }
        }

        // Begin request and Seam life cycle only if it is not nested
        // ContextualHttpServletRequest
        if (getCounterValue() == 0)
        {
            ServletLifecycle.beginRequest(request);
            ServletContexts.instance().setRequest(request);
            restoreConversationId();
            Manager.instance().restoreConversation();
            ServletLifecycle.resumeConversation(request);
            handleConversationPropagation();
        }

        try
        {
            incrementCounterValue();

            process();

            decrementCounterValue();

            // End request only if it is not nested ContextualHttpServletRequest
            if (getCounterValue() == 0)
            {
                // TODO: conversation timeout
                if(response == null)
                {
                    Manager.instance().endRequest(new ServletRequestSessionMap(request));
                    ServletLifecycle.endRequest(request);
                }
                else
                {
                    if (response.isCommitted() == false)
                    {
                        Manager.instance().endRequest(new ServletRequestSessionMap(request));
                        ServletLifecycle.endRequest(request);
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            removeCounter();
            Lifecycle.endRequest();
            log.debug("ended request due to exception");
            throw ioe;
        }
        catch (ServletException se)
        {
            removeCounter();
            Lifecycle.endRequest();
            log.debug("ended request due to exception");
            throw se;
        }
        catch (Exception e)
        {
            removeCounter();
            Lifecycle.endRequest();
            log.debug("ended request due to exception");
            throw new ServletException(e);
        }
        finally
        {
            log.debug("ended request");
        }
    }

    protected void handleConversationPropagation()
    {
        Manager.instance().handleConversationPropagation(request.getParameterMap());
    }

    protected void restoreConversationId()
    {
        ConversationPropagation.instance().restoreConversationId(request.getParameterMap());
    }

    /*
     * Getter for ThreadLocal counter value
     */
    private int getCounterValue()
    {
        AtomicInteger i = count.get();
        if (i == null || i.intValue() < 0)
        {
            log.trace("Getting 0");
            return 0;
        }
        else
        {
            log.trace("Getting " + i.intValue());
            return i.intValue();
        }
    }

    /*
     * Increments ThreadLocal counter value
     */
    private void incrementCounterValue()
    {
        AtomicInteger i = count.get();
        if (i == null || i.intValue() < 0)
        {
            i = new AtomicInteger(0);
            count.set(i);
        }
        i.incrementAndGet();
        log.trace("Incrementing to " + count.get());
    }

    /*
     * Decrements ThreadLocal counter value
     */
    private void decrementCounterValue()
    {
        AtomicInteger i = count.get();
        if (i == null)
        {
            log.trace("OOps, something removed counter befor end of request!");
            // we should never get here...
            throw new IllegalStateException("Counter for nested ContextualHttpServletRequest was removed before it should be!");
        }
        if (i.intValue() > 0)
        {
            i.decrementAndGet();
            log.trace("Decrementing to " + count.get());
        }
    }

    /*
     * Removes ThreadLocal counter
     */
    private void removeCounter()
    {
        log.trace("Removing ThreadLocal counter");
        count.remove();
    }
    
}
