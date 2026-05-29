# Data Architecture

```mermaid
erDiagram
    Accounts ||--o{ Transactions : "AccountID"
    TransactionTypes ||--o{ Transactions : "TransactionTypeID"

    Accounts {
        int AccountID PK
        decimal Balance
        decimal AvailableBalance
        datetime LastActivityDate
        datetime ModifiedDate
    }

    Transactions {
        bigint TransactionID PK
        int AccountID FK
        int TransactionTypeID FK
        decimal Amount
        decimal BalanceAfter
        string Description
        string ReferenceNumber
        datetime TransactionDate
        datetime PostDate
        string Status
        string Channel
        string CounterpartyAccount
        string Memo
    }

    TransactionTypes {
        int TransactionTypeID PK
        bool IsDebit
        bool IsActive
    }
```

The application uses SQL Server as the sole persistence store. Transaction posting updates account balances and inserts transaction ledger records within a single JDBC transaction.
