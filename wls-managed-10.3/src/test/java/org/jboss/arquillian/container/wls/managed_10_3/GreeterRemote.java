package org.jboss.arquillian.container.wls.managed_10_3;

import javax.ejb.Remote;

/**
 * Remote Interface for {@link GreeterBean}, since no-interface views are not present in Java EE 5.
 * 
 * @author Vineet Reynolds
 *
 */
@Remote
public interface GreeterRemote
{

    public String greet();
}
