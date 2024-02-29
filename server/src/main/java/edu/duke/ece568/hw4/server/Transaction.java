package edu.duke.ece568.hw4.server;

public class Transaction {
    private final int id;
    private final int orderId;
    private double amount;
    private double price;

    public Transaction(int id, int orderId, double amount, double price) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.price = price;
    }

    @Override
    public String toString() {
        return "Transaction " + id + ": Order " + orderId + " " + amount + " @ $" + price;
    }
}

