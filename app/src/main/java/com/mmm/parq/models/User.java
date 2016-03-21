package com.mmm.parq.models;

import java.util.HashMap;

public class User {
    private String id;
    private HashMap attributes;

    public User(String id, HashMap attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getAttribute(String key)  {
        return attributes.get(key).toString();
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public String getId() {
       return id;
    }
}
