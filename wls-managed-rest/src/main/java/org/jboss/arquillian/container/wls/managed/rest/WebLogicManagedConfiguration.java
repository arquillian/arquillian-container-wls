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
package org.jboss.arquillian.container.wls.managed.rest;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.wls.CommonManagedWebLogicConfiguration;
import org.jboss.arquillian.container.wls.Validate;

/**
 * Arquillian properties for managed WebLogic 12.1.x+ containers.
 *
 * @author <a href="mailto:phil.zampino@oracle.com">Phil Zampino</a>
 */
public class WebLogicManagedConfiguration extends CommonManagedWebLogicConfiguration {

    public void validate() throws ConfigurationException {
        Validate.notNullOrEmpty(getWlHome(), "The wlHome property is empty. Verify the property in arquillian.xml");
        Validate.notNullOrEmpty(getMiddlewareHome(),
            "The middlewareHome property value is not specified. Verify the property in arquillian.xml");
        Validate.notNullOrEmpty(getDomainDirectory(),
            "The domainDirectory property value is not specified. Verify the property in arquillian.xml");
        super.validate();
    }
}
