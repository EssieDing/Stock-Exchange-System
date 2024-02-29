package edu.duke.ece568.hw4.server;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

public class JDBCManagerTest {
    @Test
    public void test_jdbcManagerConnection() throws SQLException{
        assertDoesNotThrow(() -> new JDBCManager("localhost", "5432", "ece568_hw4", "postgres", "postgres"));
        assertThrows(SQLException.class, ()->new JDBCManager("localhost", "5432", "db", "postgres", "postgres"));
    }
}
