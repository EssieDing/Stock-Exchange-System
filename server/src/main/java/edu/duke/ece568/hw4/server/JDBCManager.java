package edu.duke.ece568.hw4.server;

import java.sql.*;
import java.util.Properties;

public class JDBCManager {
    private Connection connection = null;

    public JDBCManager(String serverUrl, String serverPort, String dbName, String user, String pwd)
            throws SQLException, ClassNotFoundException {
        this.getConnection(serverUrl, serverPort, dbName, user, pwd);
        this.createAccountTable();
        this.createPositionTable();
        this.createOrderTable();
        this.createArchiveTable();
        // this.conn.close();
    }

    private Connection getConnection(String serverUrl, String serverPort, String dbName, String user, String pwd) throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        Properties connectionProps = new Properties();
        connectionProps.put("user", user);
        connectionProps.put("password", pwd);
        this.connection = DriverManager.getConnection("jdbc:postgresql://" + serverUrl + ":" + serverPort + "/" + dbName,
                user,pwd);

        return this.connection;
    }

    public Connection getConnection() {
        return this.connection;
    }

    private void createAccountTable() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS ACCOUNT"
                + "(ACCOUNT_NUMBER INT PRIMARY KEY,"
                + "BALANCE FLOAT NOT NULL CHECK (BALANCE >= 0));";
        this.updateDML(query);
    }

    private void createPositionTable() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS POSITION"
                + "(POSITION_ID SERIAL PRIMARY KEY,"
                + "SYMBOL VARCHAR (255) NOT NULL,"
                + "ACCOUNT_NUMBER INT NOT NULL,"
                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT <> 0),"
                + "UNIQUE (ACCOUNT_NUMBER, SYMBOL),"
                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
        this.updateDML(query);
    }

    private void createOrderTable() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS ORDERS"
                + "(ORDER_ID SERIAL PRIMARY KEY,"
                + "SYMBOL VARCHAR (255) NOT NULL,"
                + "ACCOUNT_NUMBER INT NOT NULL,"
                + "ORDER_STATUS VARCHAR (255) NOT NULL,"
                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT <> 0),"
                + "LIMIT_PRICE FLOAT NOT NULL CHECK (LIMIT_PRICE > 0),"
                + "ISSUE_TIME TIMESTAMP NOT NULL, "
                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
        this.updateDML(query);
    }

    private void createArchiveTable() throws SQLException {
        String query = "CREATE TABLE IF NOT EXISTS ARCHIVE"
                + "(ARCHIVE_ID SERIAL PRIMARY KEY,"
                + "ORDER_ID INT NOT NULL,"
                + "SYMBOL VARCHAR (255) NOT NULL,"
                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT <> 0), "
                + "LIMIT_PRICE FLOAT NOT NULL CHECK (LIMIT_PRICE > 0),"
                + "ISSUE_TIME TIMESTAMP NOT NULL);";
        this.updateDML(query);
    }

    public void deleteAll() throws SQLException {
        String query = "DELETE FROM ARCHIVE; DELETE FROM ORDERS; DELETE FROM POSITION; DELETE FROM ACCOUNT;";
        PreparedStatement statement = this.connection.prepareStatement(query);
        statement.executeUpdate();
    }

    // data manipulation language for update
    public synchronized void updateDML(String sql) throws SQLException {
        Statement state = this.connection.createStatement();
        state.executeUpdate(sql);
        state.close();
    }

    // data query language for query
    public synchronized ResultSet queryDQL(String sql) throws SQLException {
        Statement state = this.connection.createStatement();
        ResultSet res = state.executeQuery(sql);
        state.close();
        return res;
    }



}

