（本人研究的方案，2012年7月首先在javaeye发表）

**Gorm** 是 **Grails** 框架的orm（基于Hibernate）。Gorm用起来很方便，比直接用Hibernate方便。

很多用过Grails框架的开发者都有把Gorm独立拿出来使用的想法。比如有一些代码是在Grails环境里写的，但是想脱离web服务器使用。grails倒是可以命令行运行脚本，但是需要在grails应用的目录上运行，需要源代码，并且会有构建过程。

这个问题grails官方没有提供完整的方案。我通过查看grails的相关代码，解决了这个问题。贴出来和大家分享。


这个方案没有完全脱离grails，还是要使用grails的包，还是按grails的方式启动，只是过滤了不需要的资源（如Controller和Taglib）和插件。


共有以下文件（假设在~/standalone目录下）：


```
wy-starter.conf
log4j.properties
xxx.groovy
bootstrap/
    Bootstrap.groovy
    RFGrailsPluginManagerFactoryBean.groovy
    RFGrailsApplicationFactoryBean.groovy
lib/
    spring-test-3.1.0.RELEASE.jar
    jansi-1.2.1.jar
    ant-1.8.2.jar
    jna-3.2.3.jar
    jline-1.0.jar
    servlet-api-2.4.jar
    gant_groovy1.8-1.9.6.jar
    commons-cli-1.2.jar[/code]
```


另外需要grails应用部署（War解压后的目录）路径。指定部署路径是为了方便，其实只需要WEB-INF下的grails.xml、plugins、classes、lib。其中classes和lib在wy-starter.conf里设置类路径。

 
wy-starter.conf是groovy的启动配置文件；xxx.groovy是要运行的脚本文件；假设war包解压目录为~/standalone/wy
 
wy-starter.conf内容如下：

```
load ~/standalone/wy/WEB-INF/classes
load ~/standalone/wy/WEB-INF/lib/*.jar
load ~/standalone/lib/*.jar
load ~/standalone/bootstrap/*.groovy
load ${tools.jar}[/code]
```

xxx.groovy内容如下：

```java
import bootstrap.*

def wyBase=System.getenv('WY_WEB_APP_BASE')
def appCtx=Bootstrap.bootstrap(wyBase)

//使用GORM的业务代码...[/code]
```

bootstrap/Bootstrap.groovy是启动Gorm环境的类。


bootstrap/RF*只是在grails的GrailsPluginManagerFactoryBean.groovy和GrailsApplicationFactoryBean.groovy的基础上增加了过滤的功能（前者过滤Controller和Taglib，后者过滤了除Hibernate外的插件）。直接用Grails的也是可以的，只不过多加载一些东西。


数据库的连接配置和grails一样，如果配置在grails-app/conf/DataSource.groovy的话，已经编译到classes里去了。事实上，grails-app/conf里所以的配置都是可用的。


最后，要运行xxx.groovy，可以这样：


```shell
cd ~/standalone
export GROOVY_CONF=~/standalone/wy-starter.conf
export WY_WEB_APP_BASE=~/standalone/wy
groovy xxx.groovy
```
 
注意GROOVY_CONF变量静态定义的话(如放在~/.profile)会影响i此用户运行其他groovy脚本。
以上命令以linux环境为例。如果环境是windows也差不多（可能要改一下groovy/bin/groovy.bat，以使用GROOVY_CONF环境变量配置的启动配置文件。
