/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

/**
 *
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
package org.jboss.arquillian.container.wls.remote.rest;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Testable;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.util.logging.Logger;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Verifies Arquillian can deploy a EAR file with multiple WARs as a deployment, and run in-container tests. Used to verify that
 * Arquillian can find the ServletTestRunner from among multiple web-modules through the {@link org.jboss.arquillian.container.test.api.Testable} API.
 * 
 * @author Vineet Reynolds
 */
@RunWith(Arquillian.class)
public class WebLogicTestableWarInEarTest {
    private static final Logger log = Logger.getLogger(WebLogicTestableWarInEarTest.class.getName());

    @EJB(mappedName="java:global/app/ejb/Greeter")
    private Greeter greeter;
    
    @Deployment
    public static EnterpriseArchive getInContainerTestArchive() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(GreeterServlet.class)
                //The deployed EAR does not contain the test class when we build an EnterpriseArchive, and must be manually added.
                .addClass(WebLogicTestableWarInEarTest.class);
       final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "ejb.jar")
                   .addClasses(Greeter.class);
        
        // Create another web module, but with a name that is alphabetically less than test.war.
        WebArchive anotherWar = ShrinkWrap.create(WebArchive.class, "another.war")
                .addClasses(MyServlet.class);
        
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "app.ear")
                .addAsModule(ejb)
                .addAsModule(Testable.archiveToTest(war))
                .addAsModule(anotherWar);
        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void assertTestableEarDeployed() throws Exception {
        assertThat(greeter, notNullValue());
        assertThat(greeter.greet(), equalTo("Hello"));
    }

}
