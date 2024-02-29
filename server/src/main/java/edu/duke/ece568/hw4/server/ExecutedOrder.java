package edu.duke.ece568.hw4.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ExecutedOrder {
    private int orderId;
    private int archiveId;
    private String symbol;
    private double amount;
    private double limitPrice;

    private Timestamp time;
    private JDBCManager jdbcManager;

    public ExecutedOrder(int orderId, int archiveId, String symbol, double amount, double limitPrice, Timestamp time, JDBCManager jdbcManager) {
        if (amount == 0) {
            throw new IllegalArgumentException("Error: Executed order amount cannot be zero!");
        }
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Error: Executed order limit price must be positive");
        }
        this.orderId = orderId;
        this.archiveId = archiveId;
        this.symbol = symbol;
        this.amount = amount;
        this.limitPrice = limitPrice;
        this.time = time;
        this.jdbcManager = jdbcManager;
    }

    public int getOrderId(){
        return this.orderId;
    }

    public int getArchiveId() throws SQLException{
       return this.archiveId;
    }

    public double getLimitPrice() throws SQLException {
        return this.limitPrice;
    }

    public double getAmount() throws SQLException{
        return this.amount;
    }

    public Timestamp getTime() throws SQLException{
        return this.time;
    }

    public ExecutedOrder(int orderId, String symbol, double amount, double limitPrice, Timestamp time, JDBCManager jdbcManager) {
        this(orderId, -1, symbol, amount, limitPrice, time, jdbcManager);
    }

    public void saveExecutedOrder() throws SQLException {
        String query =
            "WITH TEMP AS ( " +
                "INSERT INTO ARCHIVE (ORDER_ID, SYMBOL, AMOUNT, LIMIT_PRICE, ISSUE_TIME)" +
                "VALUES (?, ?, ?, ?, ?)" +
                "RETURNING ARCHIVE_ID" +
            ")" +
            "SELECT ARCHIVE_ID FROM TEMP";
        PreparedStatement preparedStatement = jdbcManager.getConnection().prepareStatement(query);
        preparedStatement.setInt(1, orderId);
        preparedStatement.setString(2, symbol);
        preparedStatement.setDouble(3, amount);
        preparedStatement.setDouble(4, limitPrice);
        preparedStatement.setTimestamp(5, time);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (!resultSet.next()) {
            throw new IllegalArgumentException("Error: Failed to save executed order to DB!");
        }
        archiveId = resultSet.getInt("ARCHIVE_ID");
    }

    @Override
    public String toString() {
        return "Executed Order " + orderId + " archive id " + archiveId +  ": " + amount + " " + symbol + " @ $" + limitPrice + " @ " + time;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(getClass())) {
            ExecutedOrder otherOrder = (ExecutedOrder) other;
            boolean hasSameId = otherOrder.orderId == orderId && otherOrder.archiveId == archiveId;
            boolean hasSameSymbol = otherOrder.symbol.equals(symbol);
            boolean hasSameAmount = Double.compare(otherOrder.amount, amount) == 0;
            boolean hasSameLimitPrice = Double.compare(otherOrder.limitPrice, limitPrice) == 0;
            boolean hasSameTime = otherOrder.time.getTime() == time.getTime();
            if (hasSameId && hasSameSymbol && hasSameAmount && hasSameLimitPrice && hasSameTime) {
                return true;
            }
            return false;
        }
        return false;
    }

}
