package com.miranoserqu.miras_space.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private final Map<Class<?>, List<Subscriber>> subscribers = new HashMap<>();

    private static class Subscriber {
        final Object instance;
        final Method method;

        public Subscriber(Object instance, Method method){
            this.instance = instance;
            this.method = method;
        }
    }

    public void register(Object listener){
        Class<?> clazz = listener.getClass();

        for(Method method : clazz.getDeclaredMethods()){
            if(method.isAnnotationPresent(SubscribeEvent.class)){
                Class<?>[] params = method.getParameterTypes();

                if(params.length != 1){
                    throw new IllegalArgumentException("the number of arguments for method " + method.getName() + " in class" + clazz.getName() + " was expected to be 1 but found " + params.length);
                }

                Class<?> eventType = params[0];
                method.setAccessible(true);

                subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(new Subscriber(listener, method));
            }
        }
    }

    public void post(Object event){
        List<Subscriber> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers == null) return;

        for (Subscriber sub : eventSubscribers) {
            try {
                sub.method.invoke(sub.instance, event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
