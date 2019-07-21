package com.moneytransfer.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.core.json.JsonObject;

public class Account implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Long id;
	private Long userId;
	private BigDecimal balance = BigDecimal.ZERO;
	
	public final Lock lock = new ReentrantLock();
	
	public Account() {
	}
	
	public Account( Long id ) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(amount);
    }

	public void deposit(BigDecimal amount) {
		balance = balance.add(amount);
	}

	public static Account createFromJSON(JsonObject res) {
		Long accountID = res.getLong("id");
		Long userId = res.getLong("userId");
		BigDecimal balance = new BigDecimal( res.getString("balance") );
		Account account = new Account();
		account.setBalance(balance);
		account.setId(accountID);
		account.setUserId(userId);
		return account;
	}
	
}