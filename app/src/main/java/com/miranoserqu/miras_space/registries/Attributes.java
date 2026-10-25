package com.miranoserqu.miras_space.registries;

import com.miranoserqu.miras_space.tag.Attribute.DoubleAttribute;

import java.util.function.Supplier;

public class Attributes {

    public static final Supplier<DoubleAttribute> MAX_SPEED_SQUARED = () -> new DoubleAttribute("max_forward_speed");
    public static final Supplier<DoubleAttribute> FORWARD_ACCELERATION = () -> new DoubleAttribute("forward_acceleration");
    public static final Supplier<DoubleAttribute> REVERSE_ACCELERATION = () -> new DoubleAttribute("reverse_acceleration");
    public static final Supplier<DoubleAttribute> LATERAL_SPEED = () -> new DoubleAttribute("lateral_acceleration");
    public static final Supplier<DoubleAttribute> ROTATION_SPEED = () -> new DoubleAttribute("rotation_speed");

}
