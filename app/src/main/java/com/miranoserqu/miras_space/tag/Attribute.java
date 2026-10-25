package com.miranoserqu.miras_space.tag;

import java.util.List;

public abstract class Attribute<A extends Number> {

    private final String id;

    public Attribute(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    abstract A getValue(List<AttributeModifier> attributeModifiers);

    public class IntAttribute extends Attribute<Integer> {

        public IntAttribute(String id) {
            super(id);
        }

        @Override
        public Integer getValue(List<AttributeModifier> attributeModifiers){
            double multBase = 1;
            double mult = 1;
            int base = 0;
            int f = 0;
            for(AttributeModifier attributeModifier : attributeModifiers){
                if(attributeModifier.getAttribute().equals(this)){
                    switch(attributeModifier.getType()){
                        case MULT_BASE -> multBase += attributeModifier.getValue();
                        case ADD_BASE -> base += (int) attributeModifier.getValue();
                        case MULT -> mult *= attributeModifier.getValue();
                        case ADD_FINAL -> f += (int) attributeModifier.getValue();
                    }
                }
            }
            return (int) (((base * multBase) + f) * mult);
        }

    }

    public static class DoubleAttribute extends Attribute<Double> {

        public DoubleAttribute(String id) {
            super(id);
        }

        @Override
        public Double getValue(List<AttributeModifier> attributeModifiers){
            double multBase = 1;
            double mult = 1;
            double base = 0;
            double f = 0;
            for(AttributeModifier attributeModifier : attributeModifiers){
                if(attributeModifier.getAttribute().equals(this)){
                    switch (attributeModifier.getType()) {
                        case MULT_BASE -> multBase += attributeModifier.getValue();
                        case ADD_BASE -> base += attributeModifier.getValue();
                        case MULT -> mult *= attributeModifier.getValue();
                        case ADD_FINAL -> f += attributeModifier.getValue();
                    }
                }
            }
            return ((base * multBase) + f) * mult;
        }

    }

    public class FloatAttribute extends Attribute<Float> {

        public FloatAttribute(String id) {
            super(id);
        }

        @Override
        public Float getValue(List<AttributeModifier> attributeModifiers){
            double multBase = 1;
            double mult = 1;
            float base = 0;
            float f = 0;
            for(AttributeModifier attributeModifier : attributeModifiers){
                if(attributeModifier.getAttribute().equals(this)){
                    switch (attributeModifier.getType()) {
                        case MULT_BASE -> multBase += attributeModifier.getValue();
                        case ADD_BASE -> base += (float) attributeModifier.getValue();
                        case MULT -> mult *= attributeModifier.getValue();
                        case ADD_FINAL -> f += (float) attributeModifier.getValue();
                    }
                }
            }
            return (float) (((base * multBase) + f) * mult);
        }

    }

    public boolean equals(Attribute other){
        return this.id.equals(other.id);
    }

}
