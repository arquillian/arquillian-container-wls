package org.jboss.arquillian.container.wls.remote_103x;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CDIJarTestCase {

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
    
    /*
     * This fails as well. WLS packages all classes in WEB-INF\classes into a JAR (_wl_cls_gen.jar) upon deployment.
     * And Weld scans WEB-INF\classes when it find WEB-INF\beans.xml, resulting in chaotic behavior, as _wl_cls_gen.jar
     * does not contain a beans.xml file.
     * 
     * @Deployment
    public static WebArchive deploy() {
      return ShrinkWrap.create(WebArchive.class, "foo.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClasses(SimpleBean.class, MyServlet.class)
            .setWebXML("in-container-web.xml")
            .addAsLibraries(
                  DependencyResolvers.use(MavenDependencyResolver.class)
                        .loadMetadataFromPom("pom.xml")
                        .goOffline()
                        .artifact("org.jboss.weld.servlet:weld-servlet")
                        .resolveAs(GenericArchive.class));
    }*/
    
    @Deployment
    public static WebArchive deploy() {
      return ShrinkWrap.create(WebArchive.class, "foo.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "classes/META-INF/beans.xml")
            .addClasses(SimpleBean.class, MyServlet.class)
            .setWebXML("in-container-web.xml")
            .addAsLibraries(
                  DependencyResolvers.use(MavenDependencyResolver.class)
                        .loadMetadataFromPom("pom.xml")
                        .goOffline()
                        .artifact("org.jboss.weld.servlet:weld-servlet")
                        .resolveAs(GenericArchive.class));
    }

    @Test
    public void test() {
        Assert.assertNotNull(foo);
    }
}

