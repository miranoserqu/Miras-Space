package com.miranoserqu.miras_space.event;

import javax.management.ConstructorParameters;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {

    Bus bus();

    enum Bus {
        CLIENT,
        SERVER
    }

}
