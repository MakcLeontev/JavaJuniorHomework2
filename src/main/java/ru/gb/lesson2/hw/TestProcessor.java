package ru.gb.lesson2.hw;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestProcessor {

  /**
   * Данный метод находит все void методы без аргументов в классе, и запускеет их.
   * <p>
   * Для запуска создается тестовый объект с помощью конструткора без аргументов.
   */
  public static void runTest(Class<?> testClass) {
    final Constructor<?> declaredConstructor;
    try {
      declaredConstructor = testClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
    }

    final Object testObj;
    try {
      testObj = declaredConstructor.newInstance();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
    }

    List<Method> methods;
    List<Method> beforeMethods = new ArrayList<>();
    List<Method> afterMethods = new ArrayList<>();
    List<Method> normalMethods = new ArrayList<>();
    for (Method method : testClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Test.class)) {
        checkTestMethod(method);
        normalMethods.add(method);
      }
      if (method.isAnnotationPresent((BeforeEach.class))) {
        checkTestMethod(method);
        beforeMethods.add(method);
      }
      if (method.isAnnotationPresent((AfterEach.class))) {
        checkTestMethod(method);
        afterMethods.add(method);
      }
    }

    List<Method>filterList = normalMethods.stream().sorted(Comparator.comparingInt(a -> a.getAnnotation(Test.class).order())).toList();
    List<Method> intermediate = Stream.concat(beforeMethods.stream(),filterList.stream()).collect(Collectors.toList());
    methods = Stream.concat(intermediate.stream(),afterMethods.stream()).collect(Collectors.toList());
    methods.forEach(it -> runTest(it, testObj));
  }

  private static void checkTestMethod(Method method) {
    if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
    }
  }

  private static void runTest(Method testMethod, Object testObj) {
    try {
      testMethod.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
    } catch (AssertionError e) {

    }
  }

}
