package com.moneytransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.model.Account;
import com.moneytransfer.repository.AccountRepository;
import com.moneytransfer.repository.MoneyTransferRepository;
import com.moneytransfer.util.Constants;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class MoneyTransferService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferService.class);
	
	private static volatile MoneyTransferService instance;
	private static MoneyTransferRepository moneyTransferRepository;
	private static AccountRepository accountRepository;
	
    public static MoneyTransferService getInstance(){
	    if(instance == null){
	        synchronized (MoneyTransferService.class) {
	            if(instance == null){
	            	moneyTransferRepository = MoneyTransferRepository.getInstance();
	            	accountRepository = AccountRepository.getInstance();
	                instance = new MoneyTransferService();
	            }
	        }
	    }
	    return instance;
    }
    
    private MoneyTransferService() {
	}
    
    public Future<JsonObject> transfer( SQLConnection connection, Account fromAccount, Account toAccount, BigDecimal amount, String currency ) {

    	Future<JsonObject> result = Future.future();
    	
		while (true) {
			
			if (fromAccount.lock.tryLock()) {
				try {
					if (toAccount.lock.tryLock()) {
						try {
							
							if( amount.compareTo(BigDecimal.ZERO)<0 || amount.compareTo(BigDecimal.ZERO) == 0 ) {
				        		JsonObject response = new JsonObject().put("status","error").put("explanation",Constants.AmountIsLessThanOrEqualToZero);
			        			result.complete( response );
			        			break;
							}
							
					        if (fromAccount.getBalance().compareTo(amount) < 0) {
				        		JsonObject response = new JsonObject().put("status","error").put( "explanation",Constants.FromAccountBalanceIsNotEnough );
			        			result.complete( response );
			        			break;
					        }
							
							fromAccount.withdraw(amount);
							toAccount.deposit(amount);
							result = accountRepository.updateAccounts( connection, fromAccount, toAccount, amount, currency );
							break;
						} finally {
							toAccount.lock.unlock();
						}
					}
				} finally {
					fromAccount.lock.unlock();
				}

				try {
					Random number = new Random(123L);
					int n = number.nextInt(1000);
					int TIME = 1000 + n; // 1 second + random delay to prevent livelock
					Thread.sleep(TIME);
				} catch (InterruptedException e) {
					LOGGER.info(e.getLocalizedMessage());
				}
			}
		}
		
		return result;
	}
    
    public Future<List<JsonObject>> getTranfersForAccount( SQLConnection connection, String accountId ) {
    	return moneyTransferRepository.getTranfersForAccount(connection, accountId);
    }
	
}