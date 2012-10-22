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
package org.jboss.arquillian.container.wls.managed_12_1;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies Arquillian can deploy a JAR and run in-container tests.
 *
 * @author Vineet Reynolds
 */
@RunWith(Arquillian.class)
public class WebLogicDeployJarTest {
    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(WebLogicDeployJarTest.class.getName());

    @EJB
    private Greeter greeter;
    
    /**
     * Deployment for the test
     *
     * @return
     */
    @Deployment
    public static Archive<?> getTestArchive() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(Greeter.class);
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void shouldBeAbleToDeployEnterpriseArchive() throws Exception {
        assertThat(greeter, notNullValue());
        assertThat(greeter.greet(), equalTo("Hello"));
    }
}
