package hiddencheck;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public final class HiddenRunner {

    public static void main(String[] args) throws Exception {
        String token = args[0];
        int passed = 0;
        int failed = 0;
        for (String className : java.util.List.of(args).subList(1, args.length)) {
            Class<?> type = Class.forName(className);
            Method[] methods = type.getDeclaredMethods();
            Arrays.sort(methods, Comparator.comparing(Method::getName));
            for (Method method : methods) {
                if (!method.isAnnotationPresent(Check.class)) {
                    continue;
                }
                String name = type.getSimpleName() + "." + method.getName();
                try {
                    method.invoke(type.getDeclaredConstructor().newInstance());
                    passed++;
                    System.out.println("PASS " + name);
                } catch (InvocationTargetException e) {
                    failed++;
                    System.out.println("FAIL " + name + ": " + e.getCause());
                }
            }
        }
        System.out.println("hidden: " + passed + " passed, " + failed + " failed, token " + token);
        System.exit(failed == 0 && passed > 0 ? 0 : 1);
    }

    static void eq(Object expected, Object actual, String what) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
