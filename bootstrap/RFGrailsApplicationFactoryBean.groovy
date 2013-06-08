package bootstrap

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import groovy.util.slurpersupport.GPathResult;
import grails.util.Environment;
import org.codehaus.groovy.grails.commons.*;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.grails.compiler.GrailsClassLoader;
import org.codehaus.groovy.grails.compiler.support.GrailsResourceLoader;


class RFGrailsApplicationFactoryBean implements FactoryBean<GrailsApplication>, InitializingBean {

	private static Log LOG = LogFactory.getLog(RFGrailsApplicationFactoryBean.class);
	private GrailsApplication grailsApplication = null;
	private GrailsResourceLoader resourceLoader;
	private Resource descriptor;
	private List filterPatterns=[~'org\\.grails\\.plugin\\.resource\\..*',~/.*(Service|TagLib)/,~/.*Controller/]

	public void afterPropertiesSet() throws Exception {
		if (descriptor != null && descriptor.exists()) {
			LOG.info("Loading Grails application with information from descriptor.");

			ClassLoader classLoader = null;
			if (Environment.getCurrent().isReloadEnabled()) {
				LOG.info("Reloading is enabled, using GrailsClassLoader.");
				// Enforce UTF-8 on source code for reloads
				final ClassLoader parentLoader = Thread.currentThread().getContextClassLoader();
				CompilerConfiguration config = CompilerConfiguration.DEFAULT;
				config.setSourceEncoding("UTF-8");
				classLoader = new GrailsClassLoader(parentLoader, config, resourceLoader);
			}
			else {
				LOG.info("No reloading, using standard classloader.");
				classLoader = Thread.currentThread().getContextClassLoader();
			}

			List<Class<?>> classes = new ArrayList<Class<?>>();
			InputStream inputStream = null;
			try {
				inputStream = descriptor.getInputStream();

				// Get all the resource nodes in the descriptor.
				// Xpath: /grails/resources/resource, where root is /grails
				GPathResult root = new XmlSlurper().parse(inputStream);
				GPathResult resources = (GPathResult) root.getProperty("resources");
				GPathResult grailsClasses = (GPathResult) resources.getProperty("resource");

				// Each resource node should contain a full class name,
				// so we attempt to load them as classes.
				for (int i = 0; i < grailsClasses.size(); i++) {
					GPathResult node = (GPathResult) grailsClasses.getAt(i);
					String className = node.text();
					if(filterPatterns.any{className ==~ it}){
						continue
					}
					try {
						Class<?> clazz;
						if (classLoader instanceof GrailsClassLoader) {
							clazz = classLoader.loadClass(className);
						}
						else {
							clazz = Class.forName(className, true, classLoader);
						}
						classes.add(clazz);
					}
					catch (ClassNotFoundException e) {
						LOG.warn("Class with name ["+className+"] was not found, and hence not loaded. Possible empty class or script definition?");
					}
				}
			}
			finally {
				if (inputStream != null) {
					inputStream.close();
				}
			}
			Class<?>[] loadedClasses = classes.toArray(new Class[classes.size()]);
			grailsApplication = new DefaultGrailsApplication(loadedClasses, classLoader);
		}
		else {
			Assert.notNull(resourceLoader, "Property [resourceLoader] must be set!");
			grailsApplication = new DefaultGrailsApplication(resourceLoader);
		}

		ApplicationHolder.setApplication(grailsApplication);
	}

	public GrailsApplication getObject() {
		return grailsApplication;
	}

	public Class<GrailsApplication> getObjectType() {
		return GrailsApplication.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setGrailsResourceLoader(GrailsResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setGrailsDescriptor(Resource r) {
		descriptor = r;
	}
}

