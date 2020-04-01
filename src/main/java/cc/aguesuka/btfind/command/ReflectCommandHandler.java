package cc.aguesuka.btfind.command;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * evil reflect
 *
 * @author :aguesuka
 * 2020/2/21 10:06
 */
public class ReflectCommandHandler {
    private final Object root;

    public ReflectCommandHandler(Object root) {
        this.root = root;
    }

    public void select(String param) {
        if (param == null || param.isBlank()) {
            System.out.println(root);
            return;
        }
        String[] s = param.trim().split(" ", 2);
        String path = s[0];
        Object o = getObjectByPath(path);
        if (s.length == 1) {
            System.out.println(o);
        } else {
            String method = s[1].trim();
            System.out.println(callMethod(o, method));
        }
    }

    public void method(String path) {
        Arrays.stream(getObjectByPath(path).getClass()
                .getDeclaredMethods())
                .filter(method -> method.getParameterCount() == 0)
                .forEach(method -> System.out.println(method.getName()));
    }

    private Object callMethod(Object o, String methodName) {
        Method[] declaredMethods = o.getClass().getDeclaredMethods();
        try {
            Method method = Arrays.stream(declaredMethods)
                    .filter(m -> m.getParameterCount() == 0 && match(m.getName(), methodName))
                    .findAny().orElseThrow(() -> new IllegalArgumentException("method not find"));
            method.setAccessible(true);
            System.out.println("call " + method.getName());
            return method.invoke(o);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Object getObjectByPath(String path) {
        Object o;
        List<String> nameList = new ArrayList<>();
        if (path == null || path.isBlank()) {
            o = root;
        } else {
            o = getObjectByPatterns(path.split("\\."), nameList);
        }
        System.out.println("path " + String.join(".", nameList));
        return o;
    }

    public void field(String path) {
        Object o = getObjectByPath(path);
        if (o != null) {
            Arrays.stream(o.getClass().getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .sorted(Comparator.comparingInt(f -> f.getName().length()))
                    .collect(Collectors.toList())
                    .forEach(f -> System.out.println(f.getName()));
        }
    }

    private Object getObjectByPatterns(String[] patternArray, List<String> nameList) {
        Object currentObject = root;
        for (String pattern : patternArray) {
            if (currentObject == null) {
                return null;
            }
            currentObject = getField(currentObject, pattern, nameList);
        }
        return currentObject;
    }

    private Object getField(Object o, String pattern, List<String> nameList) {
        List<Field> fieldList = Arrays.stream(o.getClass().getDeclaredFields())
                .filter(field -> match(field.getName(), pattern))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .sorted(Comparator.comparingInt(f -> f.getName().length()))
                .collect(Collectors.toList());
        if (fieldList.size() == 0) {
            throw new IllegalArgumentException("can not find field");
        }
        Field field = fieldList.get(0);
        nameList.add(field.getName());
        field.setAccessible(true);
        try {
            return field.get(o);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private boolean match(String fieldName, String pattern) {
        int fieldNameIndex = 0;
        int patternIndex = 0;
        while (patternIndex < pattern.length() && fieldNameIndex < fieldName.length()) {
            char c = Character.toLowerCase(pattern.charAt(patternIndex));
            char c1 = Character.toLowerCase(fieldName.charAt(fieldNameIndex));
            if (c == c1) {
                patternIndex++;
            }
            fieldNameIndex++;
        }
        return patternIndex == pattern.length();
    }
}
