package com.mmm.parq.models;

import java.io.Serializable;
import java.util.HashMap;

public class Spot implements Serializable {
    private String id;
    private HashMap attributes;

    public Spot(String id, HashMap attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getAttribute(String key) {
        return attributes.get(key).toString();
    }

    public String getId() {
        return id;
    }
}
