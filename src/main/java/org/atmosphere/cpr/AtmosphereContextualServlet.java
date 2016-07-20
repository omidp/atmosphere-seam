package org.atmosphere.cpr;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.contexts.ServletLifecycle;

/**
 * @author Omid Pourhadi
 *
 */
public abstract class AtmosphereContextualServlet
{

    private static ThreadLocal<AtomicInteger> count = new ThreadLocal<AtomicInteger>();

    private ServletContext context;
    private HttpServletRequest req;

    public AtmosphereContextualServlet(ServletContext context, HttpServletRequest req)
    {
        this.context = context;
        this.req = req;
    }
    
    public abstract void process() throws Exception;

    public void run() throws ServletException, IOException
    {
        if (getCounterValue() == 0)
        {
//            Lifecycle.setupApplication(new ServletApplicationMap(context));
            ServletLifecycle.beginReinitialization(req, context);
        }
        
        
        try
        {
            incrementCounterValue();

            process();

            decrementCounterValue();

            // End request only if it is not nested ContextualHttpServletRequest
            if (getCounterValue() == 0)
            {
                ServletLifecycle.endReinitialization();
//                Lifecycle.cleanupApplication();
            }
        }
        catch (IOException ioe)
        {
            removeCounter();
            Lifecycle.endRequest();
            //throw ioe;
        }
        catch (ServletException se)
        {
            removeCounter();
            Lifecycle.endRequest();
            //throw se;
        }
        catch (Exception e)
        {
            removeCounter();
            Lifecycle.endRequest();
            //throw new ServletException(e);
        }
        finally
        {
            //request ended
        }
        
    }

    private int getCounterValue()
    {
        AtomicInteger i = count.get();
        if (i == null || i.intValue() < 0)
        {
            return 0;
        }
        else
        {
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
    }

    /*
     * Decrements ThreadLocal counter value
     */
    private void decrementCounterValue()
    {
        AtomicInteger i = count.get();
        if (i == null)
        {
            // we should never get here...
            throw new IllegalStateException("Counter for nested ContextualHttpServletRequest was removed before it should be!");
        }
        if (i.intValue() > 0)
        {
            i.decrementAndGet();
        }
    }

    /*
     * Removes ThreadLocal counter
     */
    private void removeCounter()
    {
        count.remove();
    }
}
