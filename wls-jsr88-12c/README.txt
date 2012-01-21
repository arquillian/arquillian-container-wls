
What is this Arquillian container integration for?

This implementation provides the Arquillian integration with your remote WebLogic 12c 
container using the WebLogic JSR-88 deployment protocol implementation. You can use your  
any normal WebLogic 12c server installations to perform your test, but Oracle's preferred  
way is to use a ZIP distribution of WebLogic 12c for development. 

This integration can be used as local or remote container integration as well. Here the  
remote term means that the application server running in a different JVM than the Arquillian 
container/tool, which either can be on the same node/machine or on a different one. 


What are the main features of this integration?

1. As you already may know the Oracle WebLogic Server 12c is now certified for the full 
Java EE 6 platform specification, which enables higher developer productivity with standards 
based, modern APIs, including Servlet 3.0, JAX-RS 1.1, Java Server Faces 2.1, EJB 3.1, Context 
and Dependency Injection for Java, and many others. And now it can be used with Arquallian.

2. This integration is based on the standard JSR-88 deployment API, having a very simplified
configuration to work (in your arquillian.xml only adminUser, andminPasswor with operator 
roll needed to set as minimum, but have only 5 parameters all together).

3. The Aquillian container and the application server can run on different nodes/machines, 
but you need the WebLogic 12c Client as a dependency, it should be on you classpath during 
testing.

4. Works with a stand-alone server and clustered environment as well. 

5. It has already been tested with ZIP Distribution, which is very good for development purpose,
but having only one target that is the administration server as well. But you never know, if you
would actually find a bug, please raise a ticket:-).

The WebLogic client (wlfullclient.jar) which is not available on any well known public maven 
repositories (as of the writing of this container integration). But it is very easy to install 
that dependency in your local repository to make it available on your classpath for tests. 


How to install the WebLogic 12c Client?

You may already have installed the ZIP Distribution of the WebLogic 12c in your development
environment. If you did skip to step number 4.

1. Download the WebLogic 12c ZIP Distribution for the ORACLE site if you have not done before.
You can get it from http://www.oracle.com/technetwork/middleware/weblogic/downloads/index.html

2. Unzip the package to a directory that we will refer here as <MW_HOME>. If you do not want to 
install the WebLogic 12c, you can remove it from this workstation as soon as the WebLogic client
successfully installed on the maven repository. 

3. Install the ZIP distribution as described in the <MW_HOME>/README.txt file.

4. Generate the WebLogic full client .jar file using the WebLogic JarBuilder Tool:  
      - navigate to: <MW_HOME>/wlserver/server/lib
      - run the tool: java -jar wljarbuilder.jar
   Than you can fine the client jar file in the <MW_HOME>/wlserver/server/lib directory. The
   file name is "wlfullclient.jar".   
   
5. Next install the wlfullclient.jar file in your local maven directory as follows:
      - navigate to: <MW_HOME>/wlserver/server/lib if you are not there
	  - use the following command (it should be one line): 
			mvn install:install-file -Dfile=wlfullclient.jar -DgroupId=com.oracle.weblogic 
			-DartifactId=wlfullclient -Dpackaging=jar -Dversion=12.1.1

If everything went well you have done
