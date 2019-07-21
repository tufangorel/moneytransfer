package com.moneytransfer.service;


import java.util.List;

import com.moneytransfer.repository.UserRepository;

import io.vertx.core.Future;
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
    
    public Future<JsonObject> findUserById(SQLConnection connection,String userId) {
    	return userRepositoryParam.findUserById( connection, userId );
    }
    
    public Future<List<JsonObject>> getUsers(SQLConnection connection) {
    	return userRepositoryParam.getUsers(connection);
    }
    
}