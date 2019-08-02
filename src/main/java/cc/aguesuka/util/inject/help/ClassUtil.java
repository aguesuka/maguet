package cc.aguesuka.util.inject.help;


import cc.aguesuka.util.HexUtil;

import java.util.*;

/**
 * @author :yangmingyuxing
 * 2019/7/10 13:22
 */
public final class ClassUtil {
    /**
     * 获得类的所有被继承的父类和实现的接口.
     *
     * @param clazz 类
     * @return 类的所有被继承的父类和实现的接口
     */
    public static Set<Class<?>> classParent(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(clazz);
        while (!stack.isEmpty()) {
            Class<?> last = stack.removeLast();
            result.add(last);
            stack.addAll(Arrays.asList(last.getInterfaces()));
            Class<?> superclass = last.getSuperclass();
            if (superclass != null) {
                stack.add(superclass);
            }
        }
        return result;
    }

    /**
     * 字符串转基本类型
     *
     * @param value 输入值 可以为null
     * @param clazz 类型
     * @param <T>   类型 只支持{@link String} ,{@link int},{@link boolean},{@link double}
     * @return 对应类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T typeCast(String value, Class<T> clazz) {
        Objects.requireNonNull(clazz);
        if (value == null) {
            return null;
        }
        if (clazz == String.class) {
            return clazz.cast(value);
        }
        if (clazz == int.class) {
            return (T) Integer.valueOf(value);
        }
        if (clazz == boolean.class) {
            return (T) Boolean.valueOf(value);
        }
        if (clazz == double.class) {
            return ((T) Double.valueOf(value));
        }
        if (clazz == byte[].class) {
            return (T) HexUtil.decode(value);
        }
        throw new ClassCastException(clazz.toString() + " no support ");
    }


}
