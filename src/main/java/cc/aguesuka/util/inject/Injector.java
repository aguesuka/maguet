package cc.aguesuka.util.inject;

import cc.aguesuka.util.inject.annotation.Config;
import cc.aguesuka.util.inject.annotation.Init;
import cc.aguesuka.util.inject.annotation.Inject;
import cc.aguesuka.util.inject.help.ClassUtil;
import cc.aguesuka.util.inject.help.InjectorException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 注入器
 * <p>
 * 支持按类型和名称注入,不支持泛型注入;只支持field注入;所有依赖必须有无参构造方法;同一个class只有一个实例
 * <p>
 * 使用{@link this#addClass(Class[])}添加依赖
 * 使用{@link this#addProperties(String)}添加配置文件
 * 使用{@link this#build()}开始注入
 * 使用{@link this#instanceByClass(Class)}{@link this#instanceByName(String)} 获得对象
 * <p>
 * 要注入的field加上{@link Inject}注解,无参为按类型注入,有参为按名称注入
 * 在class上加上{@link Inject} 注解为为class取名称,同一个class只有一个名称
 * {@link Config}  注解会从配置文件中注入响应字段,只支持int double boolean String byte[]类型
 * {@link Init} 注入结束后会调用init的方法,必须是无参方法
 *
 * @author :yangmingyuxing
 * 2019/7/9 23:05
 * @version 1.0
 */
public class Injector implements AutoCloseable {
    private static final String DEFAULT_VALUE = "";
    private Properties properties = new Properties();
    private Set<Class<?>> resources = new LinkedHashSet<>();
    private Map<String, Object> nameMap = new HashMap<>();
    private Map<Class<?>, Object> typeMap = new HashMap<>();
    private Set<Class<?>> repeatSet = new HashSet<>();

    /**
     * 添加依赖的类
     *
     * @param classes 依赖的类
     * @return this
     */
    public Injector addClass(Class<?>... classes) {
        resources.addAll(Arrays.asList(classes));
        return this;
    }

    /**
     * 添加配置文件
     *
     * @param path 配置文件路径,以classpath开始的相对路径
     * @return this
     */
    public Injector addProperties(String path) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path)) {
            Objects.requireNonNull(inputStream);
            properties.load(inputStream);
        } catch (IOException | NullPointerException e) {
            throw new InjectorException("加载配置文件失败:" + path);
        }
        return this;
    }

    /**
     * 注入对象并执行初始化方法
     *
     * @return this
     */
    public Injector build() {
        List<Object> instanceList = new ArrayList<>();
        for (Class<?> c : resources) {
            try {
                Constructor<?> declaredConstructor = c.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                instanceList.add(declaredConstructor.newInstance());
            } catch (ReflectiveOperationException e) {
                throw new InjectorException("创建对象失败:" + c.getCanonicalName(), e);
            }
        }
        addBeans(instanceList);
        setField(instanceList);
        doInit(instanceList);
        return this;
    }

    /**
     * 根据类获得实例
     *
     * @param clazz class
     * @param <T>   类的类型
     * @return 类的实例
     */
    @SuppressWarnings("unchecked")
    public <T> T instanceByClass(Class<T> clazz) {
        Object o = typeMap.get(clazz);
        Objects.requireNonNull(o);
        return (T) o;
    }

    /**
     * 根据名称获得实例
     *
     * @param name 对象名称
     * @param <T>  对象类型
     * @return 对象实例
     * @see Inject#value()
     */
    @SuppressWarnings("unchecked")
    public <T> T instanceByName(String name) {
        Object o = nameMap.get(name);
        Objects.requireNonNull(o);
        return (T) o;
    }

    private void addBeans(Collection<Object> beans) {

        for (Object bean : beans) {
            Class<?> clazz = bean.getClass();
            // name
            Inject annotation = clazz.getAnnotation(Inject.class);
            String name = annotation == null ? DEFAULT_VALUE : annotation.value();
            if (!DEFAULT_VALUE.equals(name)) {
                if (nameMap.containsKey(name)) {
                    throw new InjectorException(String.format("%s,%s,使用了同样的名称:%s",
                            clazz.getCanonicalName(),
                            nameMap.get(name).getClass().getCanonicalName(), name));
                } else {
                    nameMap.put(name, bean);
                }
            }
            // class
            for (Class<?> parentClass : ClassUtil.classParent(clazz)) {
                if (repeatSet.contains(parentClass)) {
                    continue;
                }
                if (typeMap.containsKey(parentClass)) {
                    typeMap.remove(parentClass);
                    repeatSet.add(parentClass);
                    continue;
                }
                typeMap.put(parentClass, bean);
            }
        }
    }

    private void setField(Collection<Object> instanceList) {
        for (Object instance : instanceList) {
            for (Field declaredField : instance.getClass().getDeclaredFields()) {
                configField(instance, declaredField);
                injectField(instance, declaredField);
            }
        }
    }


    private void injectField(Object instance, Field field) {
        Inject annotation = field.getAnnotation(Inject.class);
        if (annotation == null) {
            return;
        }
        field.setAccessible(true);
        Object value = DEFAULT_VALUE.equals(annotation.value()) ?
                typeMap.get(field.getType()) : nameMap.get(annotation.value());
        if (value == null) {
            throw new InjectorException(String.format("注入field %s 时未找到所需的类", field.toGenericString()));
        }
        try {
            field.set(instance, value);
        } catch (IllegalAccessException | RuntimeException e) {
            throw new InjectorException("注入field失败:" + field.getName(), e);
        }
    }

    private void configField(Object instance, Field field) {
        Config annotation = field.getAnnotation(Config.class);
        if (annotation == null) {
            return;
        }
        field.setAccessible(true);
        String value = properties.getProperty(annotation.value());
        try {
            Objects.requireNonNull(value);
            Object afterCast = ClassUtil.typeCast(value, field.getType());
            field.set(instance, afterCast);
        } catch (IllegalAccessException | NullPointerException e) {
            throw new InjectorException("设置配置文件失败:" + instance.getClass() + ":" + annotation.value(), e);
        }
    }

    private void doInit(Collection<Object> instanceList) {
        for (Object o : instanceList) {
            for (Method declaredMethod : o.getClass().getDeclaredMethods()) {
                if (declaredMethod.getAnnotation(Init.class) != null) {
                    try {
                        declaredMethod.setAccessible(true);
                        declaredMethod.invoke(o);
                    } catch (ReflectiveOperationException e) {
                        throw new InjectorException(e);
                    }
                }
            }
        }
    }

    /**
     * 关闭容器中的需要关闭的对象
     *
     * @throws Exception 关闭时出现的异常
     */
    @Override
    public void close() throws Exception {
        for (Class<?> resource : resources) {
            Object o = typeMap.get(resource);
            if (o instanceof AutoCloseable) {
                ((AutoCloseable) o).close();
            }
        }
    }
}
