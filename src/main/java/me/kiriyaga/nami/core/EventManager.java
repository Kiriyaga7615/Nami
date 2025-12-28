package me.kiriyaga.nami.core;

import me.kiriyaga.nami.event.Event;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static me.kiriyaga.nami.Nami.LOGGER;

public class EventManager {

    private final Map<Class<? extends Event>, List<ListenerMethod>> listeners = new ConcurrentHashMap<>();

    private static class ListenerMethod implements Comparable<ListenerMethod> {
        private final Object target;
        private final Method method;
        private final EventPriority priority;
        private final Consumer<Object> invoker;

        public ListenerMethod(Object target, Method method, EventPriority priority) throws Throwable {
            this.target = target;
            this.method = method;
            this.priority = priority;
            this.invoker = ReflectionUtils.wrapConsumer(target, method);
            this.method.setAccessible(true);
        }

        public void invoke(Event event) throws Exception {
            invoker.accept(event);
        }

        public EventPriority getPriority() {
            return priority;
        }

        @Override
        public int compareTo(ListenerMethod o) {
            return o.priority.ordinal() - this.priority.ordinal();
        }
    }

    public void register(Object listenerObject) {
        for (Method method : listenerObject.getClass().getDeclaredMethods()) {
            if (!method.isAnnotationPresent(SubscribeEvent.class)) continue;
            if (method.getParameterCount() != 1) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) continue;

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;

            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            EventPriority priority = annotation.priority();

            try {
                ListenerMethod listenerMethod = new ListenerMethod(listenerObject, method, priority);
                listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(listenerMethod);
                listeners.get(eventClass).sort(Comparator.naturalOrder());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unregister(Object listenerObject) {
        for (List<ListenerMethod> list : listeners.values()) {
            list.removeIf(listener -> listener.target == listenerObject);
        }
    }

    public void post(Event event) {
        Class<?> clazz = event.getClass();

        while (clazz != null && Event.class.isAssignableFrom(clazz)) {
            List<ListenerMethod> lst = listeners.get(clazz);
            if (lst != null) {
                for (ListenerMethod listener : lst) {
                    try {
                        listener.invoke(event);
                        if (event.isCancelled()) return;
                    } catch (Exception e) {
                        LOGGER.error("Error invoking event listener: ", e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}
