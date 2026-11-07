package com.miranoserqu.miras_space.event;

import javax.management.ConstructorParameters;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventHandler {

    Bus bus();

    enum Bus {
        CLIENT,
        SERVER
    }

}
