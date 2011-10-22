package org.jboss.arquillian.container.wls.remote_103x;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

class WebLogicJMXLibClassLoader extends URLClassLoader
{
   private static final Logger logger = Logger.getLogger(WebLogicJMXLibClassLoader.class.getName());

   public WebLogicJMXLibClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory)
   {
      super(urls, parent, factory);
   }


   public WebLogicJMXLibClassLoader(URL[] urls, ClassLoader parent)
   {
      super(urls, parent);
   }


   public WebLogicJMXLibClassLoader(URL[] urls)
   {
      super(urls);
   }
   
   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      logger.log(Level.FINE, "Loading class: {0}", name);
      return super.loadClass(name);
   }
}
