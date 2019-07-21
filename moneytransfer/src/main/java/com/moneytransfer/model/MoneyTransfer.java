package com.moneytransfer.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class MoneyTransfer implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String uuid;
	private Long fromAccountId;
	private Long toAccountId;
	private BigDecimal amount;
	private String currency;
	private Date transferDate;
	
	public MoneyTransfer( Long fromAccountId, Long toAccountId, BigDecimal amount, String currency  ) {
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amount= amount;
		this.currency= currency;
	}
	
	public MoneyTransfer() {
	}
	
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getUuid() {
		return uuid;
	}

	public Long getFromAccountId() {
		return fromAccountId;
	}

	public void setFromAccountId(Long fromAccountId) {
		this.fromAccountId = fromAccountId;
	}

	public Long getToAccountId() {
		return toAccountId;
	}

	public void setToAccountId(Long toAccountId) {
		this.toAccountId = toAccountId;
	}

	public Date getTransferDate() {
		return transferDate;
	}

	public void setTransferDate(Date transferDate) {
		this.transferDate = transferDate;
	}

}