package edu.duke.ece568.hw4.server;

import org.junit.jupiter.api.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderUtilTest {
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
    public void test_findOrdersByIdAndStatus() throws SQLException {
        // Set up test data
        List<Order> ordersExpected = new ArrayList<>();
        Account account1 = new Account(1001, 99999, jdbcManager);
        Account account2 = new Account(1002, 999999, jdbcManager);
        Account account3 = new Account(1003, 9999999, jdbcManager);
        account1.saveAccount();
        account2.saveAccount();
        account3.saveAccount();
        Order order1 = new Order("AAPL", 10.0, 100.0, 1001, jdbcManager);
        Order order2 = new Order("GOOG", 5.0, 200.0, 1002, jdbcManager);
        Order order3 = new Order("AMZN", 15.0, 1500.0, 1003, jdbcManager);

        // Save test data to database
        order1.saveOrder();
        order2.saveOrder();
        order3.saveOrder();

        ordersExpected.add(order1);

        // Call the method being tested
        List<Order> ordersActual = OrderUtils.findOrdersByIdAndStatus(jdbcManager, order1.getId(), "OPEN");

        // Assert expected results
        assertEquals(ordersExpected.size(), ordersActual.size());
        for (int i = 0; i < ordersExpected.size(); i++) {
            assertEquals(ordersExpected.get(i), ordersActual.get(i));
        }
    }

    @Test
    public void test_findExecutedOrdersById() throws SQLException {
        List<ExecutedOrder> executedOrdersExpected = new ArrayList<>();
        ExecutedOrder executedOrder1 = new ExecutedOrder(7, 4, "AMZN", 2, 22, time, jdbcManager);
        ExecutedOrder executedOrder2 = new ExecutedOrder(7, 4, "FB", 2, 22, time, jdbcManager);
        ExecutedOrder executedOrder3 = new ExecutedOrder(5, 4, "FB", 2, 22, time, jdbcManager);
        executedOrder1.saveExecutedOrder();
        executedOrder2.saveExecutedOrder();
        executedOrder3.saveExecutedOrder();

        executedOrdersExpected.add(executedOrder1);
        executedOrdersExpected.add(executedOrder2);
        List<ExecutedOrder> executedOrdersActual = OrderUtils.findExecutedOrdersById(jdbcManager, 7);
        assertEquals(executedOrdersExpected.size(), executedOrdersActual.size());
        for (int i = 0; i < executedOrdersExpected.size(); i++) {
            assertEquals(executedOrdersExpected.get(i), executedOrdersActual.get(i));
        }
    }

    @Test
    public void test_findTopMatchForBuyOrder() throws SQLException {
        // Create accounts
        Account account1 = new Account(1001, 99999, jdbcManager);
        Account account2 = new Account(1002, 99999, jdbcManager);
        Account account3 = new Account(1003, 99999, jdbcManager);
        Account account4 = new Account(1004, 99999, jdbcManager);
        Account account5 = new Account(1005, 99999, jdbcManager);
        Account account6 = new Account(1006, 99999, jdbcManager);
        // Commit accounts to database
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        assertDoesNotThrow(account3::saveAccount);
        assertDoesNotThrow(account4::saveAccount);
        assertDoesNotThrow(account5::saveAccount);
        assertDoesNotThrow(account6::saveAccount);

        // Create buy orders
        Order buyOrder1 = new Order("AAPL", 10.0, 100.0, 1001, jdbcManager);
        Order buyOrder2 = new Order("AAPL", 10.0, 40, 1002, jdbcManager);

        // Commit buy orders to database
        assertDoesNotThrow(buyOrder1::saveOrder);
        assertDoesNotThrow(buyOrder2::saveOrder);

        // Create sell orders
        Order sellOrder1 = new Order("AAPL", -9.0, 90.0, 1003, jdbcManager);
        Order sellOrder2 = new Order("AAPL", -9.0, 60.0, 1004, jdbcManager);
        Order sellOrder3 = new Order("AAPL", -9.0, 80.0, 1005, jdbcManager);
        Order sellOrder4 = new Order("AAPL", -9.0, 70.0, 1006, jdbcManager);

        // Commit sale orders to database
        sellOrder1.saveOrder();
        sellOrder2.saveOrder();
        sellOrder3.saveOrder();
        sellOrder4.saveOrder();

        // Test assertions
        Order actualSellOrder1 = OrderUtils.findTopMatchForBuyOrder(jdbcManager, 1001, "AAPL", 100.0);
        assertEquals(sellOrder2, actualSellOrder1);
        Order actualSellOrder2 = OrderUtils.findTopMatchForBuyOrder(jdbcManager, 1002, "AAPL", 40);
        assertNull(actualSellOrder2);
    }

    @Test
    public void test_findTopMatchForSellOrder() throws SQLException {
        Account account1 = new Account(0, 100, jdbcManager);
        Account account2 = new Account(1, 100, jdbcManager);
        Account account3 = new Account(2, 100, jdbcManager);
        Account account4 = new Account(3, 100, jdbcManager);

        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        assertDoesNotThrow(account3::saveAccount);
        assertDoesNotThrow(account4::saveAccount);

        // success: get the order with the highest price, and not from self
        Order sellOrder1 = new Order("FB", -100, 100, 0, jdbcManager);
        Order sellOrder2 = new Order("FB", -100, 125, 0, jdbcManager);

        assertDoesNotThrow(sellOrder1::saveOrder);
        assertDoesNotThrow(sellOrder2::saveOrder);

        Order buyOrder1 = new Order("FB", 90, 90, 0, jdbcManager);
        Order buyOrder2 = new Order("FB", 90, 100, 1, jdbcManager);
        Order buyOrder3 = new Order("FB", 90, 120, 2, jdbcManager);
        Order buyOrder4 = new Order("FB", 90, 110, 3, jdbcManager);

        buyOrder1.saveOrder();
        buyOrder2.saveOrder();
        buyOrder3.saveOrder();
        buyOrder4.saveOrder();

        Order actualBuyOrder1 = OrderUtils.findTopMatchForSellOrder(jdbcManager, 0, "FB", 100);
        Order actualBuyOrder2 = OrderUtils.findTopMatchForSellOrder(jdbcManager, 0, "FB", 130);
        assertEquals(buyOrder3, actualBuyOrder1);
        assertNull(actualBuyOrder2);
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
