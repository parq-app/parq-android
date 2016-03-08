package com.mmm.parq.models;

import java.io.Serializable;
import java.util.HashMap;

public class Reservation implements Serializable {
    private String id;
    private HashMap attributes;

    public Reservation(String id, HashMap attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getAttribute(String key)  {
        return attributes.get(key).toString();
    }
}
