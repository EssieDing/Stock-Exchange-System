package edu.duke.ece568.hw4.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    private JDBCManager jdbcManager;
    private Timestamp time;

    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        jdbcManager = createJdbcManager();
        deleteAll(jdbcManager);
        time = Timestamp.from(Instant.now());
    }

    @AfterEach
    public void cleanUp() throws SQLException {
        deleteAll(jdbcManager);
    }
    @Test
    public void test_constructor() {
        assertThrows(IllegalArgumentException.class, ()->new Order("FB", 0, 10, 0, jdbcManager));
        assertThrows(IllegalArgumentException.class, ()->new Order("FB", -10, -1, 0, jdbcManager));
        assertThrows(IllegalArgumentException.class, ()->new Order("FB", 0, -1, 0, jdbcManager));
    }

    @Test
    public void test_saveOrder() throws SQLException {
        Account account = new Account(0, 100, jdbcManager);
        assertDoesNotThrow(account::saveAccount);
        Order buyOrder = new Order("APPL", 100, 100, 0, jdbcManager);
        assertDoesNotThrow(buyOrder::saveOrder);
        String query = "SELECT MAX(ORDER_ID) AS ORDER_ID FROM ORDERS;";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();
        assertTrue(resultSet.next());
        assertEquals(resultSet.getInt("ORDER_ID"), buyOrder.getId());
        assertEquals(0, buyOrder.getAccountId());
        assertThrows(IllegalArgumentException.class, buyOrder::saveOrder);
    }

    @Test
    public void test_updateAmount() throws SQLException{
        Account account = new Account(0, 100, jdbcManager);
        assertDoesNotThrow(account::saveAccount);
        Order buyOrder = new Order("APPL", 100, 100, 0, jdbcManager);
        assertThrows(IllegalArgumentException.class, ()->buyOrder.updateAmount(10));
        assertDoesNotThrow(()->buyOrder.saveOrder());
        assertDoesNotThrow(()->buyOrder.updateAmount(10));
        assertEquals(110, buyOrder.getAmount());
        assertDoesNotThrow(()->buyOrder.updateAmount(-30));
        assertEquals(80, buyOrder.getAmount());
    }

    @Test
    public void test_matchBuyOrder_valid1() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        Account account3 = new Account(2, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        assertDoesNotThrow(account3::saveAccount);
        Order buyOrder = new Order("APPL", 10, 100, 0, jdbcManager);
        assertDoesNotThrow(buyOrder::saveOrder);
        Order sellOrder1 = new Order("APPL", -3, 90, 1, jdbcManager);
        Order sellOrder2 = new Order("APPL", -5, 80, 2, jdbcManager);
        assertDoesNotThrow(sellOrder1::saveOrder);
        assertDoesNotThrow(sellOrder2::saveOrder);
        assertDoesNotThrow(buyOrder::matchForBuyOrder);
    }

    @Test
    public void test_matchBuyOrder_valid2() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        Order buyOrder = new Order("APPL", 10, 100, 0, jdbcManager);
        assertDoesNotThrow(buyOrder::saveOrder);
        Order sellOrder1 = new Order("APPL", -10, 90, 1, jdbcManager);
        assertDoesNotThrow(sellOrder1::saveOrder);
        assertDoesNotThrow(buyOrder::matchForBuyOrder);
    }

    @Test
    public void test_matchBuyOrder_valid3() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        Order buyOrder = new Order("APPL", 5, 100, 0, jdbcManager);
        assertDoesNotThrow(buyOrder::saveOrder);
        Order sellOrder1 = new Order("APPL", -10, 90, 1, jdbcManager);
        assertDoesNotThrow(sellOrder1::saveOrder);
        assertDoesNotThrow(buyOrder::matchForBuyOrder);
    }

    @Test
    public void test_matchSellOrder_valid1() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        Account account3 = new Account(2, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        assertDoesNotThrow(account3::saveAccount);
        Order sellOrder = new Order("APPL", -10, 100, 0, jdbcManager);
        assertDoesNotThrow(sellOrder::saveOrder);
        Order buyOrder1 = new Order("APPL", 3, 101, 1, jdbcManager);
        Order buyOrder2 = new Order("APPL", 4, 102, 2, jdbcManager);
        assertDoesNotThrow(buyOrder1::saveOrder);
        assertDoesNotThrow(buyOrder2::saveOrder);
        assertDoesNotThrow(sellOrder::matchForSellOrder);
    }

    @Test
    public void test_matchSellOrder_valid2() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        Order sellOrder = new Order("APPL", -10, 100, 0, jdbcManager);
        assertDoesNotThrow(sellOrder::saveOrder);
        Order buyOrder1 = new Order("APPL", 10, 101, 1, jdbcManager);
        assertDoesNotThrow(buyOrder1::saveOrder);
        assertDoesNotThrow(sellOrder::matchForSellOrder);
    }

    @Test
    public void test_matchSellOrder_valid3() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        Order sellOrder = new Order("APPL", -10, 100, 0, jdbcManager);
        assertDoesNotThrow(sellOrder::saveOrder);
        Order buyOrder1 = new Order("APPL", 20, 101, 1, jdbcManager);
        assertDoesNotThrow(buyOrder1::saveOrder);
        assertDoesNotThrow(sellOrder::matchForSellOrder);
    }

    @Test
    public void test_cancelOrder() throws ClassNotFoundException, SQLException{
        Account account1 = new Account(0, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);

        Order sellOrder = new Order("APPL", -10, 100, 0, jdbcManager);
        assertThrows(IllegalArgumentException.class, sellOrder::cancelOrder);
        assertDoesNotThrow(sellOrder::saveOrder);
        assertEquals("OPEN", sellOrder.getStatus());
        assertDoesNotThrow(sellOrder::cancelOrder);
        assertEquals("CANCELED", sellOrder.getStatus());
        assertThrows(IllegalArgumentException.class, sellOrder::cancelOrder);

        Order buyOrder = new Order("APPL", 10, 100, 0, jdbcManager);
        assertDoesNotThrow(buyOrder::saveOrder);
        assertEquals("OPEN", buyOrder.getStatus());
        assertDoesNotThrow(buyOrder::cancelOrder);
        assertEquals("CANCELED", buyOrder.getStatus());
        assertThrows(IllegalArgumentException.class, ()->buyOrder.cancelOrder());
    }

    private JDBCManager createJdbcManager() throws ClassNotFoundException, SQLException {
        return new JDBCManager("localhost", "5432", "ece568_hw4", "postgres", "postgres");
    }

    private void deleteAll(JDBCManager jdbcManager) throws SQLException {
        String query = "DELETE FROM ARCHIVE; DELETE FROM ORDERS; DELETE FROM POSITION; DELETE FROM ACCOUNT;";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.executeUpdate();
    }
}
