package edu.duke.ece568.hw4.server;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XMLParserTest {

    private JDBCManager generateJDBCManager() throws SQLException, ClassNotFoundException {
        JDBCManager jdbcManager = new JDBCManager("localhost", "5432", "ece568_hw4", "postgres", "postgres");
        return jdbcManager;
    }

    private void compare_response(String request, String expected) throws ClassNotFoundException,
            SQLException, ParserConfigurationException{
        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        XMLParser parser = new XMLParser(request, jdbcManager);
        String actual = parser.parseRequest();
        System.out.println(actual);

        assertEquals(expected, actual);
    }

    // test request format
    @Test
    public void test_invalid() throws ClassNotFoundException, SQLException,
            ParserConfigurationException, SAXException {

        String request = "abcd12345";
        String expected =
                "<results><error>java.lang.Exception: Request XML must have a tag <</error></results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_create_invalidTag() throws ClassNotFoundException, SQLException,
            ParserConfigurationException, SAXException{
        String request =
                "173\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create" +
                        "<account id=\"123456\" balance=\"1000\"/>" +
                        "<account id=\"738\" balance=\"2000\"/>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<error>" +
                        "org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 46; Element type \"create\" must be followed by either attribute specifications, \">\" or \"/>\"." +
                        "</error>" +
            "</results>";
        this.compare_response(request, expected);
    }

    // test create account
    @Test
    public void test_parseRequest_createAccount_invalidTag() throws ClassNotFoundException, SQLException,
            ParserConfigurationException, SAXException, IOException, TransformerException {

        String request_create =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<acount id=\"123456\" balance=\"1000\"/>" +
                        "</create>";

        String expected =
                "<results><error>invalid tag name is read</error></results>";

        this.compare_response(request_create, expected);
    }

    @Test
    public void test_parseRequest_createAccount_invalidTag2() throws ParserConfigurationException, ClassNotFoundException, SQLException {

        String request =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" +
                        "<create>" +
                        "<account p=\"123456\" ba=\"1000\"/>" +
                        "</create>";

        String expected =
                "<results><error>account must have attribute id</error></results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createAccount_valid() throws ParserConfigurationException, ClassNotFoundException, SQLException{

        String request = "173\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<create>"
                        + "<account id=\"88888\" balance=\"1000\"/>"
                        + "<account id=\"66666\" balance=\"2000\"/>"
                        + "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"1000.0\" id=\"88888\"/>" +
                        "<created balance=\"2000.0\" id=\"66666\"/>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createAccountDuplicate() throws ParserConfigurationException,
            ClassNotFoundException, SQLException{

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<account id=\"88888\" balance=\"100\"/>" +
                        "<account id=\"88888\" balance=\"200\"/>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"100.0\" id=\"88888\"/>" +
                        "<error id=\"88888\">org.postgresql.util.PSQLException: " +
                        "ERROR: duplicate key value violates unique constraint \"account_pkey\"  " +
                        "Detail: Key (account_number)=(88888) already exists." +
                        "</error>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createAccount_valid2(){
        String request = "123\n" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<create><account id=\"12714\" balance=\"98349213\"/>" +
                "</create>";
        String expected = "<results>" +
                "<created balance=\"98349213\" id=\"12714\"/>" +
                "<created id=\"12714\"" +
                "</results>";
    }

    // test create symbol
    @Test
    public void test_parseRequest_createSymbol_valid() throws ParserConfigurationException,
            ClassNotFoundException, SQLException{

        String request =
                "173" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<account id=\"123456\" balance=\"1000\"/>" +
                        "<symbol sym=\"SPY\">" +
                        "<account id=\"123456\">100000</account>" +
                        "</symbol>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"1000.0\" id=\"123456\"/>" +
                        "<created id=\"123456\" sym=\"SPY\"/>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createSymbol_missingChildren() throws ParserConfigurationException,
            ClassNotFoundException, SQLException{

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<account id=\"123456\" balance=\"1000\"/>" +
                        "<symbol sym=\"SPY\">" +
                        "</symbol>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"1000.0\" id=\"123456\"/>" +
                        "<error>symbol must have child</error>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createSymbol_invalidAttributes() throws ParserConfigurationException,
             ClassNotFoundException, SQLException{

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<account id=\"123456\" balance=\"1000\"/>" +
                        "<symbol sy=\"SPY\">" +
                        "<account id=\"123456\">100000</account>" +
                        "</symbol>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"1000.0\" id=\"123456\"/>" +
                        "<error>symbol must have attribute sym</error>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_createSymbol_invalidChildrenAttributes() throws ParserConfigurationException,
            ClassNotFoundException, SQLException{

        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<create>" +
                        "<account id=\"123456\" balance=\"1000\"/>" +
                        "<symbol sym=\"SPY\">" +
                        "<account iasd=\"123456\">100000</account>" +
                        "</symbol>" +
                        "</create>";

        String expected =
                "<results>" +
                        "<created balance=\"1000.0\" id=\"123456\"/>" +
                        "<error>account under symbol must have id</error>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    // helper function for compare response
    private void compare_response_noClean(String request, String expected) throws ClassNotFoundException,
            SQLException, ParserConfigurationException{
        JDBCManager jdbcManager = generateJDBCManager();

        XMLParser parser = new XMLParser(request, jdbcManager);
        String actual = parser.parseRequest();

        expected = expected.replaceAll("time=\".*?\"", "");
        actual = actual.replaceAll("time=\".*?\"", "");

        System.out.println(actual);

        assertEquals(expected, actual);
    }

    // test transaction
    @Test
    public void test_parseRequest_transactions_missingAttributes() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException, InvalidAlgorithmParameterException, TransformerException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0,10, jdbcManager);
        account.saveAccount();

        String request =
                "<transactions i=\"0\">" +
                        "<order sym=\"SYM\" amount=\"10\" limit=\"5.2\"/>" +
                        "</transactions>";
        String expect = "<results><error>transactions must have attribute id</error></results>";
        this.compare_response_noClean(request, expect);
    }

    @Test
    public void test_parseRequest_transactions_missingChildren() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();

        String request =
                "<transactions id=\"0\">" +
                        "</transactions>";
        String expect = "<results><error>the root tag MUST have children</error></results>";
        this.compare_response_noClean(request, expect);
    }

    @Test
    public void test_parseRequest_transactions_childrenTagError() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();

        String request =
                "<transactions id=\"0\">" +
                        "<qu sym=\"SYM\" amount=\"10\" limit=\"5.2\"/>" +
                        "</transactions>";
        String expect = "<results><error>the root tag MUST have order, query, cancel child node</error></results>";
        this.compare_response_noClean(request, expect);
    }

    // test openorder
    @Test
    public void test_parseRequest_open_attributeError() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();

        String request =
                "<transactions id=\"0\">" +
                        "<order sy=\"SYM\" amount=\"10\" limit=\"5.2\"/>" +
                        "</transactions>";
        String expected = "<results><error>order must have attribute sym</error></results>";
        this.compare_response(request, expected);

        request =
                "<transactions id=\"0\">" +
                        "<order sym=\"SYM\" amoun=\"10\" limit=\"5.2\"/>" +
                        "</transactions>";
        expected = "<results><error>order must have attribute amount</error></results>";
        this.compare_response(request, expected);

        request =
                "<transactions id=\"0\">" +
                        "<order sym=\"SYM\" amount=\"10\" limi=\"5.2\"/>" +
                        "</transactions>";
        expected = "<results><error>order must have attribute limit</error></results>";
        this.compare_response_noClean(request, expected);
    }

    @Test
    public void test_parseRequest_open_valid() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(12345, 1000, jdbcManager);
        account.saveAccount();

        Position position = new Position( "AMAZ", 100, 12345, jdbcManager);
        position.savePosition();

        Order order = new Order("XXX", 0.1, 1.0, 12345, jdbcManager);
        order.saveOrder();

        String request =
                "<transactions id=\"12345\">" +
                        "<order sym=\"SYM\" amount=\"10\" limit=\"5.2\"/>" +
                        "<order sym=\"AMAZ\" amount=\"-10\" limit=\"15\"/>" +
                        "</transactions>";

        String expected =
                "<results>" +
                        "<opened amount=\"10\" id=\"" + (order.getId() + 1) + "\" limit=\"5.2\" sym=\"SYM\"/>" +
                        "<opened amount=\"-10\" id=\"" + (order.getId() + 2) +"\" limit=\"15\" sym=\"AMAZ\"/>" +
                        "</results>";
        this.compare_response_noClean(request, expected);
    }


    // test query order
    @Test
    public void test_parseRequest_query_missingAttribute() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException, InvalidAlgorithmParameterException, TransformerException{

        String request =
                "<transactions id=\"0\">" +
                        "<query b=\"0\"/>" +
                        "</transactions>";

        String expected = "<results><error>query must have attribute id</error></results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_cancel_query_valid() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0, 100, jdbcManager);
        Account account2 = new Account(1, 100, jdbcManager);
        account.saveAccount();
        account2.saveAccount();

        Position position = new Position("SYM", 10, 1, jdbcManager);
        position.savePosition();

        Order buyOrder = new Order("SYM", 10, 5, 0, jdbcManager);
        buyOrder.saveOrder();
        Order sellOrder = new Order("SYM", -5, 3, 1, jdbcManager);
        sellOrder.saveOrder();
        sellOrder.matchOrder();

        Order sellOrder2 = new Order("SYM", -3, 3, 1, jdbcManager);
        sellOrder2.saveOrder();
        sellOrder2.matchOrder();

        String request =
                "123\n"+
                "<transactions id=\"0\">" +
                        "<cancel id=\"" + buyOrder.getId() + "\"/>" +
                        "<query id=\"" + buyOrder.getId() + "\"/>" +
                        "</transactions>";

        String expected =
                "<results>" +
                        "<canceled id=\"" + buyOrder.getId() + "\">" +
                        "<canceled shares=\"2.0\" />" +
                        "<executed price=\"5.0\" shares=\"5.0\" />" +
                        "<executed price=\"5.0\" shares=\"3.0\" />" +
                        "</canceled>" +
                        "<status id=\"" + buyOrder.getId() + "\">" +
                        "<canceled shares=\"2.0\" />" +
                        "<executed price=\"5.0\" shares=\"5.0\" />" +
                        "<executed price=\"5.0\" shares=\"3.0\" />" +
                        "</status>" +
                        "</results>";
        this.compare_response_noClean(request, expected);
    }

    // test cancel order
    @Test
    public void test_parseRequest_cancel_attributeMissing() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException, InvalidAlgorithmParameterException, TransformerException{

        String request =
                "<transactions id=\"0\">" +
                        "<cancel Xd=\"0\"/>" +
                        "</transactions>";

        String expected = "<results><error>cancel must have attribute id</error></results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_cancel_InvalidErrorId() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        String request =
                "<transactions id=\"0\">" +
                        "<cancel id=\"110231\"/>" +
                        "</transactions>";

        String expected =
                "<results>" +
                        "<canceled id=\"110231\">" +
                        "<error id=\"110231\">" +
                        "java.lang.IllegalArgumentException: Error: Cannot find order with ID 110231" +
                        "</error>" +
                        "</canceled>" +
                        "</results>";

        this.compare_response(request, expected);
    }

    @Test
    public void test_parseRequest_cancel_InvalidAccountId() throws ParserConfigurationException, SAXException,
            IOException, ClassNotFoundException, SQLException{

        JDBCManager jdbcManager = generateJDBCManager();
        jdbcManager.deleteAll();

        Account account = new Account(0, 100, jdbcManager);
        account.saveAccount();

        Position position = new Position("APPL", 10, 0, jdbcManager);
        position.savePosition();

        Order order = new Order("APPL", -5, 100, 0, jdbcManager);
        order.saveOrder();

        String request =
                "<transactions id=\"1\">" +
                        "<cancel id=\"" + order.getId() + "\"/>" +
                        "</transactions>";

        String expected =
                "<results>" +
                        "<canceled id=\"" + order.getId() + "\">" +
                        "<error id=\"" + order.getId()  + "\">" +
                        "java.lang.Exception: the account id doesn't match" +
                        "</error>" +
                        "</canceled>" +
                        "</results>";
        this.compare_response_noClean(request, expected);
    }





}
