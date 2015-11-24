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
        U newInstance = (U) Contexts.getApplicationContext().get(classType.getName());
        if (newInstance != null)
            return newInstance;
        newInstance = defaultType.newInstance();
        Contexts.getApplicationContext().set(classType.getName(), newInstance);
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
