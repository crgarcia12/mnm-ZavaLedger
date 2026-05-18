package com.zavabank.ledger;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public class TransactionServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");

        try {
            TransactionRequest transactionRequest = parseRequest(request);
            TransactionResult result = postTransaction(transactionRequest);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(toSuccessXml(result));
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("<transactionResponse><status>ERROR</status><message>" + LedgerXml.escape(exception.getMessage()) + "</message></transactionResponse>");
        } catch (Exception exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("<transactionResponse><status>ERROR</status><message>Unable to post transaction.</message></transactionResponse>");
        }
    }

    private TransactionRequest parseRequest(HttpServletRequest request) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(request.getInputStream());

        int debitAccountId = Integer.parseInt(readTag(document, "debitAccountId"));
        int creditAccountId = Integer.parseInt(readTag(document, "creditAccountId"));
        BigDecimal amount = new BigDecimal(readTag(document, "amount"));
        String description = readTag(document, "description");
        String referenceNumber = readTag(document, "referenceNumber");

        if (debitAccountId == creditAccountId) {
            throw new IllegalArgumentException("Debit and credit account IDs must be different.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.debitAccountId = debitAccountId;
        transactionRequest.creditAccountId = creditAccountId;
        transactionRequest.amount = amount;
        transactionRequest.description = description;
        transactionRequest.referenceNumber = referenceNumber;
        return transactionRequest;
    }

    private String readTag(Document document, String tagName) {
        if (document.getElementsByTagName(tagName).getLength() == 0) {
            return "";
        }
        String value = document.getElementsByTagName(tagName).item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private TransactionResult postTransaction(TransactionRequest request) throws SQLException {
        Connection connection = null;
        try {
            connection = LedgerConnectionFactory.openConnection();
            connection.setAutoCommit(false);

            BigDecimal debitCurrentBalance = readBalance(connection, request.debitAccountId);
            BigDecimal creditCurrentBalance = readBalance(connection, request.creditAccountId);
            if (debitCurrentBalance == null || creditCurrentBalance == null) {
                throw new IllegalArgumentException("One or both accounts do not exist.");
            }

            int debitTypeId = findTransactionType(connection, true);
            int creditTypeId = findTransactionType(connection, false);

            BigDecimal debitNewBalance = debitCurrentBalance.subtract(request.amount);
            BigDecimal creditNewBalance = creditCurrentBalance.add(request.amount);

            updateAccountBalance(connection, request.debitAccountId, debitNewBalance);
            updateAccountBalance(connection, request.creditAccountId, creditNewBalance);

            long debitTransactionId = insertTransaction(
                connection,
                request.debitAccountId,
                debitTypeId,
                request.amount.negate(),
                debitNewBalance,
                request.description,
                request.referenceNumber,
                String.valueOf(request.creditAccountId)
            );
            long creditTransactionId = insertTransaction(
                connection,
                request.creditAccountId,
                creditTypeId,
                request.amount,
                creditNewBalance,
                request.description,
                request.referenceNumber,
                String.valueOf(request.debitAccountId)
            );

            connection.commit();
            TransactionResult result = new TransactionResult();
            result.debitTransactionId = debitTransactionId;
            result.creditTransactionId = creditTransactionId;
            result.debitBalance = debitNewBalance;
            result.creditBalance = creditNewBalance;
            return result;
        } catch (SQLException | IllegalArgumentException exception) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw exception;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private BigDecimal readBalance(Connection connection, int accountId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT Balance FROM Accounts WHERE AccountID = ?");
        statement.setInt(1, accountId);
        ResultSet resultSet = statement.executeQuery();
        BigDecimal balance = null;
        if (resultSet.next()) {
            balance = resultSet.getBigDecimal(1);
        }
        resultSet.close();
        statement.close();
        return balance;
    }

    private int findTransactionType(Connection connection, boolean debit) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "SELECT TOP 1 TransactionTypeID FROM TransactionTypes WHERE IsDebit = ? AND IsActive = 1 ORDER BY TransactionTypeID"
        );
        statement.setBoolean(1, debit);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            int value = resultSet.getInt(1);
            resultSet.close();
            statement.close();
            return value;
        }
        resultSet.close();
        statement.close();
        throw new IllegalArgumentException("Transaction type configuration missing.");
    }

    private void updateAccountBalance(Connection connection, int accountId, BigDecimal newBalance) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "UPDATE Accounts SET Balance = ?, AvailableBalance = ?, LastActivityDate = GETDATE(), ModifiedDate = GETDATE() WHERE AccountID = ?"
        );
        statement.setBigDecimal(1, newBalance);
        statement.setBigDecimal(2, newBalance);
        statement.setInt(3, accountId);
        statement.executeUpdate();
        statement.close();
    }

    private long insertTransaction(
        Connection connection,
        int accountId,
        int transactionTypeId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        String referenceNumber,
        String counterpartyAccount
    ) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO Transactions (AccountID, TransactionTypeID, Amount, BalanceAfter, Description, ReferenceNumber, TransactionDate, PostDate, Status, Channel, CounterpartyAccount, Memo) " +
                "VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 'Posted', 'API', ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        statement.setInt(1, accountId);
        statement.setInt(2, transactionTypeId);
        statement.setBigDecimal(3, amount);
        statement.setBigDecimal(4, balanceAfter);
        statement.setString(5, description);
        statement.setString(6, referenceNumber);
        statement.setString(7, counterpartyAccount);
        statement.setString(8, description);
        statement.executeUpdate();
        ResultSet keys = statement.getGeneratedKeys();
        long transactionId = 0;
        if (keys.next()) {
            transactionId = keys.getLong(1);
        }
        keys.close();
        statement.close();
        return transactionId;
    }

    private String toSuccessXml(TransactionResult result) {
        return "<transactionResponse>" +
            "<status>POSTED</status>" +
            "<debitTransactionId>" + result.debitTransactionId + "</debitTransactionId>" +
            "<creditTransactionId>" + result.creditTransactionId + "</creditTransactionId>" +
            "<debitBalance>" + result.debitBalance + "</debitBalance>" +
            "<creditBalance>" + result.creditBalance + "</creditBalance>" +
            "</transactionResponse>";
    }

    private static class TransactionRequest {
        private int debitAccountId;
        private int creditAccountId;
        private BigDecimal amount;
        private String description;
        private String referenceNumber;
    }

    private static class TransactionResult {
        private long debitTransactionId;
        private long creditTransactionId;
        private BigDecimal debitBalance;
        private BigDecimal creditBalance;
    }
}
