package com.svgs.server;

import java.util.ArrayList;

import com.svgs.game_model.User;

public class Manager {
    private static ArrayList<User> users = new ArrayList<>();
    
    public static User createUser() {
        String uid = Helper.generateId(8);
        String name = Names.getRandomName();
        while (uidExists(uid)) uid = Helper.generateId(8);
        while (nameExists(name)) name = Names.getRandomName();
        User result = new User(uid, name);
        users.add(result);
        return result;
    }

    public static boolean uidExists(String uid) {
        return users.stream().map(u -> u.getUid()).filter(s -> s.equals(uid)).toArray(String[]::new).length != 0;
    }
    public static boolean nameExists(String name) {
        return users.stream().map(u -> u.getName()).filter(s -> s.equals(name)).toArray(String[]::new).length != 0;
    }
}
