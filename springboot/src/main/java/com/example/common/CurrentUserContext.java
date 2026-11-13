package com.example.common;

import com.example.entity.User;

public class CurrentUserContext {

    private static final ThreadLocal<User> HOLDER = new InheritableThreadLocal<>();

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static Integer getUserId() {
        User user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static String getRole() {
        User user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
