package org.atmosphere.seam;

import java.util.concurrent.Future;

import org.atmosphere.cpr.BroadcasterFuture;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.jboss.seam.contexts.Contexts;

/**
 * @author Omid Pourhadi
 *
 */
public class SeamBroadcaster extends DefaultBroadcaster
{

    /**
     * Implement this method to broadcast message received from an external source like JGroups, Redis, etc.
     */
    public void incomingBroadcast()
    {
        System.out.println("INCOMING ************************************");
        System.out.println(Contexts.isApplicationContextActive());
    }

    /**
     * Implement this method to broadcast message to external source like JGroups, Redis, etc.
     *
     * @param message outgoing message
     */
    public void outgoingBroadcast(Object message)
    {
        System.out.println("OUTGOING ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println(Contexts.isApplicationContextActive());
    }

    @Override
    protected Runnable getBroadcastHandler()
    {
        return new Runnable() {
            public void run()
            {
                try
                {
                    incomingBroadcast();
                }
                catch (Throwable t)
                {
                    destroy();
                    return;
                }
            }
        };
    }

    @Override
    public Future<Object> broadcast(Object msg)
    {
        return b(msg);
    }

    protected Future<Object> b(Object msg)
    {
        if (destroyed.get())
        {
            return null;
        }

        start();

        BroadcasterFuture<Object> f = new BroadcasterFuture<Object>(msg);
        try
        {
            outgoingBroadcast(msg);
        }
        finally
        {
            futureDone(f);
        }
        return f;
    }

}
