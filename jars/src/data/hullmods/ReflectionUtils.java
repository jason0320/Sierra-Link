package data.hullmods;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ReflectionUtils {

    private static final MethodHandles.Lookup LOOKUP;
    private static final Object METHOD_INVOKE_HANDLE;
    private static final Object GET_DECLARED_METHODS_HANDLE;
    private static final Object GET_METHODS_HANDLE;
    private static final Object GET_METHOD_NAME_HANDLE;

    static {
        try {
            LOOKUP = MethodHandles.lookup();
            Class<?> methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            Class<?> objArrayClass = Object[].class;

            METHOD_INVOKE_HANDLE = LOOKUP.findVirtual(methodClass, "invoke",
                    MethodType.methodType(Object.class, Object.class, objArrayClass));

            GET_DECLARED_METHODS_HANDLE = LOOKUP.findVirtual(Class.class, "getDeclaredMethods",
                    MethodType.methodType(Class.forName("[Ljava.lang.reflect.Method;", false, Class.class.getClassLoader())));

            GET_METHODS_HANDLE = LOOKUP.findVirtual(Class.class, "getMethods",
                    MethodType.methodType(Class.forName("[Ljava.lang.reflect.Method;", false, Class.class.getClassLoader())));

            GET_METHOD_NAME_HANDLE = LOOKUP.findVirtual(methodClass, "getName",
                    MethodType.methodType(String.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invoke(String methodName, Object instance, Object... args) throws Throwable {
        Class<?> clazz = instance.getClass();

        Object[] declared = (Object[]) ((java.lang.invoke.MethodHandle) GET_DECLARED_METHODS_HANDLE).invoke(clazz);
        Object[] pub     = (Object[]) ((java.lang.invoke.MethodHandle) GET_METHODS_HANDLE).invoke(clazz);

        Object targetMethod = null;

        for (Object m : declared) {
            String name = (String) ((java.lang.invoke.MethodHandle) GET_METHOD_NAME_HANDLE).invoke(m);
            if (methodName.equals(name)) { targetMethod = m; break; }
        }
        if (targetMethod == null) {
            for (Object m : pub) {
                String name = (String) ((java.lang.invoke.MethodHandle) GET_METHOD_NAME_HANDLE).invoke(m);
                if (methodName.equals(name)) { targetMethod = m; break; }
            }
        }

        if (targetMethod == null) throw new RuntimeException("Method not found: " + methodName);

        return ((java.lang.invoke.MethodHandle) METHOD_INVOKE_HANDLE).invoke(targetMethod, instance, args);
    }
}