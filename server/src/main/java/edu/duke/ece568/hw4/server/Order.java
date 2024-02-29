package edu.duke.ece568.hw4.server;

import java.security.InvalidAlgorithmParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class Order {
    private int id;
    private String symbol;
    private double amount;
    private double limitPrice;
    private int accountId;
    private String status;
    private Timestamp time;
    private JDBCManager jdbcManager;

    public Order(int id, String symbol, double amount, double limitPrice, int accountId, String status, Timestamp time, JDBCManager jdbcManager) {
        if (amount == 0) {
            throw new IllegalArgumentException("Error: Order amount cannot be zero!");
        }
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Error: Order limit price must be positive");
        }
        this.id = id;
        this.symbol = symbol;
        this.amount = amount;
        this.limitPrice = limitPrice;
        this.accountId = accountId;
        this.status = status;
        this.time = time;
        this.jdbcManager = jdbcManager;
    }

    public Order(String symbol, double amount, double limitPrice, int accountId, JDBCManager jdbcManager) {
        this(-1, symbol, amount, limitPrice, accountId, "OPEN", Timestamp.from(Instant.now()), jdbcManager);
    }

    public Order(JDBCManager jdbcManager, int id) throws SQLException {
        this.jdbcManager = jdbcManager;
        queryOrder(id);
    }

    private void queryOrder(int id) throws SQLException {
        String query = "SELECT * FROM ORDERS WHERE ORDER_ID = ?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            throw new IllegalArgumentException("Error: Cannot find order with ID " + id);
        }
        this.id = resultSet.getInt("ORDER_ID");
        this.symbol = resultSet.getString("SYMBOL");
        this.amount = resultSet.getDouble("AMOUNT");
        this.limitPrice = resultSet.getDouble("LIMIT_PRICE");
        this.accountId = resultSet.getInt("ACCOUNT_NUMBER");
        this.time = resultSet.getTimestamp("ISSUE_TIME");
        this.status = resultSet.getString("ORDER_STATUS");
    }

    public void updateAmount(double num) throws SQLException {
        if (id < 0) {
            throw new IllegalArgumentException("Error: Cannot update because order has not been saved in DB yet!");
        }
        queryOrder(id);
        double amountUpdated = amount + num;
        String query = "UPDATE ORDERS SET AMOUNT = ? WHERE ORDER_ID = ?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setDouble(1, amountUpdated);
        statement.setInt(2, id);
        statement.executeUpdate();
    }

    public void saveOrder() throws SQLException {
        if (id >= 1) {
            throw new IllegalArgumentException("Error: Order has already been saved!");
        }
        String query =
            "WITH TEMP AS ( " +
                "INSERT INTO ORDERS (SYMBOL, ACCOUNT_NUMBER, ORDER_STATUS, AMOUNT, LIMIT_PRICE, ISSUE_TIME) " +
                "VALUES(?, ?, ?, ?, ?, ?) " +
                "RETURNING ORDER_ID" +
            ")" +
            "SELECT ORDER_ID FROM TEMP;";
        PreparedStatement preparedStatement = this.jdbcManager.getConnection().prepareStatement(query);
        preparedStatement.setString(1, symbol);
        preparedStatement.setInt(2, accountId);
        preparedStatement.setString(3, status);
        preparedStatement.setDouble(4, amount);
        preparedStatement.setDouble(5, limitPrice);
        preparedStatement.setTimestamp(6, time);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (!resultSet.next()) {
            throw new IllegalArgumentException("Error: Failed to save order to DB!");
        }
        id = resultSet.getInt("ORDER_ID");
    }

    public void cancelOrder() throws SQLException {
        if (id < 0) {
            throw new IllegalArgumentException("Error: Cannot cancel because order has not been saved in DB yet!");
        }
        queryOrder(id);
        if (status.equals("CANCELED")) {
            throw new IllegalArgumentException("Error: Order has already been canceled!");
        }
        String query = "UPDATE ORDERS SET ORDER_STATUS=?, ISSUE_TIME=? WHERE ORDER_ID=? AND ORDER_STATUS <> 'CANCELED'";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setString(1, "CANCELED");
        statement.setTimestamp(2, Timestamp.from(Instant.now()));
        statement.setInt(3, id);
        statement.executeUpdate();
        if (amount > 0) {
            // for buy orders
            Account account = new Account(accountId, jdbcManager);
            account.tryUpdateBalance(amount * limitPrice);
        } else {
            // for sell orders
            Position position = new Position(symbol, Math.abs(amount), accountId, jdbcManager);
            position.savePosition();
        }
    }

    public void matchOrder() throws SQLException {
        if (id < 0) {
            throw new IllegalArgumentException("Error: Cannot match because order has not been saved in DB yet!");
        }
        queryOrder(id);
        if (amount > 0) {
            matchForBuyOrder();
        } else {
            matchForSellOrder();
        }
    }

    protected void matchForBuyOrder() throws SQLException {
        Order sellOrder = OrderUtils.findTopMatchForBuyOrder(jdbcManager, accountId, symbol, limitPrice);
        while (sellOrder != null) {
            Account seller = new Account(sellOrder.accountId, jdbcManager);
            Account buyer = new Account(accountId, jdbcManager);
            if (amount > Math.abs(sellOrder.amount)) {
                sellOrder = matchBuyMore(sellOrder, buyer, seller);
            } else if (amount == Math.abs(sellOrder.amount)) {
                matchBuyExact(sellOrder, buyer, seller);
                break;
            } else {
                matchBuyLess(sellOrder, buyer, seller);
                break;
            }
        }
    }

    private Order matchBuyMore(Order sellOrder, Account buyer, Account seller) throws SQLException {
        double totalPrice = Math.abs(sellOrder.amount * sellOrder.limitPrice);
        double totalDiffPrice = Math.abs((limitPrice - sellOrder.limitPrice) * sellOrder.amount);
        seller.tryUpdateBalance(totalPrice);
        buyer.tryUpdateBalance(totalDiffPrice);
        Position position = new Position(symbol, Math.abs(sellOrder.amount), accountId, jdbcManager);
        position.savePosition();
        sellOrder.archive(sellOrder.limitPrice);
        this.archive(sellOrder.amount, sellOrder.limitPrice);
        sellOrder = OrderUtils.findTopMatchForBuyOrder(jdbcManager, accountId, symbol, limitPrice);
        return sellOrder;
    }

    private void matchBuyExact(Order sellOrder, Account buyer, Account seller) throws SQLException {
        double totalPrice = Math.abs(sellOrder.amount * sellOrder.limitPrice);
        double totalDiffPrice = Math.abs((limitPrice - sellOrder.limitPrice) * sellOrder.amount);
        seller.tryUpdateBalance(totalPrice);
        buyer.tryUpdateBalance(totalDiffPrice);
        Position position = new Position(symbol, Math.abs(sellOrder.amount), accountId, jdbcManager);
        position.savePosition();
        sellOrder.archive(sellOrder.limitPrice);
        this.archive(sellOrder.limitPrice);
    }

    private void matchBuyLess(Order sellOrder, Account buyer, Account seller) throws SQLException {
        double totalPrice = Math.abs(amount * sellOrder.limitPrice);
        seller.tryUpdateBalance(totalPrice);
        buyer.tryUpdateBalance(Math.abs((limitPrice - sellOrder.limitPrice) * amount));
        Position position = new Position(symbol, Math.abs(amount), accountId, jdbcManager);
        position.savePosition();
        this.archive(sellOrder.limitPrice);
        sellOrder.archive(this.amount, sellOrder.limitPrice);
    }

    protected void matchForSellOrder() throws SQLException {
        Order buyOrder = OrderUtils.findTopMatchForSellOrder(jdbcManager, accountId, symbol, limitPrice);
        while (buyOrder != null) {
            Account seller = new Account(accountId, jdbcManager);
            double matchedLimitPrice = buyOrder.limitPrice;
            if (Math.abs(this.amount) > buyOrder.amount) {
                buyOrder = matchSellMore(buyOrder, seller, matchedLimitPrice);
            } else if (Math.abs(this.amount) == buyOrder.amount) {
                matchSellExact(buyOrder, seller, matchedLimitPrice);
                break;
            } else {
                matchSellLess(buyOrder, seller, matchedLimitPrice);
                break;
            }
        }
    }

    private Order matchSellMore(Order buyOrder, Account seller, double matchedLimitPrice) throws SQLException {
        double totalPrice = Math.abs(buyOrder.limitPrice * buyOrder.amount);
        seller.tryUpdateBalance(totalPrice);
        Position position = new Position(symbol, Math.abs(buyOrder.amount), buyOrder.accountId, jdbcManager);
        position.savePosition();
        buyOrder.archive(matchedLimitPrice);
        this.archive(buyOrder.amount, matchedLimitPrice);
        buyOrder = OrderUtils.findTopMatchForSellOrder(jdbcManager, accountId, symbol, limitPrice);
        return buyOrder;
    }

    private void matchSellExact(Order buyOrder, Account seller, double matchedLimitPrice) throws SQLException {
        double totalPrice = Math.abs(buyOrder.limitPrice * buyOrder.amount);
        seller.tryUpdateBalance(totalPrice);
        Position position = new Position(symbol, Math.abs(buyOrder.amount), buyOrder.accountId, jdbcManager);
        position.savePosition();
        this.archive(matchedLimitPrice);
        buyOrder.archive(matchedLimitPrice);
    }

    private void matchSellLess(Order buyOrder, Account seller, double matchedLimitPrice) throws SQLException {
        double totalPrice = Math.abs(buyOrder.limitPrice * this.amount);
        seller.tryUpdateBalance(totalPrice);
        Position position = new Position(symbol, Math.abs(amount), buyOrder.accountId, jdbcManager);
        position.savePosition();
        this.archive(matchedLimitPrice);
        buyOrder.archive(this.amount, matchedLimitPrice);
    }

    public void archive(double price) throws SQLException {
        ExecutedOrder executedOrder = new ExecutedOrder(id, symbol, amount, price, time, jdbcManager);
        executedOrder.saveExecutedOrder();
        deleteOrder();
    }

    public void archive(double orderAmount, double price) throws SQLException{
        ExecutedOrder executedOrder = new ExecutedOrder(id, symbol, -1 * orderAmount, price, time, jdbcManager);
        executedOrder.saveExecutedOrder();
        updateAmount(orderAmount);
    }

    public void deleteOrder() throws SQLException {
        if (id < 0) {
            throw new IllegalArgumentException("Error: Cannot delete because order has not been saved in DB yet!");
        }
        String query = "DELETE FROM ORDERS WHERE ORDER_ID=?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setDouble(1, id);
        statement.executeUpdate();
    }

    public int getId(){
        return this.id;
    }

    public int getAccountId() throws SQLException{
        queryOrder(id);
        return this.accountId;
    }

    public double getAmount() throws SQLException {
        queryOrder(id);
        return this.amount;
    }

    public String getStatus() throws SQLException{
        queryOrder(id);
        return this.status;
    }

    public Timestamp getTime() throws SQLException{
        queryOrder(id);
        return this.time;
    }

    @Override
    public String toString() {
        return "Order " + id + " for account " + accountId + ": " + amount + " " + symbol + " @ $" + limitPrice + " @ " + time + " " + status;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(getClass())) {
            Order otherOrder = (Order) other;
            boolean hasSameId = otherOrder.id == id;
            boolean hasSameSymbol = otherOrder.symbol.equals(symbol);
            boolean hasSameAmount = Double.compare(otherOrder.amount, amount) == 0;
            boolean hasSameLimitPrice = Double.compare(otherOrder.limitPrice, limitPrice) == 0;
            boolean hasSameAccountId = otherOrder.accountId == accountId;
            boolean hasSameStatus = otherOrder.status.equals(status);
            boolean hasSameTime = otherOrder.time.getTime() == time.getTime();
            if (hasSameId && hasSameSymbol && hasSameAmount && hasSameLimitPrice && hasSameAccountId && hasSameStatus && hasSameTime) {
                return true;
            }
            return false;
        }
        return false;
    }
}
