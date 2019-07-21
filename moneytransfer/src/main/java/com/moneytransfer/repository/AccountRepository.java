package com.moneytransfer.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.model.Account;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

public class AccountRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountRepository.class);
	
	private static volatile AccountRepository instance;
	
	public static AccountRepository getInstance() {
	    if(instance == null){
	        synchronized (AccountRepository.class) {
	            if(instance == null){
	                instance = new AccountRepository();
	            }
	        }
	    }
	    return instance;
	}
	
	private AccountRepository() {
	}

	public Future<List<JsonObject>> findAccountsByUserId(SQLConnection connection, String userId) {
		
		LOGGER.info( String.format( "Start get accounts of a specific user by user ID %s", userId) );
		
		Future<List<JsonObject>> result = Future.future();
        JsonArray params = new JsonArray().add(userId);
        
        connection.queryWithParams("select * from account where userId = ?", params, query -> {
            
        	if (query.failed()) {
        		JsonObject response = new JsonObject().put("status","error").put("explanation","internal");
        		List<JsonObject> res = new ArrayList<>();
        		res.add(response);
        		result.complete( res );
            } else {
              if ( query.result() == null || query.result().getResults().size() == 0) {
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
		
        LOGGER.info( String.format( "End get accounts of a specific user by user ID %s", userId) );
		return result;
	}

	public Future<JsonObject> findAccountById(SQLConnection connection, Long accountId) {
		
		LOGGER.info( String.format( "Start querying account with id %d", accountId) );
		Future<JsonObject> result = Future.future();
		JsonArray params = new JsonArray().add(accountId);
		
		connection.querySingleWithParams("select * from account where id = ?", params, query -> {
			
        	if ( query.failed() ) {
        		JsonObject response = new JsonObject().put("status","error").put("explanation","internal");
        		result.complete( response );
            } else {
                if ( query.result() == null || query.result().getList() == null || query.result().getList().size() == 0) {
                	JsonObject response = new JsonObject().put("status","error").put("explanation","notfound");
                	result.complete( response );
                } else {
              	  io.vertx.core.json.JsonArray jsonArray = query.result();
              	  JsonObject response = new JsonObject().put("status","ok")
              			  .put("id", jsonArray.getList().get(0))
              			  .put("userId", jsonArray.getList().get(1))
              			  .put("balance", jsonArray.getList().get(2));
              	  result.complete( response );
                }
            }
        	
		});
		
		LOGGER.info( String.format( "End querying account with id %d", accountId) );
		return result;
	}
	
	public Future<JsonObject> updateAccounts( SQLConnection connection, Account fromAccount, Account toAccount, BigDecimal amount, String currency ) {

		LOGGER.info("Start update account balances");
		Future<JsonObject> result = Future.future();
		
		connection.setAutoCommit(false, res -> {
			
			String update = "update account SET balance = ? WHERE id=?";
			JsonArray params = new JsonArray().add(fromAccount.getBalance().toString()).add(fromAccount.getId());

			connection.updateWithParams(update, params, resUpdate -> {
				if (resUpdate.succeeded()) {
					LOGGER.info("Updated sender account balance!");
					
					String updateStatement = "update account SET balance = ? WHERE id=?";
					JsonArray paramsForUpdate = new JsonArray().add(toAccount.getBalance().toString()).add(toAccount.getId());

					connection.updateWithParams( updateStatement, paramsForUpdate, resUpdateAccount -> {

						if (resUpdateAccount.succeeded()) {
							LOGGER.info("Updated receiver account balance!");
							
							String insert = "insert into money_transfer(fromAccountId, toAccountId, amount, currency ) VALUES (?,?,?,?)";
							JsonArray paramsForLogInsert = new JsonArray().add(fromAccount.getId()).add(toAccount.getId())
													.add(amount.toString())
													.add(currency);
							
					        connection.updateWithParams( insert, paramsForLogInsert, create -> {
					        	
								if (create.succeeded()) {
									LOGGER.info("Inserted log record into the money_transfer table!");
									
									connection.commit( resCommit -> {
										
								        if ( resCommit.failed() ) {
							        		JsonObject response = new JsonObject().put("status","error").put("explanation","Transaction update failed!");
							        		result.complete( response );
								        	LOGGER.info("Transaction error occured!");
								        	
									        connection.rollback( resRollback -> {
									        	if (resRollback.failed()) {
								                	JsonObject responseRollback = new JsonObject().put("status","error").put("explanation","Error during money transfer rollback!");
								                	result.complete( responseRollback );
									            }
									        });
								        }
								        if (resCommit.succeeded()) {
						                	JsonObject response = new JsonObject().put("status","ok").put("explanation","Money transfer completed successfully!");
						                	result.complete( response );
								        	LOGGER.info("Transaction completed successfully!");
								        	LOGGER.info("End update account balances");
								        }
								    });
								} else {
									LOGGER.info("Could not insert log record into the money_transfer table!");
								}
					        });
						} else {
							LOGGER.info("Could not update receiver account balance!");
						}
					});
				} else {
					LOGGER.info("Could not update sender account balance!");
				}
			});

		});
		
		return result;
	}
	
}