/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * TestCase to verify CDI support in test classes when deploying EAR files.
 * 
 * @author Vineet Reynolds
 *
 */
@RunWith(Arquillian.class)
public class WebLogicCDIEarTestCase {

    @Inject
    private SimpleBean foo;

    @Deployment
    public static EnterpriseArchive deploy() {
      WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "foo.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClasses(SimpleBean.class, MyServlet.class)
            //The deployed EAR does not contain the test class when we build an EnterpriseArchive, and must be manually added.
            .addClass(WebLogicCDIEarTestCase.class)
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