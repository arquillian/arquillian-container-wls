package org.jboss.arquillian.container.wls.remote_103x;

import javax.ejb.Local;

@Local
public interface Greeter
{

   public String greet();

}