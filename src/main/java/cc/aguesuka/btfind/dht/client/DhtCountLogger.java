package cc.aguesuka.btfind.dht.client;

import cc.aguesuka.btfind.util.CountMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * record {@link DhtListener} called count
 *
 * @author :yangmingyuxing
 * 2020/2/14 16:17
 */
public interface DhtCountLogger extends DhtListener {

    /**
     * create instance by countMap
     *
     * @param countMap countMap
     * @return DhtCountLogger
     */
    static DhtCountLogger of(CountMap countMap) {
        Class<?>[] interfaces = {DhtCountLogger.class};
        ClassLoader classLoader = DhtCountLogger.class.getClassLoader();

        InvocationHandler invocationHandler = (proxy, method, args) -> {
            countMap.put("dht " + method.getName());
            // all DhtListener is return void
            return null;
        };

        return (DhtCountLogger) Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
    }


}
