package org.atmosphere.seam;

import java.lang.reflect.Field;

import org.atmosphere.annotation.Processor;
import org.atmosphere.config.AtmosphereAnnotation;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.Broadcaster;
import org.jboss.seam.annotations.In;

/**
 * @author Omid Pourhadi
 *
 */
@AtmosphereAnnotation(SeamManagedService.class)
public class SeamManagedServiceProcessor implements Processor<Object>
{

    public void handle(AtmosphereFramework framework, Class<Object> annotatedClass)
    {
        Field[] fields = annotatedClass.getDeclaredFields();
        if (fields != null && fields.length > 0)
        {
            for (int i = 0; i < fields.length; i++)
            {
                Field f = fields[i];
                f.setAccessible(true);
                if (f.isAnnotationPresent(In.class))
                {
                    if (f.getType().equals(Broadcaster.class))
                    {
                        // BroadcasterFactory b = (BroadcasterFactory)
                        // Contexts.getApplicationContext().get("BroadcasterFactory");
                        // b.get();
                        // TODO : set value
                    }

                }
            }
        }
    }

}
