package com.miranoserqu.miras_space.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Sendable {
    int id();
    Target target();
    public static enum Target {
        SERVER,
        CLIENT
    }
}
