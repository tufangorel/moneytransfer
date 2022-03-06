package com.company.moneytransfer.service;


import java.util.List;

import com.company.moneytransfer.repository.UserRepository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class UserService {

	private static volatile UserService instance;
	private static UserRepository userRepositoryParam;
	
    public static UserService getInstance(){
	    if(instance == null){
	        synchronized (UserService.class) {
	            if(instance == null){
	            	userRepositoryParam = UserRepository.getInstance();
	                instance = new UserService();
	            }
	        }
	    }
	    return instance;
    }
    
    private UserService() {
    }
    
    public Promise<JsonObject> findUserById(SQLConnection connection, String userId) {
    	return userRepositoryParam.findUserById( connection, userId );
    }
    
    public Future<List<JsonObject>> getUsers(SQLConnection connection) {
    	return userRepositoryParam.getUsers(connection);
    }
    
}