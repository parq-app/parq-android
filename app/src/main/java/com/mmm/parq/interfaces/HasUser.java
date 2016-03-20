package com.mmm.parq.interfaces;

import com.mmm.parq.models.User;

import java.util.concurrent.Future;

public interface HasUser {
    Future<User> getUser();
}
