package com.moneytransfer.verticles;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.model.Account;
import com.moneytransfer.service.AccountService;
import com.moneytransfer.service.MoneyTransferService;
import com.moneytransfer.service.UserService;
import com.moneytransfer.util.Constants;
import com.moneytransfer.util.DBUtil;

import io.netty.handler.codec.http.HttpResponseStatus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;


public class MoneyTransferVerticle extends AbstractVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferVerticle.class);
	
	private JDBCClient client;
	private HttpServer httpServer;
	
    @Override
    public void start(Future<Void> startFuture) throws Exception {

        // Create a JDBC client with a test database
		client = JDBCClient.createShared(vertx,
				new JsonObject().put("url", "jdbc:hsqldb:mem:test?shutdown=true")
						.put("driver_class", "org.hsqldb.jdbcDriver")
						.put("max_pool_size", 30)
						.put("user", "SA")
						.put("password", ""));
		
		DBUtil.setUpInitialData( vertx );
		
		// Create a router object.
        Router router = Router.router(vertx);
        
        Future<Void> steps = startHttpServer(router);
        steps.setHandler(ar -> {
          if (ar.succeeded()) {
            startFuture.complete();
          } else {
            startFuture.fail(ar.cause());
          }
        });
        
    }
    
    @Override
    public void stop(Future<Void> stopFuture) {
        httpServer.close( ar -> {
        	LOGGER.info("HTTP server stopped!");
            stopFuture.complete();
        });
        
        vertx.close(h -> {
            if (h.failed()) {
            	LOGGER.error(h.cause().getLocalizedMessage());
            }
        });
    }
    
	private Future<Void> startHttpServer(Router router) {
		Future<Void> future = Future.future();
		httpServer = vertx.createHttpServer();

        Set<HttpMethod> allowMethods = new HashSet<>();
        allowMethods.add(HttpMethod.GET);
        allowMethods.add(HttpMethod.POST);

        router.route().handler(CorsHandler.create("*")
          .allowedMethods(allowMethods));
        router.route("/api*").handler(BodyHandler.create());
        
        // in order to minimize the nesting of call backs we can put the JDBC connection on the context for all routes
        // that match /api
        // this should really be encapsulated in a reusable JDBC handler that uses can just add to their app
        router.route("/api*").handler( routingContext -> client.getConnection(res -> {
          if (res.failed()) {
            routingContext.fail(res.cause());
          } else {
        	SQLConnection conn = res.result();

            // save the connection on the context
            routingContext.put("conn", conn);

            // we need to return the connection back to the jdbc pool. In order to do that we need to close it, to keep
            // the remaining code readable one can add a headers end handler to close the connection.
            routingContext.addHeadersEndHandler(done -> conn.close(v -> { }));

            routingContext.next();
          }
        })).failureHandler(routingContext -> {
          SQLConnection conn = routingContext.get("conn");
          if (conn != null) {
            conn.close(v -> {
            });
          }
        });
        
		router.post().handler(BodyHandler.create());
		router.get("/api/users").handler(this::getUsers);
		router.get("/api/users/:id").handler(this::getUserById);
		router.get("/api/accounts/:userid").handler(this::getAccountsByUserId);
		router.post("/api/transfer").handler(this::transfer);
		router.get("/api/transfer/:accountid").handler(this::getTranfersForAccount);
		
		httpServer.requestHandler(router).listen(9090, ar -> {
			if (ar.succeeded()) {
				LOGGER.info("HTTP server running on port 9090");
				future.complete();
			} else {
				LOGGER.error("Could not start a HTTP server", ar.cause());
				future.fail(ar.cause());
			}
		});

		return future;
	}
	
	/**
	 * Get list of all users from the system.
	 * HTTP.METHOD = GET
	 * @param routingContext
	 */
    private void getUsers(RoutingContext routingContext) {
    	
    	LOGGER.info("Start get list of all users from the system.");
    	HttpServerResponse response = routingContext.response();
    	SQLConnection conn = routingContext.get("conn");
    	
    	Future<List<JsonObject>> resp = UserService.getInstance().getUsers(conn);
    	resp.setHandler( resultHandler( routingContext, res -> {
        	
            if ( res.size() == 1 && res.get(0).getString("status").equals("error") && res.get(0).getString("explanation").equals("internal") ) {
          	  
	      	  JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
	      	  response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
	      	  	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
	      	  	.end(message.encodePrettily());
          	
            } else if ( res.size() == 1 && res.get(0).getString("status").equals("error") && res.get(0).getString("explanation").equals("notfound") ) {
          	  
              JsonObject message = new JsonObject().put("errorMessage", Constants.RecordNotFound);
              response.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
              	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
              	.end( message.encodePrettily() );
              
            } else if ( res.size() >= 1 ) {
          	  
                JsonArray arr = new JsonArray();
                res.forEach(arr::add);
                response.setStatusCode(HttpResponseStatus.OK.code())
                	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                	.end( arr.encodePrettily() );
            }
            
        }));
    
    	LOGGER.info("End get list of all users from the system.");
    }
    
    /**
     * Get specific user by user ID
     * HTTP.METHOD = GET
     * @param routingContext
     */
    private void getUserById(RoutingContext routingContext) {
    	
    	LOGGER.info("Start get specific user by user ID.");
    	HttpServerResponse response = routingContext.response();
    	SQLConnection conn = routingContext.get("conn");
        String userId = routingContext.pathParam("id");
        
        if (Objects.isNull(userId)) {
        	JsonObject message = new JsonObject().put("errorMessage", Constants.UserIdIsNULL);
      	  	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
	  	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
	  	  		.end( message.encodePrettily() );
            return;
        }
        
        Future<JsonObject> resp = UserService.getInstance().findUserById( conn, userId );
        resp.setHandler( resultHandler( routingContext, res -> {
        	
          if ( res.getString("status").equals("error") && res.getString("explanation").equals("internal") ) {
        	  
        	JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
        	response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
        		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
        	  	.end( message.encodePrettily() );
        	
          } else if ( res.getString("status").equals("error") && res.getString("explanation").equals("notfound") ) {
        	  
            JsonObject message = new JsonObject().put("errorMessage", Constants.RecordNotFound);
            response.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	.end( message.encodePrettily() );
            
          } else if ( res.getString("status").equals("ok") ) {
        	  
        	JsonObject user = new JsonObject().put("ID", res.getMap().get("ID"))
        							.put("name", res.getMap().get("name"));
            response.setStatusCode(HttpResponseStatus.OK.code())
            	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .end( user.encode() );
          }
          
        }));
      
        LOGGER.info("End get specific user by user ID.");
    }
    
    /**
     * Get accounts of a specific user by user ID
     * HTTP.METHOD = GET
     * @param routingContext
     */
    private void getAccountsByUserId(RoutingContext routingContext) {
    	
    	LOGGER.info("Start get accounts of a specific user by user ID.");
    	HttpServerResponse response = routingContext.response();
    	SQLConnection conn = routingContext.get("conn");
        String userId = routingContext.pathParam("userid");
        
        if ( Objects.isNull(userId) ) {
        	JsonObject message = new JsonObject().put("errorMessage", Constants.UserIdIsNULL);
      	  	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
	  	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
	  	  		.end( message.encodePrettily() );
            return;
        }
        
        Future<List<JsonObject>> resp = AccountService.getInstance().getAccountsByUserId(conn,userId);
    	resp.setHandler( resultHandler( routingContext, res -> {
        	
            if ( res.size() == 1 && res.get(0).getString("status") != null &&
            	 res.get(0).getString("status").equals("error") && 
            	 res.get(0).getString("explanation") != null &&
            	 res.get(0).getString("explanation").equals("internal") ) {
          	  
          	  JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
          	  response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
          	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
          	  		.end( message.encodePrettily() );
          	
            } else if ( res.size() == 1 && res.get(0).getString("status") != null &&
            			res.get(0).getString("status").equals("error") &&
            			res.get(0).getString("explanation") != null &&
            			res.get(0).getString("explanation").equals("notfound") ) {
          	  
            	JsonObject message = new JsonObject().put("errorMessage", Constants.RecordNotFound);
            	response.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            		.end( message.encodePrettily() );
              
            } else if ( res.size() >= 1 ) {
          	  
                JsonArray arr = new JsonArray();
                res.forEach(arr::add);
                response.setStatusCode(HttpResponseStatus.OK.code())
                	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                	.end( arr.encodePrettily() );
            }
            
        }));
        
    	LOGGER.info("End get accounts of a specific user by user ID.");
    }
    
    /**
     * Internal money transfer between 2 different accounts
     * HTTP.METHOD = POST
     * @param routingContext
     */
    private void transfer(RoutingContext routingContext) {
    	
    	LOGGER.info("Start internal money transfer between 2 different accounts.");
    	HttpServerResponse response = routingContext.response();
    	SQLConnection conn = routingContext.get("conn");
    	
    	try {

    		com.moneytransfer.model.MoneyTransfer moneyTransfer = Json.decodeValue( routingContext.getBodyAsString(),
        			com.moneytransfer.model.MoneyTransfer.class );
        	
        	if( Objects.isNull( moneyTransfer.getFromAccountId() )) {
    			LOGGER.info(Constants.FromAccountIDIsRequired);
            	JsonObject message = new JsonObject().put("errorMessage", Constants.FromAccountIDIsRequired);
            	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	  	.end( message.encodePrettily() );
            	LOGGER.info("End internal money transfer between 2 different accounts.");
            	return;
        	}
        	if( Objects.isNull( moneyTransfer.getToAccountId() )) {
    			LOGGER.info(Constants.ToAccountIDIsRequired);
            	JsonObject message = new JsonObject().put("errorMessage", Constants.ToAccountIDIsRequired);
            	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	  	.end( message.encodePrettily() );
            	LOGGER.info("End internal money transfer between 2 different accounts.");
            	return;
        	}
        	if( Objects.isNull( moneyTransfer.getAmount() )) {
    			LOGGER.info(Constants.AmountIsRequired);
            	JsonObject message = new JsonObject().put("errorMessage", Constants.AmountIsRequired);
            	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	  	.end( message.encodePrettily() );
            	LOGGER.info("End internal money transfer between 2 different accounts.");
            	return;
        	}
        	if( Objects.isNull( moneyTransfer.getCurrency() )) {
    			LOGGER.info(Constants.CurrencyIsRequired);
            	JsonObject message = new JsonObject().put("errorMessage", Constants.CurrencyIsRequired);
            	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	  	.end( message.encodePrettily() );
            	LOGGER.info("End internal money transfer between 2 different accounts.");
            	return;
        	}
        	
        	if( moneyTransfer.getFromAccountId().compareTo(moneyTransfer.getToAccountId()) == 0 ) {
    			LOGGER.info(Constants.SenderAndReceiverAccountIDsMustBeDifferent);
            	JsonObject message = new JsonObject().put("errorMessage", Constants.SenderAndReceiverAccountIDsMustBeDifferent);
            	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            	  	.end( message.encodePrettily() );
            	LOGGER.info("End internal money transfer between 2 different accounts.");
            	return;
        	}
        	
        	Future<JsonObject> fromAccountQueryResponse = AccountService.getInstance().getAccountById(conn, moneyTransfer.getFromAccountId());
        	fromAccountQueryResponse.setHandler( resultHandler( routingContext, resFromAccount -> {
        		
        		if ( resFromAccount.getString("status").equals("error") && resFromAccount.getString("explanation").equals("internal") ) {
        			JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
        			response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                	  		.end( message.encodePrettily() );
        			LOGGER.info("End internal money transfer between 2 different accounts.");
        			return;
                }
        		if( resFromAccount.getString("status").equals("error") && resFromAccount.getString("explanation").equals("notfound") ) {
        			JsonObject message = new JsonObject().put("errorMessage", Constants.FromAccountIDNotFoundInSystem);
        			response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                	  		.end( message.encodePrettily() );
        			LOGGER.info("End internal money transfer between 2 different accounts.");
        			return;
        		}
        		if( resFromAccount.getString("status").equals("ok") ) {
        			
        			Future<JsonObject> toAccountQueryResponse = AccountService.getInstance().getAccountById(conn, moneyTransfer.getToAccountId());
                	toAccountQueryResponse.setHandler( resultHandler( routingContext, resToAccount -> {
                		
                		if ( resToAccount.getString("status").equals("error") && resToAccount.getString("explanation").equals("internal") ) {
                			JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
                			response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                        	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                        	  		.end( message.encodePrettily() );
                			LOGGER.info("End internal money transfer between 2 different accounts.");
                			return;
                        }
                		
                		if( resToAccount.getString("status").equals("error") && resToAccount.getString("explanation").equals("notfound") ) {
                			JsonObject message = new JsonObject().put("errorMessage", Constants.ToAccountIDNotFoundInSystem);
                			response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                        	  		.end( message.encodePrettily() );
                			LOGGER.info("End internal money transfer between 2 different accounts.");
                			return;
                		}
                		
                		if( resToAccount.getString("status").equals("ok") ) {
                			
                			CompositeFuture.all(fromAccountQueryResponse,toAccountQueryResponse).setHandler( r-> {
                                
                        		final Account[] accounts = {null,null};
                        		
                        		if( fromAccountQueryResponse.succeeded() && fromAccountQueryResponse.isComplete() ) {
                            		JsonObject fromAccountJSON = fromAccountQueryResponse.result();
                            		if( fromAccountJSON != null ) {
                            			
                            			Long fromAccountID = fromAccountJSON.getLong("id");
                            			Long userId = fromAccountJSON.getLong("userId");
                            			BigDecimal balance = new BigDecimal( fromAccountJSON.getDouble("balance") );
                            			
                            			fromAccountJSON = new JsonObject()
                                    			  .put("id", fromAccountID )
                                    			  .put("userId", userId )
                                    			  .put("balance", balance.toString() );
                            			
                            			accounts[0] = Account.createFromJSON(fromAccountJSON);
                            		}
                            		
                            		if( toAccountQueryResponse.succeeded() && toAccountQueryResponse.isComplete() ) {
                            			
                                        JsonObject toAccountJSON = toAccountQueryResponse.result();
                                		if( toAccountJSON != null ) {
                                			
                                			Long accountID = Long.valueOf( toAccountJSON.getLong("id") );
                                			Long userId = Long.valueOf( toAccountJSON.getLong("userId") );
                                			BigDecimal balance = new BigDecimal( toAccountJSON.getDouble("balance") );
                                			
                                			toAccountJSON = new JsonObject()
                                        			  .put("id", accountID )
                                        			  .put("userId", userId )
                                        			  .put("balance", balance.toString() );
                                			
                                			accounts[1] = Account.createFromJSON(toAccountJSON);
                                		}
                                		
                                    	Future<JsonObject> resp = MoneyTransferService.getInstance().transfer( conn, accounts[0], accounts[1],
                       					     										moneyTransfer.getAmount(), moneyTransfer.getCurrency() );
                    	   				resp.setHandler( resultHandler( routingContext, resTransfer -> {
                    	   				
                    	            		if ( resp.succeeded() && resp.isComplete() && resTransfer.getString("status").equals("error") ) {
                    	            			JsonObject message = new JsonObject().put("errorMessage", resTransfer.getString("explanation"));
                    	            			response.setStatusCode(HttpResponseStatus.NOT_ACCEPTABLE.code())
                    	                    	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                    	                    	  		.end( message.encodePrettily() );
                    	            			LOGGER.info("End internal money transfer between 2 different accounts.");
                    	            			return;
                    	                    }
                    	            		
                    	            		if( resp.succeeded() && resp.isComplete() && resTransfer.getString("status").equals("ok") ) {
                    	            			
                    							Future<JsonObject> resultFromAccount = AccountService.getInstance().getAccountById(conn, accounts[0].getId());
                    							resultFromAccount.setHandler( resultHandler( routingContext, resTransferQueryFromAccountValue -> {
                    								
                    								if( resultFromAccount.succeeded() && resultFromAccount.isComplete() 
                    									&& resTransferQueryFromAccountValue.getString("status").equals("ok")) {
                        								Future<JsonObject> resultToAccount = AccountService.getInstance().getAccountById(conn, accounts[1].getId());
                        								resultToAccount.setHandler( resultHandler( routingContext, resTransferQueryToAccountValue -> {
                        									
                        									if( resultToAccount.succeeded() && resultToAccount.isComplete() 
                        										&& resTransferQueryToAccountValue.getString("status").equals("ok")) {
                        										
                                    							JsonObject fromAccountLatest = resultFromAccount.result();
                                    							JsonObject toAccountLatest = resultToAccount.result();
                                    	            			JsonObject message = new JsonObject()
                                    	            					.put("transferResult", resTransfer.getString("explanation"))
                                    	            					.put("SenderAccountDetails", fromAccountLatest)
                                    	            					.put("ReceiverAccountDetails", toAccountLatest);
                                    	            			response.setStatusCode(HttpResponseStatus.OK.code())
                                    	                    	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                    	                    	  		.end( message.encodePrettily() );
                                    	            			
                                    	            			LOGGER.info("End internal money transfer between 2 different accounts.");
                        									}
                        								}));
                    								}
                    							}));
                    	            		}
                    	   				}));
                            		}
                        		}
                    		});
                		}
                	}));
        		}
        	}));
        	
		} catch (Exception e) {
			LOGGER.info(e.getLocalizedMessage());
        	JsonObject message = new JsonObject().put("errorMessage", Constants.MoneyTransferFormatIsNotValid);
        	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
        		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
        	  	.end( message.encodePrettily() );
        	LOGGER.info("End internal money transfer between 2 different accounts.");
		}
    }
    
    /**
     * Get list of transfers of a specific account from the system
     * HTTP.METHOD = GET
     * @param routingContext
     */
    private void getTranfersForAccount(RoutingContext routingContext) {
    	
    	LOGGER.info("Start get list of transfers of a specific account from the system.");
    	HttpServerResponse response = routingContext.response();
    	SQLConnection conn = routingContext.get("conn");
        String accountId = routingContext.pathParam("accountid");
        
        if ( Objects.isNull(accountId) ) {
        	JsonObject message = new JsonObject().put("errorMessage", Constants.AccountIdIsNULL);
      	  	response.setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
	  	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
	  	  		.end( message.encodePrettily() );
            return;
        }
        
        Future<List<JsonObject>> resp = MoneyTransferService.getInstance().getTranfersForAccount( conn, accountId );
    	resp.setHandler( resultHandler( routingContext, res -> {
        	
            if ( res.size() == 1 && res.get(0).getString("status") != null &&
            	 res.get(0).getString("status").equals("error") &&
            	 res.get(0).getString("explanation") != null &&
            	 res.get(0).getString("explanation").equals("internal") ) {
          	  
          	  JsonObject message = new JsonObject().put("errorMessage", Constants.InternalServerErrorOccured);
          	  response.setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
          	  		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
          	  		.end( message.encodePrettily() );
          	
            } else if ( res.size() == 1 && res.get(0).getString("status") != null &&
            			res.get(0).getString("status").equals("error") &&
            			res.get(0).getString("explanation") != null &&
            			res.get(0).getString("explanation").equals("notfound") ) {
          	  
            	JsonObject message = new JsonObject().put("errorMessage", Constants.RecordNotFound);
            	response.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
            		.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
            		.end( message.encodePrettily() );
              
            } else if ( res.size() >= 1 ) {
          	  
                JsonArray arr = new JsonArray();
                res.forEach(arr::add);
                response.setStatusCode(HttpResponseStatus.OK.code())
                	.putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                	.end( arr.encodePrettily() );
            }
            
        }));
        
    	LOGGER.info("End get list of transfers of a specific account from the system.");
    }
    
	private <T> Handler<AsyncResult<T>> resultHandler(RoutingContext context, Consumer<T> consumer) {
		return res -> {
			if (res.succeeded()) {
				consumer.accept(res.result());
			} else {
				context.response().setStatusCode(503).end();
			}
		};
	}
    
}