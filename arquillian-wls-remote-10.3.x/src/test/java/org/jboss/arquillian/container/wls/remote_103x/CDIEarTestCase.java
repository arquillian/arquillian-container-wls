package org.jboss.arquillian.container.wls.remote_103x;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CDIEarTestCase {

    @Inject
    private SimpleBean foo;

    /* Ok, this deployment fails. The deployed EAR file contains foo.jar in the root of the EAR
     * and not in WEB-INF/lib of the WAR. Probably a bug in ARQ or SW.
     *  
     * @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "foo.jar").addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClasses(SimpleBean.class);
    }*/
    
    @Deployment
    public static EnterpriseArchive deploy() {
      WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "foo.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClasses(SimpleBean.class, MyServlet.class)
            //The deployed EAR does not contain the test class when we build an EnterpriseArchive, and must be manually added.
            .addClass(CDIEarTestCase.class)
            .setWebXML("in-container-web.xml");
      
      EnterpriseArchive enterpriseArchive = ShrinkWrap.create(EnterpriseArchive.class, "foo.ear")
            .addAsModule(webArchive)
            // Add Weld-Servlet as an enterprise lib, since Arquillian-CDI enricher is also added into EarRoot/lib.
            // If Weld-Servlet is placed in WEB-INF\lib, Weld will not be able to load the CDI enricher service.
            .addAsLibraries(DependencyResolvers.use(MavenDependencyResolver.class)
                  .loadMetadataFromPom("pom.xml")
                  .goOffline()
                  .artifact("org.jboss.weld.servlet:weld-servlet")
                  .resolveAs(GenericArchive.class));
      
      return enterpriseArchive;
    }

   @Test
   public void test()
   {
      Assert.assertNotNull(foo);
   }

}