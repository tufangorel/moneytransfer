package com.moneytransfer.service;

import java.util.List;

import com.moneytransfer.repository.AccountRepository;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class AccountService {

	private static volatile AccountService instance;
	private static AccountRepository accountRepositoryParam;
	
    public static AccountService getInstance(){
	    if(instance == null){
	        synchronized (AccountService.class) {
	            if(instance == null){
	            	accountRepositoryParam = AccountRepository.getInstance();
	                instance = new AccountService();
	            }
	        }
	    }
	    return instance;
    }
    
    private AccountService() {
    }
    
    public Future<List<JsonObject>> getAccountsByUserId(SQLConnection connection, String userId) {
    	return accountRepositoryParam.findAccountsByUserId(connection,userId);
    }
    
    public Future<JsonObject> getAccountById( SQLConnection connection, Long accountId ) {
    	return accountRepositoryParam.findAccountById( connection,accountId );
    }
    
}
