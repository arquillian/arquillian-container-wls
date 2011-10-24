package org.jboss.arquillian.container.wls.remote_103x;

import java.util.Collection;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

/**
 * The Weld-Servlet dependency will be treated as an auxiliary archive.
 * This ensures that the Weld-Servlet JAR will be placed in the EarRoot/lib directory,
 * and not in WEB-INF/lib of the protocol unit.
 * 
 * This avoids the {@link NoClassDefFoundError} and {@link ClassNotFoundException}
 * involving "javax.enterprise.inject.spi.Extension" and possibly others,
 * when starting the Weld Listener.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicWeldAppender implements AuxiliaryArchiveAppender
{

   public Archive<?> createAuxiliaryArchive()
   {
      Collection<JavaArchive> archives = DependencyResolvers.use(MavenDependencyResolver.class)
                        .loadMetadataFromPom("pom.xml")
                        .goOffline()
                        .artifact("org.jboss.weld.servlet:weld-servlet")
                        .resolveAs(JavaArchive.class);
      if(archives.size() == 1)
      {
         JavaArchive[] array = archives.toArray(new JavaArchive[0]);
         return array[0];
      }
      else
      {
         throw new IllegalStateException("Found more than one JavaArchive matching weld-servlet");
      }
   }

}
