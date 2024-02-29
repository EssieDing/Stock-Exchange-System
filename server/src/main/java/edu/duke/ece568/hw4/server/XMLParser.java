package edu.duke.ece568.hw4.server;


import com.google.common.base.StandardSystemProperty;
import org.postgresql.util.PSQLException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

public class XMLParser{
    private String request;
    private JDBCManager jdbcManager;
    private Document response;
    private DocumentBuilder docBuilder;
    private Element responseRoot;

    public XMLParser(String request, JDBCManager jdbc) throws ParserConfigurationException {
        this.request = request;
        this.jdbcManager = jdbc;
        this.response = null;
        this.responseRoot = null;
        docBuilderBuild();
    }

    private void docBuilderBuild() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        docBuilder = docFactory.newDocumentBuilder();
    }

    private void addErrorNode(String errorMsg) {
        Element node = this.response.createElement("error");
        node.appendChild(this.response.createTextNode(errorMsg));
        this.responseRoot.appendChild(node);
    }

    /**
     * Document to String
     *
     * @param doc Xmldocument
     * @return String
     */
    private static String doc2String(Document doc) {
        try {
            Source source = new DOMSource(doc);
            StringWriter stringWriter = new StringWriter();
            StreamResult result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString().replaceAll("\n|\r", "");
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkAttribute(Element node, String attribute) {
        if(!node.hasAttribute(attribute)){
            String errorMsg = node.getNodeName() + " must have attribute " + attribute.toString();
            this.addErrorNode(errorMsg);
            return false;
        }
        return true;
    }



    public String parseRequest() throws ParserConfigurationException {
        try {
            // standard input
            // replace new line symbol
            this.request = this.request.replaceAll(">[\\s\r\n]*<", "><");
            this.request = this.request.replaceAll("\r\n", "");
            this.request = this.request.replaceAll("\n", "");

            // remove integer message size
            int firstTag = this.request.indexOf("<");
            if(firstTag == -1){
                throw new Exception("Request XML must have a tag <");
            }

            this.request = this.request.substring(firstTag);
            System.out.println(request);

            // build document of request
            Document requestDoc = docBuilder.parse(new InputSource(new StringReader(this.request)));

            // get the top-level node
            requestDoc.getDocumentElement().normalize();
            Element requestRoot = requestDoc.getDocumentElement();
            String rootName = requestRoot.getNodeName();

            // create the response xml root node

            this.response = docBuilder.newDocument();
            this.responseRoot = response.createElement("results");
            response.appendChild(responseRoot);

            if (rootName.equals("create")) {
                processCreate(requestRoot);
            } else if (rootName.equals("transactions")) {
                processTransaction(requestRoot);
            } else {
                this.addErrorNode("root node must be create or transactions");
            }
            return doc2String(this.response);

        } catch (Exception e) {
            e.printStackTrace();
//            Element node = this.response.createElement("error");
//            node.appendChild(this.response.createTextNode(e.toString()));
//            this.responseRoot.appendChild(node);
            return "<results><error>" + e.toString() + "</error></results>";
        }
    }


    protected void processCreate(Element createNode) throws SQLException{
        NodeList childList = createNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element node = (Element) childList.item(i);
                String nodeName = node.getNodeName();
                if (nodeName.equals("account")) {
                    Node nodeParent = node.getParentNode();
                    if (nodeParent != null && nodeParent.getNodeName().equals("symbol")) {
                        createSymbol(nodeParent.getAttributes(), node);
                    } else {
                        createAccount(node);
                    }

                } else if (nodeName.equals("symbol")) {
                    if (node.hasChildNodes()){
                        processCreate(node);
                    } else {
                        this.addErrorNode("symbol must have child");
                    }
                } else {
                    this.addErrorNode("invalid tag name is read");
                }
            }
        }
    }

    private void createAccount(Element accountNode) throws SQLException{
        if (!checkAttribute(accountNode,"id")) {
            return;
        }
        if(!checkAttribute(accountNode,"balance")){
            return;
        }
        try{
            int id = Integer.parseInt(accountNode.getAttribute("id"));
            double balance = Double.parseDouble(accountNode.getAttribute("balance"));

            this.jdbcManager.getConnection().setAutoCommit(false);
            Account account = new Account(id, balance, jdbcManager);
            account.saveAccount();
            this.jdbcManager.getConnection().commit();

            Element node = this.response.createElement("created");
            node.setAttribute("id", Integer.toString(id));
            node.setAttribute("balance", Double.toString(balance));
            responseRoot.appendChild(node);

        } catch (Exception e) {
            this.jdbcManager.getConnection().rollback();

            Element node = this.response.createElement("error");
            node.setAttribute("id", accountNode.getAttribute("id"));
            node.appendChild(response.createTextNode(e.toString()));
            responseRoot.appendChild(node);
        }
    }

    private void createSymbol(NamedNodeMap symAttr, Element temp) throws SQLException{
        Node symbolNode = symAttr.getNamedItem("sym");
        if (symbolNode == null){
            this.addErrorNode("symbol must have attribute sym");
            return;
        }
        String sym = symbolNode.getNodeValue();

        NamedNodeMap attr = temp.getAttributes();
        Node idNode = attr.getNamedItem("id");
        if (idNode == null) {
            this.addErrorNode("account under symbol must have id");
            return;
        }
        int accountID = Integer.parseInt(idNode.getNodeValue());
        double amount = Double.parseDouble(temp.getTextContent());
        try {
            this.jdbcManager.getConnection().setAutoCommit(false);
            Position myPosition = new Position(sym, amount, accountID, jdbcManager);
            myPosition.savePosition();
            this.jdbcManager.getConnection().commit();

            // add symbol to response
            Element node = this.response.createElement("created");
            node.setAttribute("sym", sym);
            node.setAttribute("id", String.valueOf(accountID));
            this.responseRoot.appendChild(node);

        } catch (Exception e) {
            try {
                this.jdbcManager.getConnection().rollback();
            } catch (SQLException se) {
                this.addErrorNode(se.toString());
            }
            Element node = this.response.createElement("error");
            node.setAttribute("sym", sym);
            node.setAttribute("id", String.valueOf(accountID));
            node.appendChild(this.response.createTextNode(e.toString()));
            this.responseRoot.appendChild(node);
        }
    }

    private void processTransaction(Element transactionNode) throws SQLException {
        if (!checkAttribute(transactionNode, "id")){
            return;
        }
        int accountId = Integer.parseInt(transactionNode.getAttribute("id"));
        NodeList childList = transactionNode.getChildNodes();

        int count = 0;
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element node= (Element) childList.item(i);
                if (node.getNodeName().equals("order")) {
                    if (!checkAttribute(node, "sym") || !checkAttribute(node, "amount")
                            || !checkAttribute(node, "limit")) {
                        return;
                    }
                    String sym = node.getAttribute("sym");
                    String amount = node.getAttribute("amount");
                    String limit = node.getAttribute("limit");
                    if (sym.equals("") || amount.equals("") || limit.equals("")) {
                        this.addErrorNode("need symbol, amount and limit to open order");
                        return;
                    }
                    this.openOrder(accountId, sym, amount, limit);
                    count++;
                } else if (node.getNodeName().equals("query")) {
                    if (!checkAttribute(node, "id")) {
                        return;
                    }
                    String orderId = node.getAttribute("id");
                    if (orderId.equals("")) {
                        this.addErrorNode("need unique id of opened order to query");
                        return;
                    }
                    count++;
                    this.queryOrder(Integer.parseInt(orderId));

                } else if (node.getNodeName().equals("cancel")) {
                    if (!checkAttribute(node, "id")) {
                        return;
                    }
                    String orderId = node.getAttribute("id");
                    if (orderId.equals("")) {
                        this.addErrorNode("need unique id of opened order to cancel");
                        return;
                    }
                    count++;
                    this.cancelOrder(accountId, Integer.parseInt(orderId));

                } else {
                    this.addErrorNode("the root tag MUST have order, query, cancel child node");
                    return;
                }
            }
        }
        if (count == 0) {
            this.addErrorNode("the root tag MUST have children");
        }
    }

    private void openOrder(int accountID, String sym, String amount, String limit) throws PSQLException, SQLException {
        try {
            this.jdbcManager.getConnection().setAutoCommit(false);
            Account myaccount = new Account(accountID, jdbcManager);
            int id = myaccount.placeOrder(sym, Double.parseDouble(amount), Double.parseDouble(limit));
            this.jdbcManager.getConnection().commit();


            // add to response
            Element node = this.response.createElement("opened");
            node.setAttribute("sym", sym);
            node.setAttribute("amount", amount);
            node.setAttribute("limit", limit);
            node.setAttribute("id", String.valueOf(id));
            this.responseRoot.appendChild(node);

        } catch (Exception e) {
            try {
                this.jdbcManager.getConnection().rollback();
            } catch (SQLException se) {
                this.addErrorNode(se.toString());
            }
            Element node = this.response.createElement("error");
            node.setAttribute("sym", sym);
            node.setAttribute("amount", amount);
            node.setAttribute("limit", limit);
            node.appendChild(this.response.createTextNode(e.toString()));
            this.responseRoot.appendChild(node);
        }
    }

    private void queryOrder(int orderId) throws PSQLException, SQLException{
        try {
            // add to response
            Element node = this.response.createElement("status");
            node.setAttribute("id", String.valueOf(orderId));
            this.responseRoot.appendChild(node);

            // open status
            List<Order> orders = OrderUtils.findOrdersByIdAndStatus(this.jdbcManager, orderId, "OPEN");
            for(Order order: orders){
                Element childNode = this.response.createElement("open");
                childNode.setAttribute("shares", String.valueOf(order.getAmount()));
                node.appendChild(childNode);
            }

            // canceled
            orders = OrderUtils.findOrdersByIdAndStatus(this.jdbcManager, orderId, "CANCELED");
            for(Order order: orders){
                Element childNode = this.response.createElement("canceled");
                childNode.setAttribute("shares", String.valueOf(order.getAmount()));
                childNode.setAttribute("time", order.getTime().toString());
                node.appendChild(childNode);
            }


            //executed
            List<ExecutedOrder> executedOrders = OrderUtils.findExecutedOrdersById(jdbcManager, orderId);
            for(ExecutedOrder executedOrder: executedOrders){
                Element childNode = this.response.createElement("executed");
                childNode.setAttribute("shares", Double.toString(executedOrder.getAmount()));
                childNode.setAttribute("price", Double.toString(executedOrder.getLimitPrice()));
                childNode.setAttribute("time", executedOrder.getTime().toString());
                node.appendChild(childNode);
            }


        } catch (Exception e) {
            this.addErrorNode(e.toString());
        }
    }

    private void cancelOrder(int accountId, int orderId) throws PSQLException, SQLException{
        // add to response
        Element node = this.response.createElement("canceled");
        node.setAttribute("id", String.valueOf(orderId));
        responseRoot.appendChild(node);

        try{
            // cancel order
            this.jdbcManager.getConnection().setAutoCommit(false);
            Order order = new Order(jdbcManager, orderId);
            if(order.getAccountId() != accountId){
                throw new Exception("the account id doesn't match");
            }
            order.cancelOrder();
            this.jdbcManager.getConnection().commit();

            // add to response
            Element responseNode = this.response.createElement("canceled");
            responseNode.setAttribute("shares", String.valueOf(order.getAmount()));
            responseNode.setAttribute("time", order.getTime().toString());
            node.appendChild(responseNode);

            // parser executed orders
            List<ExecutedOrder> executedOrders = OrderUtils.findExecutedOrdersById(jdbcManager, orderId);
            for(ExecutedOrder executedOrder: executedOrders){
                responseNode = this.response.createElement("executed");
                responseNode.setAttribute("shares", String.valueOf(executedOrder.getAmount()));
                responseNode.setAttribute("price", String.valueOf(executedOrder.getLimitPrice()));
                responseNode.setAttribute("time", executedOrder.getTime().toString());
                node.appendChild(responseNode);
            }

        }
        catch(Exception e){
            this.jdbcManager.getConnection().rollback();

            Element errorNode = this.response.createElement("error");
            errorNode.setAttribute("id", Integer.toString(orderId));
            errorNode.appendChild(this.response.createTextNode(e.toString()));
            node.appendChild(errorNode);
        }

    }

}