package fansirsqi.xposed.sesame.util;

import androidx.core.util.Consumer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


//代码来自https://github.com/zjyzip/AdClose

public class HookUtil {

    public static class HookInfo {
        public String className;
        public String[] methodNames;
        public Object returnValue;

        public HookInfo(String className, String[] methodNames, Object returnValue) {
            this.className = className;
            this.methodNames = methodNames;
            this.returnValue = returnValue;
        }

        public HookInfo(String className, String methodName, Object returnValue) {
            this(className, new String[]{methodName}, returnValue);
        }
    }

    public static void hookSingleMethod(
            ClassLoader classLoader,
            String className,
            String methodName,
            Object returnValue) {

        hookMultipleMethods(classLoader, className, new String[]{methodName}, returnValue);
    }

    public static void hookMultipleMethods(
            ClassLoader classLoader,
            String className,
            String[] methodNames,
            Object returnValue) {

        Class<?> actualClass = getClass(className, classLoader);
        if (actualClass == null) return;

        for (Method method : actualClass.getDeclaredMethods()) {
            for (String targetMethodName : methodNames) {
                if (method.getName().equals(targetMethodName)) {
                    method.setAccessible(true);
                    XposedBridge.hookMethod(method, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return returnValue;
                        }
                    });
                    break;
                }
            }
        }
    }

    public static void findAndHookMethod(
            Object clazz,
            String methodName,
            Object[] parameterTypes,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action) {

        findAndHookMethod(clazz, methodName, parameterTypes, hookType, action, null);
    }

    public static void findAndHookMethod(
            Object clazz,
            String methodName,
            Object[] parameterTypes,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action,
            ClassLoader classLoader) {

        try {
            Class<?> actualClass = getClass(clazz, classLoader);
            if (actualClass == null) {
                XposedBridge.log("findAndHookMethod - Class not found: " + clazz);
                return;
            }

            Class<?>[] classParams;
            if (parameterTypes == null) {
                classParams = new Class<?>[0];
            } else {
                classParams = new Class<?>[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Object param = parameterTypes[i];
                    if (param instanceof String) {
                        classParams[i] = getClass((String) param, classLoader);
                    } else if (param instanceof Class<?>) {
                        classParams[i] = (Class<?>) param;
                    } else {
                        classParams[i] = null;
                    }
                }
            }


            if (Arrays.asList(classParams).contains(null)) {
                XposedBridge.log("findAndHookMethod - One or more parameter types could not be resolved.");
                return;
            }

            Method method = actualClass.getDeclaredMethod(methodName, classParams);
            method.setAccessible(true);
            XposedBridge.hookMethod(method, createMethodHook(hookType, action));

        } catch (Throwable e) {
            XposedBridge.log("findAndHookMethod - Error hooking method: " + methodName + " - " + e.getMessage());
        }
    }

    private static Class<?> getClass(Object clazz, ClassLoader classLoader) {
        if (clazz instanceof String) {
            return XposedHelpers.findClassIfExists((String) clazz, classLoader);
        }
        return (clazz instanceof Class<?>) ? (Class<?>) clazz : null;
    }

    public static void hookMethod(Method method, String hookType, Consumer<XC_MethodHook.MethodHookParam> action) {
        if (method == null) {
            XposedBridge.log("hookMethod - Attempted to hook a null method.");
            return;
        }

        try {
            method.setAccessible(true);
            XposedBridge.hookMethod(method, createMethodHook(hookType, action));
        } catch (Throwable e) {
            XposedBridge.log("hookMethod - Error occurred while hooking method " + method.getName() + ": " + e.getMessage());
        }
    }

    public static void hookAllMethods(
            Object clazz,
            String methodName,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action) {

        hookAllMethods(clazz, methodName, hookType, action, null);
    }

    public static void hookAllMethods(
            Object clazz,
            String methodName,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action,
            ClassLoader classLoader) {

        try {
            Class<?> actualClass = getClass(clazz, classLoader);
            if (actualClass == null) {
                XposedBridge.log("hookAllMethods - Class not found: " + clazz);
                return;
            }
            XposedBridge.hookAllMethods(actualClass, methodName, createMethodHook(hookType, action));

        } catch (Throwable e) {
            XposedBridge.log("hookAllMethods - Error hooking all methods of " + methodName + " - " + e.getMessage());
        }
    }

    public static void hookAllConstructors(
            Object clazz,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action) {

        hookAllConstructors(clazz, hookType, action, null);
    }

    public static void hookAllConstructors(
            Object clazz,
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action,
            ClassLoader classLoader) {

        try {
            Class<?> actualClass = getClass(clazz, classLoader);
            if (actualClass == null) {
                XposedBridge.log("hookAllConstructors - Class not found: " + clazz);
                return;
            }

            XC_MethodHook methodHook = createMethodHook(hookType, action);
            for (Constructor<?> constructor : actualClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                XposedBridge.hookMethod(constructor, methodHook);
            }

        } catch (Throwable e) {
            XposedBridge.log("hookAllConstructors - Error hooking constructors: " + e.getMessage());
        }
    }

    private static XC_MethodHook createMethodHook(
            String hookType,
            Consumer<XC_MethodHook.MethodHookParam> action) {

        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("before".equals(hookType)) action.accept(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if ("after".equals(hookType)) action.accept(param);
            }
        };
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        XposedHelpers.setStaticObjectField(clazz, fieldName, value);
    }

    public static void setStaticObjectField(
            Object clazz,
            ClassLoader classLoader,
            String fieldName,
            Object value) {

        try {
            Class<?> actualClass = getClass(clazz, classLoader);
            if (actualClass != null) {
                XposedHelpers.setStaticObjectField(actualClass, fieldName, value);
            } else {
                XposedBridge.log("setStaticObjectField - Class not found: " + clazz);
            }

        } catch (Throwable e) {
            XposedBridge.log("setStaticObjectField - Error setting static field: " + e.getMessage());
        }
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        XposedHelpers.setIntField(obj, fieldName, value);
    }

    public static void setIntField(
            Object clazz,
            ClassLoader classLoader,
            Object obj,
            String fieldName,
            int value) {

        try {
            Class<?> actualClass = getClass(clazz, classLoader);
            if (actualClass != null && actualClass.isInstance(obj)) {
                XposedHelpers.setIntField(obj, fieldName, value);
            } else {
                XposedBridge.log("setIntField - Object is not an instance of " + clazz);
            }

        } catch (Throwable e) {
            XposedBridge.log("setIntField - Error setting int field: " + e.getMessage());
        }
    }

    public static String getFormattedStackTrace() {
        StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
        StringBuilder stackTrace = new StringBuilder("Stack Trace:\n");

        for (int i = 3; i < stackElements.length; i++) {
            StackTraceElement element = stackElements[i];

            String className = element.getClassName();
            String methodName = element.getMethodName();
            int lineNumber = element.getLineNumber();

            stackTrace.append("  ")
                    .append(className)
                    .append(".")
                    .append(methodName)
                    .append("(line: ")
                    .append(lineNumber)
                    .append(")\n");
        }

        return stackTrace.toString();
    }
}

