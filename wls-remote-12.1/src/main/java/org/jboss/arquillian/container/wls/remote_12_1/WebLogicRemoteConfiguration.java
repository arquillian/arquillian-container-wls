package org.jboss.arquillian.container.wls.remote_12_1;

import org.jboss.arquillian.container.wls.CommonWebLogicConfiguration;

/**
 * Arquillian properties for the WebLogic 12.1.x containers.
 * Properties derived from the {@link CommonWebLogicConfiguration} class
 * can be overridden or added to, here.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicRemoteConfiguration extends CommonWebLogicConfiguration
{
   /**
    * @param remoteMachine Specify whether the target server is located on another machine (ie. if the target server
    *                      do not share the same file system as the test server)
    */
   public void setRemoteMachine(boolean remoteMachine) {
      this.remoteMachine = remoteMachine;
   }
}
