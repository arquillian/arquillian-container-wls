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
package org.jboss.arquillian.container.wls.managed_10_3;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * WebLogic 10.3.x (and prior versions) packages the contents of WEB-INF\classes
 * into a JAR file _wl_cls_gen.jar (placed in WEB-INF\lib) during deployment.
 * If beans.xml is present in the WEB-INF directory, then Weld will be unable to locate it,
 * since the classloader in WebLogic will attempt to find it in the _wl_cls_gen.jar
 * and other JARs in WEB-INF\lib.
 * 
 * This {@link ProtocolArchiveProcessor} will relocate the beans.xml found in WEB-INF
 * of all protocol deployments, to the WEB-INF/classes/META-INF directory of the archive.
 * When WebLogic packages the WEB-INF\classes contents into the _wl_cls_gen.jar,
 * the classloader will now find the beans.xml file, as it would be placed in the META-INF directory
 * of _wl_cls_gen.jar.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicCDIProcessor implements ProtocolArchiveProcessor
{

   public void process(TestDeployment testDeployment, Archive<?> protocolArchive)
   {
      Archive<?> testArchive = testDeployment.getApplicationArchive();
      relocateBeansXML(testArchive, protocolArchive);
   }

   private void relocateBeansXML(Archive<?> testArchive, Archive<?> protocolArchive)
   {
      if(WebArchive.class.isInstance(testArchive) && testArchive.contains("WEB-INF/beans.xml"))
      {
         WebArchive webTestArchive = WebArchive.class.cast(testArchive);
         Asset beansXML = webTestArchive.delete(ArchivePaths.create("WEB-INF/beans.xml")).getAsset();
         webTestArchive.addAsWebInfResource(beansXML,"classes/META-INF/beans.xml");
      }
      else if(EnterpriseArchive.class.isInstance(testArchive))
      {
         EnterpriseArchive enterpriseTestArchive = EnterpriseArchive.class.cast(testArchive);
         for(WebArchive nestedWebTestArchive : enterpriseTestArchive.getAsType(WebArchive.class, Filters.include("/.*\\.war")))
         {
            if(nestedWebTestArchive.contains("WEB-INF/beans.xml"))
            {
               Asset beansXML = nestedWebTestArchive.delete(ArchivePaths.create("WEB-INF/beans.xml")).getAsset();
               nestedWebTestArchive.addAsWebInfResource(beansXML,"classes/META-INF/beans.xml");
            }
         }
      }
   }

}
