package edu.duke.ece568.hw4.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutedOrderTest {
    private JDBCManager jdbcManager;
    private Timestamp time;
    @BeforeEach
    public void setUp() throws ClassNotFoundException, SQLException {
        jdbcManager = createJdbcManager();
        time = Timestamp.from(Instant.now());
    }
    @AfterEach
    public void cleanUp() throws SQLException {
        deleteAll(jdbcManager);
    }
    @Test
    public void testConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new ExecutedOrder(12, 4, "SYM", 1, -1, time, jdbcManager));
        assertThrows(IllegalArgumentException.class, () -> new ExecutedOrder(2, 4, "SYM", 0, 22, time, jdbcManager));
        assertDoesNotThrow(() -> new ExecutedOrder(7, 4, "SYM", 2, 22, time, jdbcManager));
    }

    @Test
    public void test_saveExecutedOrder() {
        ExecutedOrder executedOrder1 = new ExecutedOrder(7, 4, "AMZN", 2, 22, time, jdbcManager);
        ExecutedOrder executedOrder2 = new ExecutedOrder(7, 4, "TSLA", 2, 22, time, jdbcManager);
        assertDoesNotThrow(executedOrder1::saveExecutedOrder);
        assertDoesNotThrow(executedOrder2::saveExecutedOrder);
    }

    @Test
    public void test_equals() {
        ExecutedOrder executedOrder1 = new ExecutedOrder(7, 4, "AMZN", 2, 22, time, jdbcManager);
        ExecutedOrder executedOrder2 = new ExecutedOrder(7, 4, "FB", 2, 22, time, jdbcManager);
        ExecutedOrder executedOrder3 = new ExecutedOrder(5, 4, "FB", 2, 22, time, jdbcManager);
        ExecutedOrder sameExecutedOrder1 = new ExecutedOrder(7, 4, "AMZN", 2, 22, time, jdbcManager);
        assertNotEquals(executedOrder1, executedOrder2);
        assertNotEquals(executedOrder1, executedOrder3);
        assertEquals(executedOrder1, sameExecutedOrder1);
    }

    private JDBCManager createJdbcManager() throws ClassNotFoundException, SQLException{
        return new JDBCManager("localhost", "5432", "ece568_hw4", "postgres", "postgres");
    }
    private void deleteAll(JDBCManager jdbcManager) throws SQLException {
        String query = "DELETE FROM ARCHIVE; DELETE FROM ORDERS; DELETE FROM POSITION; DELETE FROM ACCOUNT;";
        PreparedStatement statement = jdbcManager.getConnection().prepareStatement(query);
        statement.executeUpdate();
    }

}
