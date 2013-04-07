package bootstrap

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import groovy.util.slurpersupport.GPathResult;
import org.codehaus.groovy.grails.plugins.*;
import org.codehaus.groovy.grails.commons.*;


class RFGrailsPluginManagerFactoryBean implements FactoryBean<GrailsPluginManager>, InitializingBean, ApplicationContextAware {

    private GrailsApplication application;
    private GrailsPluginManager pluginManager;
    private Resource descriptor;
    private ApplicationContext applicationContext;
    private List filterPatterns=[~'JqueryGrailsPlugin',~'ResourcesGrailsPlugin']

    public GrailsPluginManager getObject() {
        return pluginManager;
    }

    public Class<GrailsPluginManager> getObjectType() {
        return GrailsPluginManager.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        pluginManager = PluginManagerHolder.getPluginManager();

        if (pluginManager == null) {
            Assert.state(descriptor != null, "Cannot create PluginManager, /WEB-INF/grails.xml not found!");

            ClassLoader classLoader = application.getClassLoader();
            List<Class<?>> classes = new ArrayList<Class<?>>();
            InputStream inputStream = null;

            try {
                inputStream = descriptor.getInputStream();

                // Xpath: /grails/plugins/plugin, where root is /grails
                GPathResult root = new XmlSlurper().parse(inputStream);
                GPathResult plugins = (GPathResult) root.getProperty("plugins");
                GPathResult nodes = (GPathResult) plugins.getProperty("plugin");

                int count = nodes.size()
                for (int i = 0; i < count; i++) {
                    GPathResult node = (GPathResult) nodes.getAt(i);
                    final String pluginName = node.text();
                    if(filterPatterns.any{pluginName ==~ it}){
                        continue
                    }
                    Class<?> clazz;
                    if (classLoader instanceof GroovyClassLoader) {
                        clazz = classLoader.loadClass(pluginName);
                    }
                    else {
                        clazz = Class.forName(pluginName, true, classLoader);
                    }
                    if (!classes.contains(clazz)) {
                        classes.add(clazz);
                    }
                }
            }
            finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }

            Class<?>[] loadedPlugins = classes.toArray(new Class[classes.size()]);

            pluginManager = new DefaultGrailsPluginManager(loadedPlugins, application);
            pluginManager.setApplicationContext(applicationContext);
            PluginManagerHolder.setPluginManager(pluginManager);
            pluginManager.loadPlugins();
        }

        pluginManager.setApplication(application);
        pluginManager.doArtefactConfiguration();
        application.initialise();
    }

    public void setGrailsDescriptor(Resource grailsDescriptor) {
        descriptor = grailsDescriptor;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @param application the application to set
     */
    public void setApplication(GrailsApplication application) {
        this.application = application;
    }
}

