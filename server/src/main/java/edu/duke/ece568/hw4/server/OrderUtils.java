package edu.duke.ece568.hw4.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class OrderUtils {

    public static List<Order> findOrdersByIdAndStatus(JDBCManager jdbcManager, int orderId, String status) throws SQLException {
        String query = "SELECT * FROM ORDERS WHERE ORDER_ID=? AND ORDER_STATUS=?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, orderId);
        statement.setString(2, status.toUpperCase());

        List<Order> orders = new ArrayList<>();
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            int id = resultSet.getInt("ORDER_ID");
            String symbol = resultSet.getString("SYMBOL");
            double amount = resultSet.getDouble("AMOUNT");
            double limitPrice = resultSet.getDouble("LIMIT_PRICE");
            int accountId = resultSet.getInt("ACCOUNT_NUMBER");
            String status_ = resultSet.getString("ORDER_STATUS");
            Timestamp time = resultSet.getTimestamp("ISSUE_TIME");
            Order order = new Order(id, symbol, amount, limitPrice, accountId, status_, time, jdbcManager);
            orders.add(order);
        }
        return orders;
    }

    public static Order findTopMatchForBuyOrder(JDBCManager jdbcManager, int accountId, String symbol, double limitPrice) throws SQLException {
        String query = "SELECT * FROM ORDERS " +
                "WHERE ACCOUNT_NUMBER <> ? " +
                "AND SYMBOL = ? " +
                "AND AMOUNT < 0 " +
                "AND LIMIT_PRICE <= ? " +
                "AND ORDER_STATUS = ? " +
                "ORDER BY LIMIT_PRICE ASC, ISSUE_TIME ASC " +
                "LIMIT 1";

        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, accountId);
        statement.setString(2, symbol);
        statement.setDouble(3, limitPrice);
        statement.setString(4, "OPEN");

        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            return null;
        }

        int id = resultSet.getInt("ORDER_ID");
        String symbol_ = resultSet.getString("SYMBOL");
        double amount = resultSet.getDouble("AMOUNT");
        double limitPrice_ = resultSet.getDouble("LIMIT_PRICE");
        int accountId_ = resultSet.getInt("ACCOUNT_NUMBER");
        String status = resultSet.getString("ORDER_STATUS");
        Timestamp time = resultSet.getTimestamp("ISSUE_TIME");
        return new Order(id, symbol_, amount, limitPrice_, accountId_, status, time, jdbcManager);
    }

    public static Order findTopMatchForSellOrder(JDBCManager jdbcManager, int accountId, String symbol, double limitPrice) throws SQLException {
        String query = "SELECT * FROM ORDERS " +
                "WHERE ACCOUNT_NUMBER <> ? " +
                "AND SYMBOL = ? " +
                "AND AMOUNT > 0 " +
                "AND LIMIT_PRICE >= ? " +
                "AND ORDER_STATUS = ? " +
                "ORDER BY LIMIT_PRICE DESC, ISSUE_TIME ASC " +
                "LIMIT 1";

        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, accountId);
        statement.setString(2, symbol);
        statement.setDouble(3, limitPrice);
        statement.setString(4, "OPEN");

        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            return null;
        }

        int id = resultSet.getInt("ORDER_ID");
        String symbol_ = resultSet.getString("SYMBOL");
        double amount = resultSet.getDouble("AMOUNT");
        double limitPrice_ = resultSet.getDouble("LIMIT_PRICE");
        int accountId_ = resultSet.getInt("ACCOUNT_NUMBER");
        String status = resultSet.getString("ORDER_STATUS");
        Timestamp time = resultSet.getTimestamp("ISSUE_TIME");
        return new Order(id, symbol_, amount, limitPrice_, accountId_, status, time, jdbcManager);
    }

    public static List<ExecutedOrder> findExecutedOrdersById(JDBCManager jdbcManager, int orderId) throws SQLException {
        String query = "SELECT * FROM ARCHIVE WHERE ORDER_ID=?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, orderId);

        List<ExecutedOrder> executedOrders = new ArrayList<>();
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            int orderId_ = resultSet.getInt("ORDER_ID");
            int archiveId = resultSet.getInt("ARCHIVE_ID");
            String symbol = resultSet.getString("SYMBOL");
            double amount = resultSet.getDouble("AMOUNT");
            double limitPrice = resultSet.getDouble("LIMIT_PRICE");
            Timestamp time = resultSet.getTimestamp("ISSUE_TIME");
            ExecutedOrder executedOrder = new ExecutedOrder(orderId_, archiveId, symbol, amount, limitPrice, time, jdbcManager);
            executedOrders.add(executedOrder);
        }
        return executedOrders;
    }

}