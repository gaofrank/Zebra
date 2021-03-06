package com.zebra.boot.listener;

import com.zebra.boot.registry.IRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Map;

/**
 * Created by js-dev.cn on 2016/11/24.
 * 监听器:当服务启动之后,立即监听并开始向注册中心Registry注册服务,告诉注册中心,自己是一个服务节点
 */
@Component
@ComponentScan
@WebListener
public class ZebraListener implements ServletContextListener{

    private static Logger logger = LoggerFactory.getLogger(ZebraListener.class);

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private IRegistry registry;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        //获取上下文
        ServletContext sc = servletContextEvent.getServletContext();
        ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);

        //获取Mapping(就是存放了所有的Controoler中@Path注解的Url映射)
        RequestMappingHandlerMapping mapping = ac.getBean(RequestMappingHandlerMapping.class);

        //遍历所有的handlerMethods(为了从mapping中取出Url,交给Zookeeper进行注册)
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        for (RequestMappingInfo key : handlerMethods.keySet()) {
            String serviceName = key.getName();
            if(!StringUtils.isEmpty(serviceName)){
                //不等于空,则拿到了服务名称,则立即注册
                registry.register(serviceName,String.format("%s:%d",serverAddress,serverPort));
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        //向注册中心销毁自己的服务信息

        //获取上下文
        ServletContext sc = servletContextEvent.getServletContext();
        //获取ac
        ApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sc);

        //获取Mapping(就是存放了所有的Controoler中@Path注解的Url映射)
        RequestMappingHandlerMapping mapping = ac.getBean(RequestMappingHandlerMapping.class);
        //遍历所有的handlerMethods(为了从mapping中取出Url,交给Zookeeper进行注册)
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
        for (RequestMappingInfo key : handlerMethods.keySet()) {
            String serviceName = key.getName();
            if (!StringUtils.isEmpty(serviceName)) {
                logger.info("卸载服务:" + serviceName);
                //不等于空,表示找到了配置中心的服务地址，则拿到了服务名称,要进行立即注册
                registry.unRegister(serviceName, String.format("%s:%d", serverAddress, serverPort));
            }
        }
    }
}
