package com.miranoserqu.miras_space.registries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class Registry<J> {

    private final Map<String, J> reg = new HashMap<>();
    private final String location;

    public Registry(String location){
        this.location = location;
    }

    public Supplier<J> register(String name, Supplier<J> supplier){
        this.reg.put(name, supplier.get());
        return supplier;
    }

}
