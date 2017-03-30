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
package org.jboss.arquillian.container.wls;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ClassLoader} that is used to load classes
 * from <code>WL_HOME/server/lib/weblogic.jar</code>.
 * <p>
 * Classloading is delegated to the parent first, before
 * attempting to load from the weblogic.jar file.
 *
 * @author Vineet Reynolds
 */
class WebLogicJMXLibClassLoader extends URLClassLoader {
    private static final Logger logger = Logger.getLogger(WebLogicJMXLibClassLoader.class.getName());

    public WebLogicJMXLibClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public WebLogicJMXLibClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public WebLogicJMXLibClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        logger.log(Level.FINEST, "Loading class: {0}", name);
        return super.loadClass(name);
    }
}
