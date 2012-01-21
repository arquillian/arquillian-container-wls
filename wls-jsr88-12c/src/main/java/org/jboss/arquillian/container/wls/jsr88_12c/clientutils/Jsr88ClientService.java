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
 * @author Z.Paulovics
 */
package org.jboss.arquillian.container.wls.jsr88_12c.clientutils;

import java.io.File;
import java.util.logging.Logger;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.wls.jsr88_12c.Jsr88Configuration;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class Jsr88ClientService {

	private CompletionStatus completionStatus;
	private ProgressHandler progressHandler;

	protected DeploymentManager deploymentManager;
	protected TargetModuleID[] targetModuleIds;
	protected Jsr88Configuration configuration;

	private static final Logger log = Logger.getLogger(Jsr88ClientService.class.getName());


	public Jsr88ClientService(Jsr88Configuration configuration){
    	completionStatus = new CompletionStatus();
    	progressHandler =  new ProgressHandler( completionStatus );
		this.configuration = configuration;
	}

	
	public void startUp() throws WeblogicClientException {

		String message;
		final String deploymentManagerURI = Jsr88Configuration.DMANAGER_URI +
				configuration.getAdminHost() + ":" + configuration.getAdminPort();
		try {

			connectDeploymentManager( Jsr88Configuration.DFACTORY_CLASS, 
					deploymentManagerURI, 
					configuration.getAdminHost(),
					configuration.getAdminPort(),
					configuration.getAdminUser(), 
					configuration.getAdminPassword() );

		} catch (Exception ch) {
			message = "Could not connect to the admin server on: " + deploymentManagerURI + " | " 
		    		+ ch.getMessage();
	        throw new WeblogicClientException(message);
	    }
	}

	
	public HTTPContext doDeploy(Archive<?> archive) {

	  if (getDeploymentManager() == null) {
         throw new WeblogicClientException("Could not deploy since deployment manager was not loaded");
      }

      HTTPContext httpContext = null;
	  Target[] deploymentTargets = getTargetArray();

	  /* The deployment name on the server side will be the archive name, the web module 
	   * module context-root either will be the archive name without the extension or the
	   * specified value in an appropriate .xml file if you have any (for instance: in a
	   * glassfish-web.xml in case of GlassFish, or weblogic.xml in case of WebLogic)
	   */
      File archiveFile = ShrinkWrapUtil.toFile(archive);
      ProgressObject progress = getDeploymentManager().distribute(deploymentTargets, archiveFile, null);
      
      if ( !waitForCompletion(progress) ) {
    	  throw new WeblogicClientException("Deploying module; Status: " + progress.getDeploymentStatus().getState()
    			  + " message: " + progress.getDeploymentStatus().getMessage()); 
      } else {				  
	      targetModuleIds = progress.getResultTargetModuleIDs();		      

	      ProgressObject startProgress = getDeploymentManager().start(targetModuleIds);
	      // waitForCompletion(startProgress);
	      if ( !waitForCompletion(startProgress) ) {
	    	  throw new WeblogicClientException("Startinging module; Status: " + startProgress.getDeploymentStatus().getState()
	    			  + " message: " + startProgress.getDeploymentStatus().getMessage() );
		  } else {
		      targetModuleIds = startProgress.getResultTargetModuleIDs();
		      if ( targetModuleIds.length > 1 ) {
		    	  throw new WeblogicClientException( "Deployment to multiple targets is not implemented." );
		      }
		  }
	     
	    }
		
		/* 
		 * Gives back a default httpContext based on the adminserver host and port
		 * Servlets can not be accessed by JSR-88, we shall use JMX to construct a
		 * proper httpContext for Aquillian.
		 */
      	String host = configuration.getAdminHost();
      	int port = Integer.valueOf(configuration.getAdminPort());
		httpContext = new HTTPContext( host, port );
				
		return httpContext;
	}

	
	public void doUndeploy(Archive<?> archive) {

		if ( getDeploymentManager() == null ) {
			throw new WeblogicClientException("Could not undeploy since deployment manager was not loaded");
		}

		String deploymentName = createDeploymentName(archive.getName());
		ModuleType deploymentType = getModuleType(archive);	      
	    Target[] deploymentTargets = getTargetArray();
		TargetModuleID[] availableModuleIDs = null;
		TargetModuleID moduleMatch = null;
		try {
			availableModuleIDs = getDeploymentManager().getAvailableModules(deploymentType, deploymentTargets);			
		} catch (Exception e) {
			throw new WeblogicClientException( e.getMessage() );
		}
		for ( TargetModuleID candidate : availableModuleIDs) {
			if ( candidate.getModuleID().equals(deploymentName) ){
				moduleMatch = candidate;
				break;
			}  
		}
		if (moduleMatch == null) {
			throw new WeblogicClientException("Could not find the module " + deploymentName + "on the target " + configuration.getTarget());
		}
		TargetModuleID[] undeployModulIds = {moduleMatch};
		ProgressObject stopProgress = getDeploymentManager().stop(undeployModulIds);

		if ( !waitForCompletion(stopProgress) ) {
			throw new WeblogicClientException( "Stopping module; Status: " + stopProgress.getDeploymentStatus().getState() 
					+ " message: " + stopProgress.getDeploymentStatus().getMessage() );
		} else {
			ProgressObject undeployProgress = getDeploymentManager().undeploy(undeployModulIds);
			if ( !waitForCompletion(stopProgress ) ) {
				throw new WeblogicClientException( "Undeploying module; Status: " + undeployProgress.getDeploymentStatus().getState()
						+ " message: " + undeployProgress.getDeploymentStatus().getMessage() );
			}
		}
		
	}

	public void shutDown() {
		releaseDeploymentManager();		
	}

   protected DeploymentManager connectDeploymentManager(String factoryClass, String uri, 
		   String host, String port, String username, String password) throws Exception {

	  if (getDeploymentManager() == null) {
		 final String deploymentManagerURI = Jsr88Configuration.DMANAGER_URI +
					configuration.getAdminHost() + ":" + configuration.getAdminPort();
         DeploymentFactoryManager dfm = DeploymentFactoryManager.getInstance();
         DeploymentFactory df = (DeploymentFactory) Class.forName(factoryClass).newInstance();
                  
         if ( df.handlesURI(deploymentManagerURI) ) {
             dfm.registerDeploymentFactory(
                     (DeploymentFactory) Class.forName(factoryClass).newInstance());
        	 setDeploymentManager( dfm.getDeploymentManager(deploymentManagerURI, username, password) );
     		 log.info( "DeploymentManager has connected successfully to the server." );
         } else {
			 throw new WeblogicClientException( "The provided URI " + uri + " is not supported." );
         }
      }
      return getDeploymentManager();
   }

   protected DeploymentManager getDeploymentManager() {
	   return deploymentManager;
   }
   
   protected void setDeploymentManager( DeploymentManager deploymentManager) {
	   this.deploymentManager = deploymentManager;
   }
   
   protected void releaseDeploymentManager()
   {
      if ( getDeploymentManager() != null)
      {
    	  getDeploymentManager().release();
      }
   }

   protected Target[] getTargetArray() {
	      Target targetMatch = null;
	      
	      Target[] availableTargets = getDeploymentManager().getTargets();
	      for (Target candidate : availableTargets) {
	         if (candidate.getName().equals(configuration.getTarget())) {
	            targetMatch = candidate;
	            break;
	         }
	      }

	      if ( targetMatch == null) {
	    	  throw new WeblogicClientException( "Can not find the specified target: " + configuration.getTarget() );
		  }

		  Target[] deploymentTargets = { targetMatch };	      
		  return deploymentTargets;
   }
   
   private ModuleType getModuleType(Archive<?> archive) {

	  if (WebArchive.class.isInstance(archive)) {
         return ModuleType.WAR;
      } else if (EnterpriseArchive.class.isInstance(archive)) {
         return ModuleType.EAR;
      } else if (ResourceAdapterArchive.class.isInstance(archive)) {
         return ModuleType.RAR;
      } else {
    	 return ModuleType.EJB;
      }
   }

   protected String createDeploymentName(String archiveName) {

	   String correctedName = archiveName;
		if(correctedName.startsWith("/"))
		{
			correctedName = correctedName.substring(1);
		}
		if(correctedName.indexOf(".") != -1)
		{
			correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
		}
		return correctedName;
   }

	protected Jsr88Configuration getConfiguration() {
		return configuration;
	}

	protected void setConfiguration(Jsr88Configuration configuration) {
		this.configuration = configuration;
	}


   
   /**
    * Wait for the completion of an long running action initiated by the JSR-88
    * 
    * @param ProgressObject - ProgressObject provided by theDeploymentManager 
    * @return true/false	- true if the call was successful; false if failed 
    */	
    protected boolean waitForCompletion(ProgressObject progressObj) {

    	completionStatus.reset();

    	progressObj.addProgressListener(progressHandler);
    	completionStatus.waitForCompleted();
    	progressObj.removeProgressListener(progressHandler);

    	if ( completionStatus.getDeploymentStatus() == null ) {
    		// the deployment has failed
    		return false;
    	}
    	return completionStatus.getDeploymentStatus().isCompleted();
	}	
	

    /**
     * The object of type CompletionStatus is synchronized to make it sure 
     * the access of the object is Thread safe. 
     */	    
    class CompletionStatus {

    	private boolean completed = false;
    	DeploymentStatus finalStatus = null;				
    	
        public synchronized void reset() {
            // Reset the completed status.
            completed = false;
        	finalStatus = null;				
        }

		public DeploymentStatus getDeploymentStatus() {
            return finalStatus;
        }

		public synchronized void setStatus(DeploymentStatus status) {
            // Reset state
            completed = true;
	    	finalStatus = status;				
            // Notify: status has changed
            notifyAll();
        }

        public synchronized void waitForCompleted() {
            // Wait until the action is completed
            while ( !completed ) {
                try {
                	wait();
                } catch (InterruptedException e) {}
            }
        }    	
    }
    
    class ProgressHandler implements ProgressListener { 

    	private CompletionStatus statusObject = null;

		ProgressHandler (CompletionStatus statusObject) {
			this.statusObject = statusObject;
		}
		
		public void handleProgressEvent(ProgressEvent event){ 

			if ( !event.getDeploymentStatus().isRunning() ) {
/*    		
  				log.info( "Deployment status: " 
	    			+ event.getTargetModuleID().getModuleID() 
	    			+ " / " + event.getTargetModuleID().getTarget().getName() 
    				+ " / " + event.getDeploymentStatus().getState()
    				+ " / " + event.getDeploymentStatus().getMessage());
*/
				statusObject.setStatus(event.getDeploymentStatus());				
			}
		}

    }

}
