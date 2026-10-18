package com.acme.pricing.testing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Runs every @Test method of every *Test class found under the given classes directory. */
public final class TestRunner {

    public static void main(String[] args) throws Exception {
        Path classesDir = Path.of(args.length > 0 ? args[0] : "build/classes");
        int passed = 0;
        int failed = 0;
        for (String className : findTestClasses(classesDir)) {
            Class<?> testClass = Class.forName(className);
            for (Method method : testClass.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Test.class)) {
                    continue;
                }
                String name = testClass.getSimpleName() + "." + method.getName();
                try {
                    method.invoke(testClass.getDeclaredConstructor().newInstance());
                    passed++;
                    System.out.println("PASS " + name);
                } catch (InvocationTargetException e) {
                    failed++;
                    System.out.println("FAIL " + name + ": " + e.getCause());
                }
            }
        }
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 && passed > 0 ? 0 : 1);
    }

    private static List<String> findTestClasses(Path classesDir) throws IOException {
        try (Stream<Path> files = Files.walk(classesDir)) {
            return files
                    .map(path -> classesDir.relativize(path).toString())
                    .filter(path -> path.endsWith("Test.class"))
                    .map(path -> path.replace('\\', '/').replace('/', '.').replaceAll("\\.class$", ""))
                    .sorted()
                    .toList();
        }
    }
}
