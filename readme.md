# Atmosphere-Seam Integration

This module is based on atmosphere-native-runtime project and is only tested on Jboss eap 6.4 and JBoss Seam 2.3
  
##Configuration

+ web.xml

```
<servlet>
		<description>AtmosphereServlet</description>
		<servlet-name>AtmosphereServlet</servlet-name>
		<servlet-class>org.atmosphere.seam.AtmosphereSeamServlet</servlet-class>
		<init-param>
			<param-name>org.atmosphere.cpr.packages</param-name>
			<param-value>ir.seam.ui.websocket, org.atmosphere.seam</param-value>
		</init-param>
		<init-param>
			<param-name>org.atmosphere.cpr.objectFactory</param-name>
			<param-value>org.atmosphere.seam.SeamObjectFactory</param-value>
		</init-param>		
		<load-on-startup>0</load-on-startup>
		<async-supported>true</async-supported>
	</servlet>
	<servlet-mapping>
		<servlet-name>AtmosphereServlet</servlet-name>
		<url-pattern>/echo/*</url-pattern>
	</servlet-mapping>
```

+ add dependency

```
		<dependency>
			<groupId>org.atmosphere.seam</groupId>
			<artifactId>atmosphere-seam</artifactId>
			<version>2.4.0</version>
		</dependency>
```

+ Your ManagedService

```
@SeamManagedService
@ManagedService(path = "/echo/{uname}")
public class SocketManagedService
{

    @In // use @Inject instead still not working
    Broadcaster broadcaster;
    
    @Ready
    public void ready(AtmosphereResource r)
    {
        if(Identity.instance().isLoggedIn())
        {
            String uname = Identity.instance().getCredentials().getUsername();
            BroadcasterFactory broadcasterFactory = r.getAtmosphereConfig().getBroadcasterFactory();
            Broadcaster lookup = broadcasterFactory.lookup("/echo/"+uname, true);
        }
    }
    
    @Disconnect
    public void onDisconnect(AtmosphereResourceEvent event)
    {
        
    }
    
    @org.atmosphere.config.service.Message
    public String message(String msg)
    {
        return msg;
    }
    
}
```


