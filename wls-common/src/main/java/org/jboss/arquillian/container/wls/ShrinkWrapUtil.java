/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * ShrinkWrapUtil
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public final class ShrinkWrapUtil
{
   private ShrinkWrapUtil() { }

   /**
    * Creates a tmp folder and exports the file. Returns the URL for that file location.
    * 
    * @param archive Archive to export
    * @return
    */
   public static URL toURL(final Archive<?> archive)
   {
      // create a random named temp file, then delete and use it as a directory
      try
      {
         File root = File.createTempFile("arquillian", archive.getName());
         root.delete();
         root.mkdirs();
         
         File deployment = new File(root, archive.getName());
         deployment.deleteOnExit();
         archive.as(ZipExporter.class).exportTo(deployment, true);
         return deployment.toURI().toURL();
      }
      catch (Exception e) 
      {
         throw new RuntimeException("Could not export deployment to temp", e);
      }
   }
   
   public static URL toURL(final Descriptor descriptor)
   {
      // create a random named temp file, then delete and use it as a directory
      try
      {
         File root = File.createTempFile("arquillian", descriptor.getDescriptorName());
         root.delete();
         root.mkdirs();
         
         File deployment = new File(root, descriptor.getDescriptorName());
         deployment.deleteOnExit();
         
         FileOutputStream stream = new FileOutputStream(deployment);
         try
         {
            descriptor.exportTo(stream);
         }
         finally
         {
            try
            {   
               stream.close();
            }
            catch (Exception e)
            {
               throw new RuntimeException(e); 
            }
         }

         return deployment.toURI().toURL();
      }
      catch (Exception e) 
      {
         throw new RuntimeException("Could not export deployment to temp", e);
      }
   }

   /**
    * Creates a tmp folder and exports the file. Returns the URL for that file location.
    * 
    * @param archive Archive to export
    * @return
    */
   public static File toFile(final Archive<?> archive)
   {
      return toFile(archive, false);
   }

   /**
    * Creates a tmp folder and exports the file. Returns the URL for that file location.
    *
    * @param archive Archive to export
    * @param exploded Specifies, whether to explode the archive after creation
    * @return
    */
   public static File toFile(final Archive<?> archive, final boolean exploded)
   {
       // create a random named temp file, then delete and use it as a directory
       try
       {
           File root = File.createTempFile("arquillian", archive.getName());
           root.delete();
           root.mkdirs();

           File deployment = new File(root, archive.getName());
           deployment.deleteOnExit();
           archive.as(ZipExporter.class).exportTo(deployment, true);
           return exploded ? Unzipper.unzip(deployment, new File(new File(root, "exploded"), archive.getName()))
                           : deployment;
       }
       catch (Exception e)
       {
           throw new RuntimeException("Could not export deployment to temp", e);
       }
   }

    /**
     * Simple unzip implementation used for extracting contents of deployment archive.
     */
   private static class Unzipper
   {
      private static final int BUFFER = 2048;

      private static File unzip(final File archive, final File outputDir)
      {
         if (!outputDir.exists())
         {
            outputDir.mkdirs();
         }

         try {
            BufferedOutputStream dest = null;
            BufferedInputStream is = null;
            ZipEntry entry;
            ZipFile zipfile = new ZipFile(archive);
            Enumeration e = zipfile.entries();
            while(e.hasMoreElements())
            {
               entry = (ZipEntry) e.nextElement();

               is = new BufferedInputStream(zipfile.getInputStream(entry));
               int count;
               byte data[] = new byte[BUFFER];

               File outputFile = new File(outputDir, entry.getName());
               if (entry.isDirectory())
               {
                  if (!outputFile.exists()) { outputFile.mkdirs(); }
               } else
               {
                  ensureFileExists(outputFile);
                  FileOutputStream fos = new FileOutputStream(outputFile);
                  dest = new BufferedOutputStream(fos, BUFFER);
                  while ((count = is.read(data, 0, BUFFER)) != -1)
                  {
                     dest.write(data, 0, count);
                  }
                  dest.flush();
                  dest.close();
               }
               is.close();
            }
         } catch(Exception e) {
            e.printStackTrace();
         }
         return outputDir;
      }

       private static void ensureFileExists(final File file) throws IOException
       {
          if (!file.getParentFile().exists()) { file.getParentFile().mkdirs(); }
          if (!file.exists()) { file.createNewFile(); }
       }
   }
}
