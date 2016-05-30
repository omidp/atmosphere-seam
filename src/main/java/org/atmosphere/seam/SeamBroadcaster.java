package org.atmosphere.seam;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFuture;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.Deliver;
import org.jboss.seam.contexts.Contexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Omid Pourhadi
 *
 */
public class SeamBroadcaster extends DefaultBroadcaster
{

    private static final Logger logger = LoggerFactory.getLogger(SeamBroadcaster.class);

    private static final String REDIS_AUTH = SeamBroadcaster.class.getName() + ".authorization";
    private static final String REDIS_SERVER = SeamBroadcaster.class.getName() + ".server";
    private static final String REDIS_SHARED_POOL = SeamBroadcaster.class.getName() + ".sharedPool";

    private Jedis jedisSubscriber;
    private Jedis jedisPublisher;
    private Callback callback;

    private String authToken = null;

    private boolean sharedPool = false;
    private JedisPool jedisPool;
    private AtmosphereConfig config;
    private URI uri;
    private final AtomicBoolean destroyed = new AtomicBoolean();

    @Override
    public Broadcaster initialize(String id, URI uri, AtmosphereConfig config)
    {
        this.config = config;
        this.uri = URI.create("http://localhost:6379");
        super.initialize(id, uri, config);
        this.callback = new Callback() {

            public String getID()
            {
                return SeamBroadcaster.this.getID();
            }

            public void broadcastReceivedMessage(String message)
            {
                SeamBroadcaster.this.broadcastReceivedMessage(message);

            }
        };
        setUp();
        return this;
    }

    public synchronized void setUp()
    {
        if (config.getServletConfig().getInitParameter(REDIS_AUTH) != null)
        {
            authToken = config.getServletConfig().getInitParameter(REDIS_AUTH);
        }

        if (config.getServletConfig().getInitParameter(REDIS_SERVER) != null)
        {
            uri = URI.create(config.getServletConfig().getInitParameter(REDIS_SERVER));
        }
        else if (uri == null)
        {
            throw new NullPointerException("uri cannot be null");
        }

        if (config.getServletConfig().getInitParameter(REDIS_SHARED_POOL) != null)
        {
            sharedPool = Boolean.parseBoolean(config.getServletConfig().getInitParameter(REDIS_SHARED_POOL));
        }

        logger.info("{} shared connection pool {}", getClass().getName(), sharedPool);

        if (sharedPool)
        {
            if (config.properties().get(REDIS_SHARED_POOL) != null)
            {
                jedisPool = (JedisPool) config.properties().get(REDIS_SHARED_POOL);
            }

            // setup is synchronized, no need to sync here as well.
            if (jedisPool == null)
            {
                GenericObjectPool.Config gConfig = new GenericObjectPool.Config();
                gConfig.testOnBorrow = true;
                gConfig.testWhileIdle = true;

                jedisPool = new JedisPool(gConfig, uri.getHost(), uri.getPort());

                config.properties().put(REDIS_SHARED_POOL, jedisPool);
            }
            else
            {
                disconnectSubscriber();
            }
        }

        // We use the pool only for publishing
        jedisSubscriber = new Jedis(uri.getHost(), uri.getPort());
        try
        {
            jedisSubscriber.connect();
            auth(jedisSubscriber);
        }
        catch (JedisException e)
        {
            logger.error("failed to connect subscriber", e);
            disconnectSubscriber();
        }

        jedisPublisher = sharedPool ? null : new Jedis(uri.getHost(), uri.getPort());
        if (!sharedPool)
        {
            try
            {
                jedisPublisher.connect();
                auth(jedisPublisher);
            }
            catch (JedisException e)
            {
                logger.error("failed to connect publisher", e);
                disconnectPublisher();
            }
        }
    }

    public synchronized void setID(String id)
    {
        disconnectPublisher();
        disconnectSubscriber();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
        if (!sharedPool)
        {
            Object lockingObject = getLockingObject();
            synchronized (lockingObject)
            {
                try
                {
                    disconnectPublisher();
                    disconnectSubscriber();
                    if (jedisPool != null)
                    {
                        jedisPool.destroy();
                    }
                }
                catch (Throwable t)
                {
                    logger.warn("Jedis error on close", t);
                }
                finally
                {
                    config.properties().put(REDIS_SHARED_POOL, null);
                }
            }
        }
    }

    private void auth(Jedis jedis)
    {
        if (authToken != null)
        {
            jedis.auth(authToken);
        }
    }

    private void disconnectSubscriber()
    {
        if (jedisSubscriber == null)
            return;

        synchronized (jedisSubscriber)
        {
            try
            {
                jedisSubscriber.disconnect();
            }
            catch (JedisException e)
            {
                logger.error("failed to disconnect subscriber", e);
            }
        }
    }

    private void disconnectPublisher()
    {
        if (jedisPublisher == null)
            return;

        synchronized (jedisPublisher)
        {
            try
            {
                jedisPublisher.disconnect();
            }
            catch (JedisException e)
            {
                logger.error("failed to disconnect publisher", e);
            }
        }
    }

    private Object getLockingObject()
    {
        return sharedPool ? jedisPool : jedisPublisher;
    }

    /**
     * Implement this method to broadcast message received from an external
     * source like JGroups, Redis, etc.
     */
    public void incomingBroadcast()
    {
        // there is no Seam context here
        jedisSubscriber.subscribe(new JedisPubSub() {

            public void onMessage(String channel, String message)
            {
                callback.broadcastReceivedMessage(message);
            }

            public void onSubscribe(String channel, int subscribedChannels)
            {
                logger.debug("onSubscribe: {}", channel);
            }

            public void onUnsubscribe(String channel, int subscribedChannels)
            {
                logger.debug("onUnsubscribe: {}", channel);
            }

            public void onPSubscribe(String pattern, int subscribedChannels)
            {
                logger.debug("onPSubscribe: {}", pattern);
            }

            public void onPUnsubscribe(String pattern, int subscribedChannels)
            {
                logger.debug("onPUnsubscribe: {}", pattern);
            }

            public void onPMessage(String pattern, String channel, String message)
            {
                logger.debug("onPMessage: pattern: {}, channel: {}, message: {}", new Object[] { pattern, channel, message });
            }
        }, callback.getID());
    }

    /**
     * Implement this method to broadcast message to external source like
     * JGroups, Redis, etc.
     *
     * @param message
     *            outgoing message
     */
    public void outgoingBroadcast(Object message)
    {
        // there is Seam context
        String contents = message.toString();

        Object lockingObject = getLockingObject();
        synchronized (lockingObject)
        {
            if (destroyed.get())
            {
                logger.debug("JedisPool closed. Re-opening");
                setID(callback.getID());
            }

            if (sharedPool)
            {
                for (int i = 0; i < 10; ++i)
                {
                    boolean valid = true;
                    Jedis jedis = jedisPool.getResource();

                    try
                    {
                        auth(jedis);
                        jedis.publish(callback.getID(), contents);
                    }
                    catch (JedisException e)
                    {
                        valid = false;
                        logger.warn("outgoingBroadcast exception", e);
                    }
                    finally
                    {
                        if (valid)
                        {
                            jedisPool.returnResource(jedis);
                        }
                        else
                        {
                            jedisPool.returnBrokenResource(jedis);
                        }
                    }

                    if (valid)
                    {
                        break;
                    }
                }
            }
            else
            {
                try
                {
                    jedisPublisher.publish(callback.getID(), contents);
                }
                catch (JedisException e)
                {
                    logger.warn("outgoingBroadcast exception", e);
                }
            }
        }
    }

    public boolean isShared()
    {
        return sharedPool;
    }

    public String getAuth()
    {
        return authToken;
    }

    public void setAuth(String auth)
    {
        authToken = auth;
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

    protected void broadcastReceivedMessage(Object message)
    {
        try
        {
            Object newMsg = filter(message);
            // if newSgw == null, that means the message has been filtered.
            if (newMsg != null)
            {
                push(new Deliver(newMsg, new BroadcasterFuture<Object>(newMsg), message));
            }
        }
        catch (Throwable t)
        {
            logger.error("failed to push message: " + message, t);
        }
    }

    public static interface Callback
    {

        String getID();

        void broadcastReceivedMessage(String message);

    }

}
