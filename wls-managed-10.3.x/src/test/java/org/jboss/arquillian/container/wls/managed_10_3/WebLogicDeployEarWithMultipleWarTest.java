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
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
package org.jboss.arquillian.container.wls.managed_10_3;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.webapp30.WebAppDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies Arquillian can deploy a EAR file with multiple WARs as a deployment.
 *
 * @author Vineet Reynolds
 */
@RunWith(Arquillian.class)
public class WebLogicDeployEarWithMultipleWarTest {
    private static final Logger log = Logger.getLogger(WebLogicDeployEarWithMultipleWarTest.class.getName());

    @Deployment(testable = false)
    public static EnterpriseArchive getTestArchive() {
        Class<MyServlet> servletClass = MyServlet.class;
        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
            .addClasses(MyServlet.class)
            .setWebXML(
                new StringAsset(Descriptors.create(WebAppDescriptor.class).version("2.5").createServlet()
                    .servletName(servletClass.getSimpleName()).servletClass(servletClass.getCanonicalName()).up()
                    .createServletMapping().servletName(servletClass.getSimpleName()).urlPattern("/Test").up()
                    .exportAsString()));

        // Create another web module, but with a name that is alphabetically less than test.war.
        WebArchive anotherWar = ShrinkWrap.create(WebArchive.class, "another.war")
            .addClasses(MyServlet.class)
            .setWebXML(
                new StringAsset(Descriptors.create(WebAppDescriptor.class).version("2.5").createServlet()
                    .servletName(servletClass.getSimpleName()).servletClass(servletClass.getCanonicalName()).up()
                    .createServletMapping().servletName(servletClass.getSimpleName()).urlPattern("/Test").up()
                    .exportAsString()));

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "foo.ear")
            .addAsModule(war)
            .addAsModule(anotherWar);

        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void assertFirstWarDeployed(@ArquillianResource URL deploymentUrl) throws Exception {
        final URLConnection response = new URL(deploymentUrl, "test/" + MyServlet.URL_PATTERN).openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(response.getInputStream()));
        final String result = in.readLine();
        in.close();

        assertThat(result, equalTo("hello"));
    }

    @Test
    public void assertSecondWarDeployed(@ArquillianResource URL deploymentUrl) throws Exception {
        final URLConnection anotherResponse = new URL(deploymentUrl, "another/" + MyServlet.URL_PATTERN).openConnection();

        BufferedReader anotherIn = new BufferedReader(new InputStreamReader(anotherResponse.getInputStream()));
        final String anotherResult = anotherIn.readLine();
        anotherIn.close();

        assertThat(anotherResult, equalTo("hello"));
    }
}
