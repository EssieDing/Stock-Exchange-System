package edu.duke.ece568.hw4.server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Position {
    private String symbol;
    private double amount;
    private int accountId;
    private JDBCManager jdbcManager;

    public Position(String symbol, double amount, int accountId, JDBCManager jdbcManager) {
        this.symbol = symbol;
        this.amount = amount;
        this.accountId = accountId;
        this.jdbcManager = jdbcManager;
    }

    public Position(String symbol, int accountId, JDBCManager jdbcManager) throws SQLException {
        this.jdbcManager = jdbcManager;
        queryPosition(symbol, accountId);
    }

    private void queryPosition(String symbol, int accountId) throws SQLException {
        String query = "SELECT * FROM POSITION WHERE ACCOUNT_NUMBER=? AND SYMBOL=?";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.setInt(1, accountId);
        statement.setString(2, symbol);
        ResultSet resultSet = statement.executeQuery();
        if (!resultSet.next()) {
            throw new IllegalArgumentException("Error: Cannot find position for symbol " + symbol + " in account " + accountId);
        }
        this.accountId = resultSet.getInt("ACCOUNT_NUMBER");
        this.symbol = resultSet.getString("SYMBOL");
        this.amount = resultSet.getDouble("AMOUNT");

    }

    public void savePosition() throws SQLException {
        try {
            Position p = new Position(symbol, accountId, jdbcManager);
            double amountUpdated = p.amount + amount;
            if (amountUpdated < 0) {
                throw new IllegalArgumentException("Error: Amount updated is negative!");
            }
            if (amountUpdated == 0) {
                String query = "DELETE FROM POSITION WHERE ACCOUNT_NUMBER=? AND SYMBOL=?";
                PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
                statement.setInt(1, accountId);
                statement.setString(2, symbol);
                statement.executeUpdate();
            } else {
                String query = "UPDATE POSITION SET AMOUNT=? WHERE ACCOUNT_NUMBER=? AND SYMBOL=?";
                PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
                statement.setDouble(1, amountUpdated);
                statement.setInt(2, accountId);
                statement.setString(3, symbol);
                statement.executeUpdate();
            }
        } catch (IllegalArgumentException e) {
            String query = "INSERT INTO POSITION (ACCOUNT_NUMBER, SYMBOL, AMOUNT) VALUES (?, ?, ?)";
            PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
            statement.setInt(1, accountId);
            statement.setString(2, symbol);
            statement.setDouble(3, amount);
            statement.executeUpdate();
        }
    }

    public String getSymbol() throws SQLException {
        queryPosition(symbol, accountId);
        return symbol;
    }

    public double getAmount() throws SQLException {
        queryPosition(symbol, accountId);
        return amount;
    }

    public int getAccountId() throws SQLException {
        queryPosition(symbol, accountId);
        return accountId;
    }

    @Override
    public String toString() {
        return "Position: " + " symbol " + symbol + " " + " amount " + amount + "on account " + accountId;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(getClass())) {
            Position otherPosition = (Position) other;
            boolean hasSameSymbol = otherPosition.symbol.equals(symbol);
            boolean hasSameAccountId = otherPosition.accountId == accountId;
            boolean hasSameAmount = Double.compare(otherPosition.amount, amount) == 0;
            if (hasSameSymbol && hasSameAccountId && hasSameAmount) {
                return true;
            }
            return false;
        }
        return false;
    }

}
