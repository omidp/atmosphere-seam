package org.atmosphere.seam;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereObjectFactory;
import org.jboss.seam.contexts.Contexts;

/**
 * @author Omid Pourhadi
 *
 */
public class SeamObjectFactory implements AtmosphereObjectFactory<Object>
{

    AtmosphereConfig config;

    public void configure(AtmosphereConfig config)
    {
        this.config = config;
    }

    public <T, U extends T> T newClassInstance(Class<T> classType, Class<U> defaultType) throws InstantiationException,
            IllegalAccessException
    {
        if (Contexts.isApplicationContextActive() == false)
            throw new IllegalArgumentException("AtmosphereSeamServlet is not configured");
        Contexts.getApplicationContext().remove(classType.getSimpleName());
        U newInstance = defaultType.newInstance();
        Contexts.getApplicationContext().set(classType.getSimpleName(), newInstance);
        return newInstance;
    }

    public AtmosphereObjectFactory allowInjectionOf(Object z)
    {
        return this;
    }

    @Override
    public String toString()
    {
        return "Seam ObjectFactory";
    }

}
