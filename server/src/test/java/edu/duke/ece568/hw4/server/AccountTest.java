package edu.duke.ece568.hw4.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {
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
    public void testConstructors() throws ClassNotFoundException, SQLException, IllegalAccessException{
        assertThrows(IllegalArgumentException.class, () -> new Account(0, jdbcManager));
        Account account1 = new Account(0, 100, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertThrows(SQLException.class, account1::saveAccount);
    }

    @Test
    public void test_canBuy() throws ClassNotFoundException, SQLException {
        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();
        assertTrue(account.canBuy(5, 19));
        assertTrue(account.canBuy(5, 20));
        assertFalse(account.canBuy(1, 200));
        assertThrows(IllegalArgumentException.class, () -> account.canBuy(0, 20));
        assertThrows(IllegalArgumentException.class, () -> account.canBuy(-5, 20));
        assertThrows(IllegalArgumentException.class, () -> account.canBuy(20, -10));
        assertThrows(IllegalArgumentException.class, () -> account.canBuy(20, 0));
    }

    @Test
    public void test_canSell() throws ClassNotFoundException, SQLException {
        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();
        Position position = new Position("APPL", 100, 0, jdbcManager);
        position.savePosition();
        assertTrue(account.canSell("APPL", 80.5));
        assertTrue(account.canSell("APPL", 100));
        assertFalse(account.canSell("APPL", 100.1));
        assertFalse(account.canSell("FB", 80));
        assertFalse(account.canSell("FB", 80));
        assertFalse(account.canSell("FB", 80));
        assertThrows(IllegalArgumentException.class, ()->account.canSell("", 10));
        assertThrows(IllegalArgumentException.class, ()->account.canSell(null, 10));
        assertThrows(IllegalArgumentException.class, ()->account.canSell("FB", 0));
        assertThrows(IllegalArgumentException.class, ()->account.canSell("FB", -1));
    }

    @Test
    public void test_placeOrder_invalid() throws ClassNotFoundException, SQLException {
        Account account = new Account(0, 1000, jdbcManager);
        assertDoesNotThrow(account::saveAccount);
        Position position = new Position("APPL", 10, 0, jdbcManager);
        assertDoesNotThrow(position::savePosition);
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("", 10, 10));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder(null, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", 10, 0));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", 10, -10));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", -10, -10));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", 10, 101));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("APPL", -11, 90));
        assertThrows(IllegalArgumentException.class, () -> account.placeOrder("FB", -1, 90));
    }

    @Test
    public void test_placeOrder_valid() {
        Account account1 = new Account(0, 1000, jdbcManager);
        Account account2 = new Account(1, 1000, jdbcManager);
        assertDoesNotThrow(account1::saveAccount);
        assertDoesNotThrow(account2::saveAccount);
        Position position = new Position("APPL", 10, 0, jdbcManager);
        assertDoesNotThrow(position::savePosition);
        assertDoesNotThrow(()->account2.placeOrder("APPL", 5, 30));
        assertDoesNotThrow(()->account1.placeOrder("APPL", 10, 12));
        assertDoesNotThrow(()->account1.placeOrder("APPL", -2, 20));
    }

    @Test
    public void test_tryUpdateBalance() throws SQLException{
        Account account1 = new Account(0, 100, jdbcManager);
        account1.saveAccount();
        assertTrue(account1.tryUpdateBalance(10));
        assertTrue(account1.tryUpdateBalance(-20));
        assertFalse(account1.tryUpdateBalance(-90.1));
    }

    @Test
    public void test_equals() throws ClassNotFoundException, SQLException {
        Account account1 = new Account(0, 999, jdbcManager);
        account1.saveAccount();
        Account account2 = new Account(1, 999, jdbcManager);
        account2.saveAccount();
        Account sameAccount1 = new Account(0, jdbcManager);
        assertEquals(account1, sameAccount1);
        assertNotEquals(account1, account2);
        assertNotEquals(account2, sameAccount1);
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
