package org.nju.artemis.aejb.component.interceptors;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;

import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.ejb.client.EJBLocator;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.logging.Logger;

/**
 * @author <a href="wangjue1199@gmail.com">Jason</a>
 */
public class DispatcherInterceptor implements Interceptor {
	Logger log = Logger.getLogger(DispatcherInterceptor.class);
	private Map<String,Object> contextData;
	
	@Override
	public Object processInvocation(InterceptorContext context) throws Exception {
		log.info("DispatcherInterceptor: processInvocation");
		contextData = context.getContextData();
		String appName = (String) contextData.get("appName");
		String moduleName = (String) contextData.get("moduleName");
		String distinctName = (String) contextData.get("distinctName");
		String beanName = (String) contextData.get("beanName");
		Class<?> viewClass = (Class<?>) contextData.get("viewClass");
		boolean stateful = (Boolean) contextData.get("stateful");
		return dispatch(context, appName, moduleName, distinctName, beanName, viewClass, stateful);
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "static-access" })
	private Object dispatch(InterceptorContext context, String appName, String moduleName, String distinctName, String beanName, Class<?> viewClass, boolean stateful){
		EJBLocator ejbLocator = null;
		Object result = null;
		final String proxyName = "Proxy:" + appName + moduleName + beanName + distinctName;
		if(contextData.containsKey(proxyName)) {
			final Proxy proxy = (Proxy) contextData.get(proxyName);
	        try {
				result = proxy.getInvocationHandler(proxy).invoke(proxy, context.getMethod(), context.getParameters());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (Throwable e) {
				e.printStackTrace();
			}
	        return result;
		}
		if (EJBHome.class.isAssignableFrom(viewClass) || EJBLocalHome.class.isAssignableFrom(viewClass)) {
            ejbLocator = new EJBHomeLocator(viewClass, appName, moduleName, beanName, distinctName);
        } else if (stateful) {
            try {
                ejbLocator = EJBClient.createSession(viewClass, appName, moduleName, beanName, distinctName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            ejbLocator = new StatelessEJBLocator(viewClass, appName, moduleName, beanName, distinctName);
        }
        final Proxy proxy = (Proxy)EJBClient.createProxy(ejbLocator);
        try {
			result = proxy.getInvocationHandler(proxy).invoke(proxy, context.getMethod(), context.getParameters());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
        context.getContextData().put(proxyName, proxy);
        return result;
	}
}
