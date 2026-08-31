package com.assignment.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "crypto_activity")
public class CryptoActivity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(nullable = false)
    private String blockchain;

    @Column(name = "wallet_address_from", nullable = false)
    private String walletAddressFrom;

    @Column(name = "wallet_address_to", nullable = false)
    private String walletAddressTo;

    @Column(name = "tx_hash", nullable = false)
    private String txHash;

    @Column(name = "exchange_name")
    private String exchangeName;

    protected CryptoActivity() {
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getBlockchain() {
        return blockchain;
    }

    public String getWalletAddressFrom() {
        return walletAddressFrom;
    }

    public String getWalletAddressTo() {
        return walletAddressTo;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getExchangeName() {
        return exchangeName;
    }
}
