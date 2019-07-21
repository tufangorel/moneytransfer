package test.com.moneytransfer.vertices;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.model.MoneyTransfer;
import com.moneytransfer.util.Constants;
import com.moneytransfer.verticles.MoneyTransferVerticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class MoneyTransferVerticleTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferVerticleTest.class);

	private final static int PORT = 9090;
	private static Vertx vertx;

	@BeforeClass
	public static void setUp(TestContext context) throws IOException {

		LOGGER.info("-- Start MoneyTransferVerticleTest execution! -- ");
		vertx = Vertx.vertx();
		final DeploymentOptions options = new DeploymentOptions().setConfig(
				new JsonObject().put("http.port", PORT));

		// default config
		String verticleID = MoneyTransferVerticle.class.getName();
		vertx.deployVerticle( verticleID, options, context.asyncAssertSuccess());
	}
	
	@AfterClass
	public static void tearDown(TestContext context) throws IOException {
		String verticleID = MoneyTransferVerticle.class.getName();
		vertx.undeploy(verticleID);
		vertx.close(context.asyncAssertSuccess());
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LOGGER.info("-- End MoneyTransferVerticleTest execution! --");
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetUserByUserID(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users/1", response -> response.bodyHandler(body -> {

			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
			
			int ID = body.toJsonObject().getInteger("ID");
			String name = body.toJsonObject().getString("name");
			assertNotNull(body.toJsonObject());
			assertEquals( 1, ID);
			assertEquals( "user1", name );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetUsers(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users", response -> response.bodyHandler(body -> {

			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
			
			JsonArray jsonArray = body.toJsonArray();
			assertNotNull(jsonArray);
			assertTrue( jsonArray.size() == 2 );
			
			JsonObject account1 = jsonArray.getJsonObject(0);
			int accountID = account1.getInteger("ID");
			String name1 = account1.getString("NAME");
			assertEquals( 1, accountID);
			assertEquals( "user1", name1 );
			
			JsonObject account2 = jsonArray.getJsonObject(1);
			int accountID2 = account2.getInteger("ID");
			String userName2 = account2.getString("NAME");
			assertEquals( 2, accountID2 );
			assertEquals( "user2", userName2 );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetUserByUserID_Not_Found(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users/3", response -> response.bodyHandler(body -> {

			JsonObject responseFromService = body.toJsonObject();
			String errorMessage = responseFromService.getString("errorMessage");
			assertEquals( HttpResponseStatus.NOT_FOUND.code(), response.statusCode() );
			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals( "Record not found!", errorMessage );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetUserByUserID_INTERNAL_SERVER_ERROR(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users/null", response -> response.bodyHandler(body -> {

			JsonObject responseFromService = body.toJsonObject();
			String errorMessage = responseFromService.getString("errorMessage");
			assertEquals( HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), response.statusCode() );
			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals( Constants.InternalServerErrorOccured, errorMessage );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetAccountsByUserId_INTERNAL_SERVER_ERROR(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/accounts/null", response -> response.bodyHandler(body -> {

			JsonObject responseFromService = body.toJsonObject();
			String errorMessage = responseFromService.getString("errorMessage");
			assertEquals( HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), response.statusCode() );
			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals( Constants.InternalServerErrorOccured, errorMessage );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetAccountsByUserId_NOT_FOUND(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/accounts/13", response -> response.bodyHandler(body -> {

			JsonObject responseFromService = body.toJsonObject();
			String errorMessage = responseFromService.getString("errorMessage");
			assertEquals( HttpResponseStatus.NOT_FOUND.code(), response.statusCode() );
			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertEquals( Constants.RecordNotFound, errorMessage );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_OK(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily(new MoneyTransfer( (long)1, (long)2, amount, "EUR"));
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.OK.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String transferResult = responseFromService.getString("transferResult");
					JsonObject SenderAccountDetails = responseFromService.getJsonObject("SenderAccountDetails");
					JsonObject ReceiverAccountDetails = responseFromService.getJsonObject("ReceiverAccountDetails");
					assertEquals( "Money transfer completed successfully!", transferResult );
					assertNotNull(SenderAccountDetails);
					assertNotNull(ReceiverAccountDetails);
					
					String SenderAccountDetailsStatus = SenderAccountDetails.getString("status");
					int SenderAccountDetailsID = SenderAccountDetails.getInteger("id");
					int SenderAccountDetailsUserID = SenderAccountDetails.getInteger("userId");
					double userIdBalance = SenderAccountDetails.getDouble("balance");
					assertEquals( "ok", SenderAccountDetailsStatus );
					assertEquals( 1, SenderAccountDetailsID );
					assertEquals( 1, SenderAccountDetailsUserID );
					assertEquals( 90.0, userIdBalance, 0.0 );
					
					String ReceiverAccountDetailsStatus = ReceiverAccountDetails.getString("status");
					int ReceiverAccountDetailsID = ReceiverAccountDetails.getInteger("id");
					int ReceiverAccountDetailsUserID = ReceiverAccountDetails.getInteger("userId");
					double userIdBalanceReceiver = ReceiverAccountDetails.getDouble("balance");
					assertEquals( "ok", ReceiverAccountDetailsStatus );
					assertEquals( 2, ReceiverAccountDetailsID );
					assertEquals( 2, ReceiverAccountDetailsUserID );
					assertEquals( 210.0, userIdBalanceReceiver, 0.0 );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_Sender_Account_ID_Required(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily(new MoneyTransfer( null, (long)2, amount, "EUR"));
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.FromAccountIDIsRequired, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_Receiver_Account_ID_Required(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily(new MoneyTransfer( (long)1, null, amount, "EUR"));
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.ToAccountIDIsRequired, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_Amount_IS_Required(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)2, null, "EUR") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.AmountIsRequired, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_CURRENCY_IS_Required(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)2, amount, null) );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.CurrencyIsRequired, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_SENDER_AND_RECEIVER_ACOUNT_IDS_MUST_BE_DIFFERENT(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)1, amount, "TRL") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.SenderAndReceiverAccountIDsMustBeDifferent, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_SENDER_ACCOUNT_ID_NOT_FOUND_IN_SYSTEM(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)11, (long)1, amount, "TRL") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.FromAccountIDNotFoundInSystem, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_RECEIVER_ACCOUNT_ID_NOT_FOUND_IN_SYSTEM(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(10.0);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)12, amount, "TRL") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.BAD_REQUEST.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.ToAccountIDNotFoundInSystem, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_AMOUNT_IS_LESS_THAN_OR_EQUAL_TO_ZERO(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(0);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)2, amount, "TRL") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.NOT_ACCEPTABLE.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.AmountIsLessThanOrEqualToZero, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testTransfer_SENDER_ACCOUNT_BALANCE_IS_NOT_ENOUGH(TestContext context) throws Exception {
		
		HttpClient client = vertx.createHttpClient();
		BigDecimal amount = BigDecimal.valueOf(2000);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)2, amount, "TRL") );
		final String length = Integer.toString(json.length());
		Async async = context.async();
		client.post( PORT, "localhost", "/api/transfer")
		      .putHeader("content-type", "application/json")
		      .putHeader("content-length", length)
		      .handler(response -> {
		    	  
		    	assertEquals(response.statusCode(), HttpResponseStatus.NOT_ACCEPTABLE.code());
		        assertTrue(response.headers().get("content-type").contains("application/json"));
		        response.bodyHandler(body -> {
		        	
		        	LOGGER.info(body.toString());
					JsonObject responseFromService = body.toJsonObject();
					
					String errorMessage = responseFromService.getString("errorMessage");
					assertEquals( Constants.FromAccountBalanceIsNotEnough, errorMessage );
					
		          client.close();
				  async.complete();
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetAccountsByUserId(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		
		client.getNow(PORT, "localhost", "/api/accounts/1", response -> response.bodyHandler(body -> {

			JsonArray jsonArray = body.toJsonArray();
			assertEquals( HttpResponseStatus.OK.code(), response.statusCode() );
			assertTrue(response.headers().get("content-type").contains("application/json"));
			assertNotNull(jsonArray);
			assertTrue(jsonArray.size() > 0);
			
			JsonObject account = jsonArray.getJsonObject(0);
			int accountID = account.getInteger("ID");
			int userID = account.getInteger("USERID");
			
			assertEquals( 1, accountID);
			assertEquals( 1, userID );
			
			client.close();
			async.complete();
		}));
		
		async.awaitSuccess();
	}
	
}