package com.miranoserqu.miras_space.resource;

import com.miranoserqu.miras_space.Option;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Translatable {

    String location;

    private Translatable(){

    }

    String get(){
        return translations.get(this.location);
    }

    public static Translatable ofLocation(String location){
        Translatable t = new Translatable();
        t.location = location;
        return t;
    }

    public static Translatable ofNamespaceAndLocation(String namespace, String location){
        Translatable t = new Translatable();
        t.location = namespace.concat("/").concat(location);
        return t;
    }

    public static Translatable ofNamespaceAndLocation(String location){
        Translatable t = new Translatable();
        t.location = "miras_space/".concat(location);
        return t;
    }

    private static final Map<String, String> translations = new HashMap<>();

    public static void load(){
        File file = new File(Option.LANG.get().getValue());
        JSONObject json = new JSONObject(file);
        for(String key : json.keySet()){
            translations.putIfAbsent(key, json.getString(key));
        }
    }

    public static void load(URI location){
        File file = new File(location);
        JSONObject json = new JSONObject(file);
        for(String key : json.keySet()){
            translations.putIfAbsent(key, json.getString(key));
        }
    }

}
