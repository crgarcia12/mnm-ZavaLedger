package com.zavabank.ledger;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccountServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/xml");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("<error><message>Invalid account path.</message></error>");
            return;
        }

        String[] segments = pathInfo.split("/");
        if (segments.length != 3) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("<error><message>Expected /api/accounts/{id}/balance or /api/accounts/{id}/transactions.</message></error>");
            return;
        }

        int accountId;
        try {
            accountId = Integer.parseInt(segments[1]);
        } catch (NumberFormatException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("<error><message>Account ID must be numeric.</message></error>");
            return;
        }

        if ("balance".equalsIgnoreCase(segments[2])) {
            writeBalance(response, accountId);
            return;
        }

        if ("transactions".equalsIgnoreCase(segments[2])) {
            writeTransactions(response, accountId);
            return;
        }

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.getWriter().write("<error><message>Endpoint not found.</message></error>");
    }

    private void writeBalance(HttpServletResponse response, int accountId) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = LedgerConnectionFactory.openConnection();
            statement = connection.prepareStatement("SELECT Balance, AvailableBalance FROM Accounts WHERE AccountID = ?");
            statement.setInt(1, accountId);
            resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("<balanceResponse><status>NOT_FOUND</status></balanceResponse>");
                return;
            }

            BigDecimal balance = resultSet.getBigDecimal("Balance");
            BigDecimal availableBalance = resultSet.getBigDecimal("AvailableBalance");
            response.getWriter().write(
                "<balanceResponse>" +
                    "<status>OK</status>" +
                    "<accountId>" + accountId + "</accountId>" +
                    "<balance>" + balance + "</balance>" +
                    "<availableBalance>" + availableBalance + "</availableBalance>" +
                    "</balanceResponse>"
            );
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("<balanceResponse><status>ERROR</status></balanceResponse>");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void writeTransactions(HttpServletResponse response, int accountId) throws IOException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = LedgerConnectionFactory.openConnection();
            statement = connection.prepareStatement(
                "SELECT TransactionID, Amount, BalanceAfter, Description, ReferenceNumber, TransactionDate, Status, CounterpartyAccount " +
                    "FROM Transactions WHERE AccountID = ? ORDER BY TransactionDate DESC, TransactionID DESC"
            );
            statement.setInt(1, accountId);
            resultSet = statement.executeQuery();

            StringBuilder xml = new StringBuilder();
            xml.append("<transactionsResponse><status>OK</status><accountId>").append(accountId).append("</accountId><transactions>");
            while (resultSet.next()) {
                xml.append("<transaction>")
                    .append("<transactionId>").append(resultSet.getLong("TransactionID")).append("</transactionId>")
                    .append("<amount>").append(resultSet.getBigDecimal("Amount")).append("</amount>")
                    .append("<balanceAfter>").append(resultSet.getBigDecimal("BalanceAfter")).append("</balanceAfter>")
                    .append("<description>").append(LedgerXml.escape(resultSet.getString("Description"))).append("</description>")
                    .append("<referenceNumber>").append(LedgerXml.escape(resultSet.getString("ReferenceNumber"))).append("</referenceNumber>")
                    .append("<counterpartyAccount>").append(LedgerXml.escape(resultSet.getString("CounterpartyAccount"))).append("</counterpartyAccount>")
                    .append("<status>").append(LedgerXml.escape(resultSet.getString("Status"))).append("</status>")
                    .append("<transactionDate>").append(resultSet.getTimestamp("TransactionDate")).append("</transactionDate>")
                    .append("</transaction>");
            }
            xml.append("</transactions></transactionsResponse>");
            response.getWriter().write(xml.toString());
        } catch (SQLException exception) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("<transactionsResponse><status>ERROR</status></transactionsResponse>");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
