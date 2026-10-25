package com.miranoserqu.miras_space.tag;

import java.util.HashMap;
import java.util.Map;

public class Tag {

    Map<String, Object> objects = new HashMap<>();

    public Tag(){

    }

    public String saveJSON(){
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        for(String object : this.objects.keySet()){
            json.append("    \"").append(object).append("\": {\n        \"");
            Object o = objects.get(object);
            if(o instanceof String str){
                json.append("string\": ").append(str);
            }
            if(o instanceof Double d){
                json.append("double\": ").append(d);
            }
            if(o instanceof Integer i){
                json.append("int\": ").append(i);
            }
            if(o instanceof Long l){
                json.append("long\": ").append(l);
            }
            if(o instanceof Float f){
                json.append("float\": ").append(f);
            }
            if(o instanceof Tag tag){
                json.append("tag\": ").append(tag.saveJSON());
            }
            json.append("    \n}");
        }
        return json.toString();
    }

    public void putString(String key, String value){
        this.objects.put(key, value);
    }

    public void putDouble(String key, Double value){
        this.objects.put(key, value);
    }

    public void putInt(String key, Integer value){
        this.objects.put(key, value);
    }

    public void putLong(String key, Long value){
        this.objects.put(key, value);
    }

    public void putFloat(String key, Float value){
        this.objects.put(key, value);
    }

    public void put(String key, Tag value){
        this.objects.put(key, value);
    }

    public String getString(String key){
        if(this.objects.get(key) instanceof String str){
            return str;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Double getDouble(String key){
        if(this.objects.get(key) instanceof Double d){
            return d;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Integer getInt(String key){
        if(this.objects.get(key) instanceof Integer i){
            return i;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Long getLong(String key){
        if(this.objects.get(key) instanceof Long l){
            return l;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Float getFloat(String key){
        if(this.objects.get(key) instanceof Float f){
            return f;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Tag get(String key){
        if(this.objects.get(key) instanceof Tag tag){
            return tag;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public String getString(String key, String def){
        if(this.objects.getOrDefault(key, def) instanceof String str){
            return str;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Double getDouble(String key, Double def){
        if(this.objects.getOrDefault(key, def) instanceof Double d){
            return d;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Integer getInt(String key, Integer def){
        if(this.objects.getOrDefault(key, def) instanceof Integer i){
            return i;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Long getLong(String key, Long def){
        if(this.objects.getOrDefault(key, def) instanceof Long l){
            return l;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Float getFloat(String key, Float def){
        if(this.objects.getOrDefault(key, def) instanceof Float f){
            return f;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public Tag get(String key, Tag def){
        if(this.objects.getOrDefault(key, def) instanceof Tag tag){
            return tag;
        }
        throw new IllegalArgumentException("Value associated to key: " + key + " is no the correct type.");
    }

    public void remove(String key){
        this.objects.remove(key);
    }

}
