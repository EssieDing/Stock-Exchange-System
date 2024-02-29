package edu.duke.ece568.hw4.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Account {
    private int id;
    private double balance;
    private JDBCManager jdbcManager;

    public Account(int id, double balance, JDBCManager jdbcManager) {
        this.id = id;
        this.balance = balance;
        this.jdbcManager = jdbcManager;
    }

    public Account(int id, JDBCManager jdbcManager) throws SQLException {
        this.jdbcManager = jdbcManager;
        queryAccount(id);
    }

    private void queryAccount(int id) throws SQLException {
        String query = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER=?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, id);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            throw new IllegalArgumentException("Cannot find account " + id);
        }
        this.id = resultSet.getInt("ACCOUNT_NUMBER");
        this.balance = resultSet.getDouble("BALANCE");
    }

    public void saveAccount() throws SQLException {
        String query = "INSERT INTO ACCOUNT(ACCOUNT_NUMBER, BALANCE) VALUES (?, ?)";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, id);
        statement.setDouble(2, balance);
        statement.executeUpdate();
    }

    public boolean canBuy(double amount, double limitPrice) throws SQLException {
        queryAccount(id);
        if (amount <= 0) {
            throw new IllegalArgumentException("Buy amount must be non-negative!");
        }
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Buy price must be non-negative!");
        }
        return balance >= amount * limitPrice;
    }

    public boolean canSell(String symbol, double amount) throws SQLException {
        queryAccount(id);
        if (symbol == null || symbol == "") {
            throw new IllegalArgumentException("Stock symbol must be valid!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Sell amount must be non-negative!");
        }
        String query = "SELECT AMOUNT FROM POSITION WHERE ACCOUNT_NUMBER=? AND SYMBOL=? AND AMOUNT >= ?";
        try (PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query)) {
            statement.setInt(1, id);
            statement.setString(2, symbol);
            statement.setDouble(3, amount);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public int placeOrder(String symbol, double amount, double limitPrice) throws SQLException {
        // error checking
        if (symbol == null || symbol == "") {
            throw new IllegalArgumentException("Stock symbol must be valid!");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("Sell amount must not be zero!");
        }
        if (limitPrice <= 0) {
            throw new IllegalArgumentException("Limit price must be non-negative!");
        }

        if (amount > 0) {
            System.out.println("enter place buy order");
            // for buy orders
            return placeBuyOrders(symbol, amount, limitPrice);
        } else {
            System.out.println("enter place sell order");
            // for sell orders
            return placeSellOrders(symbol, amount, limitPrice);
        }
    }

    private int placeBuyOrders(String symbol, double amount, double limitPrice) throws SQLException {
        if (!canBuy(amount, limitPrice)) {
            throw new IllegalArgumentException("This account has insufficient balance to buy the order!");
        }
        tryUpdateBalance(-1 * amount * limitPrice);
        Order order = new Order(symbol, amount, limitPrice, getId(), jdbcManager);
        order.saveOrder();
        order.matchOrder();
        return order.getId();
    }

    private int placeSellOrders(String symbol, double amount, double limitPrice) throws SQLException {
        if(!canSell(symbol, Math.abs(amount))){
            throw new IllegalArgumentException("This account has insufficient stocks to place the sell order!");
        }
        Position position = new Position(symbol, amount, getId(), jdbcManager);
        position.savePosition();
        Order order = new Order(symbol, amount, limitPrice,getId(), jdbcManager);
        order.saveOrder();
        order.matchOrder();
        return order.getId();
    }

    public boolean tryUpdateBalance(double number) throws SQLException {
        queryAccount(id);
        if (balance + number < 0) {
            return false;
        }
        balance += number;
        String query = "UPDATE ACCOUNT SET BALANCE = ? WHERE ACCOUNT_NUMBER = ?";
        try (PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query)) {
            statement.setDouble(1, balance);
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public int getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return "Account " + id + " has $" + balance;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(getClass())) {
            Account otherAccount = (Account) other;
            boolean hasSameId = otherAccount.getId() == getId();
            boolean hasSameBalance = Double.compare(otherAccount.getBalance(), getBalance()) == 0;
            if (hasSameId && hasSameBalance) {
                return true;
            }
            return false;
        }
        return false;
    }

}