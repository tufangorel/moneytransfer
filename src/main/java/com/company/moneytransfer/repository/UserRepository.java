package com.company.moneytransfer.repository;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class UserRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);
	
	private static volatile UserRepository instance;
	
    public static UserRepository getInstance(){
	    if(instance == null){
	        synchronized (UserRepository.class) {
	            if(instance == null){
	                instance = new UserRepository();
	            }
	        }
	    }
	    return instance;
    }
    
    private UserRepository() {
    }
    
	public Promise<JsonObject> findUserById(SQLConnection connection,String userId) {
		
		LOGGER.info( String.format("Start find user by user ID : %s", userId) );

		Promise<JsonObject> result = Promise.promise();
		JsonArray params = new JsonArray().add(userId);
		
		connection.querySingleWithParams("select * from user where id = ?", params, query -> {
			
        	if ( query.failed() ) {
        		JsonObject response = new JsonObject().put("status","error").put("explanation","internal");
        		result.complete( response );
            } else {
                if ( query.result() == null || query.result().getList() == null || query.result().getList().size() == 0) {
                	JsonObject response = new JsonObject().put("status","error").put("explanation","notfound");
                	result.complete( response );
                } else {
              	  io.vertx.core.json.JsonArray jsonArray = query.result();
              	  JsonObject response = new JsonObject().put("status","ok").put("ID", jsonArray.getList().get(0)).put("name", jsonArray.getList().get(1));
              	  result.complete( response );
                }
            }
        	
		});
		
		LOGGER.info( String.format("End find user by user ID : %s", userId) );
		return result;
	}
	
	public Future<List<JsonObject>> getUsers(SQLConnection connection) {
		
		LOGGER.info("Start get all users in the system.");
		Promise<List<JsonObject>> result = Promise.promise();

		connection.query("select * from user", query -> {
			
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
		
		LOGGER.info("End get all users in the system.");
		return result.future();
	}

}
