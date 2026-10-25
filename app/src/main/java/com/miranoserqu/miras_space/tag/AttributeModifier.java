package com.miranoserqu.miras_space.tag;

import java.util.UUID;

public class AttributeModifier {

    private final Attribute attribute;
    private final UUID uuid;
    private final String name;
    private final ModifierType type;
    private final double value;

    public AttributeModifier(Attribute attribute, UUID uuid, String name, ModifierType type, double value){
        this.attribute = attribute;
        this.uuid = uuid;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getValue() {
        return value;
    }

    public ModifierType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void save(Tag tag){
        Tag t = new Tag();
        t.putDouble("value", this.value);
        t.putString("uuid", uuid.toString());
        t.putString("attribute", this.attribute.getId());
        t.putInt("type", this.type.ordinal());
        tag.put(this.name, t);
    }

    public enum ModifierType {
        ADD_BASE,
        MULT_BASE,
        MULT,
        ADD_FINAL
    }

}
