package com.morocco.immovable;

import java.sql.Connection;
import java.sql.DriverManager;

public class Database {
    public static Connection connection = null;

    public static void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://" + Settings.dbHost + ":" + Settings.dbPort + "/" + Settings.dbName, Settings.dbUser, Settings.dbPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (connection == null) ;
    }
}
