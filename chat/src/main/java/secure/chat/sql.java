package secure.chat;

import java.sql.*;

import com.mysql.cj.jdbc.Driver;

public class sql {

    private static Connection connect;

    public static synchronized Connection connect() {
        return connect;
    }

    public static void connection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void ConnectingToSQL() {

        connection();
        String host = "jdbc:mysql://localhost:3306/?autoReconnect=true&useSSL=false";
        String username = "root";
        String password = "projetcrypto";

        try {
            connect = (Connection) DriverManager.getConnection(host, username, password);
            System.out.println("connected to database\n");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    sql() {
        connection();
        ConnectingToSQL();
    }
}
