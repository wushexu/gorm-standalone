package bootstrap

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator
import org.codehaus.groovy.grails.plugins.GrailsPluginManagerFactoryBean
import org.codehaus.groovy.grails.core.io.*
import org.codehaus.groovy.grails.web.context.*
import grails.spring.WebBeanBuilder

import org.springframework.core.io.FileSystemResource
import org.springframework.mock.web.MockServletContext

class Bootstrap {

    static def bootstrap(appBase){

		if(appBase.endsWith('/') || appBase.endsWith('\\')){
			appBase=appBase[0..-2]
		}

        def beanDefinitions = new WebBeanBuilder().beans {
            grailsResourceLoader(GrailsResourceLoaderFactoryBean)
            grailsDescriptor(FileSystemResource,"$appBase/WEB-INF/grails.xml")
            grailsApplication(RFGrailsApplicationFactoryBean){
                grailsDescriptor = ref(grailsDescriptor)
                grailsResourceLoader = ref("grailsResourceLoader")
            }
            pluginManager(RFGrailsPluginManagerFactoryBean){
                grailsDescriptor = ref(grailsDescriptor)
                application = ref(grailsApplication)
            }
            grailsConfigurator(GrailsRuntimeConfigurator,grailsApplication){
                pluginManager = ref("pluginManager")
            }
        }

        def appCtx = beanDefinitions.createApplicationContext()

        //grailsApp = appCtx.grailsApplication
        def servletContext = new MockServletContext(appBase,new PluginPathAwareFileSystemResourceLoader())
        appCtx.servletContext=servletContext
        servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, appCtx)

        ExpandoMetaClass.enableGlobally()

        def webCtx = GrailsConfigUtils.configureWebApplicationContext(servletContext, appCtx)
        //GrailsConfigUtils.executeGrailsBootstraps(grailsApp, webCtx, servletContext)

        def persistenceInterceptor = webCtx.containsBean('persistenceInterceptor') ? webCtx.persistenceInterceptor : null
        persistenceInterceptor?.init()

        return webCtx
    }

}

