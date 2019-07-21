package com.moneytransfer.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class MoneyTransferRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferRepository.class);
	
	private static volatile MoneyTransferRepository instance;
	
    public static MoneyTransferRepository getInstance(){
	    if(instance == null){
	        synchronized (MoneyTransferRepository.class) {
	            if(instance == null){
	                instance = new MoneyTransferRepository();
	            }
	        }
	    }
	    return instance;
    }
    
    private MoneyTransferRepository() {
    }
    
    public Future<List<JsonObject>> getTranfersForAccount( SQLConnection connection, String accountId ) {
    	
    	LOGGER.info( String.format( "Start get transfer for specific account by account ID : %s", accountId) );
    	
    	Future<List<JsonObject>> result = Future.future();
    	JsonArray params = new JsonArray().add(accountId);
    	
    	connection.queryWithParams("select * from money_transfer where fromAccountId=?", params, query -> {
    		
        	if ( query.failed() ) {
        		JsonObject response = new JsonObject().put("status","error").put("explanation","internal");
        		List<JsonObject> res = new ArrayList<>();
        		res.add(response);
        		result.complete( res );
            } else {
                if ( query.result() == null || query.result().getResults() == null || query.result().getResults().size() == 0) {
                	JsonObject response = new JsonObject().put("status","error").put("explanation","notfound");
                	List<JsonObject> res = new ArrayList<>();
                	res.add(response);
                	result.complete( res );
                } else {
                    JsonArray arr = new JsonArray();
                    query.result().getRows().forEach(arr::add);
              	    result.complete( arr.getList() );
                }
            }
    		
    	});
    	
    	LOGGER.info( String.format("End get transfer for specific account by account ID : %s", accountId));
    	return result;
    }
    
}