package test.integration.com.moneytransfer.vertices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.model.MoneyTransfer;
import com.moneytransfer.verticles.MoneyTransferVerticle;

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


@RunWith(VertxUnitRunner.class)
public class MoneyTransferVerticleIntegrationTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferVerticleIntegrationTest.class);

	private final static int PORT = 9090;
	private static Vertx vertx;

	@BeforeClass
	public static void setUp(TestContext context) throws IOException {

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
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		LOGGER.info("-- End MoneyTransferVerticleTest execution! --");
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetTransfersOfAccount(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		
		BigDecimal amount = BigDecimal.valueOf(1);
		final String json = Json.encodePrettily( new MoneyTransfer( (long)1, (long)2, amount, "TRL") );
		final String length = Integer.toString(json.length());
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
					assertEquals( "Money transfer completed successfully!", transferResult );
					
					client.getNow(PORT, "localhost", "/api/transfer/1", responseGetTransfer -> responseGetTransfer.bodyHandler( bodyGetTransfer -> {

						JsonArray jsonArray = bodyGetTransfer.toJsonArray();
						assertEquals( HttpResponseStatus.OK.code(), responseGetTransfer.statusCode() );
						assertTrue(responseGetTransfer.headers().get("content-type").contains("application/json"));
						assertNotNull(jsonArray);
						
						LOGGER.info(jsonArray.encodePrettily());
						JsonObject accountTransferDetail = jsonArray.getJsonObject(0);
						int accountTransferID = accountTransferDetail.getInteger("ID");
						int fromAccountID = accountTransferDetail.getInteger("FROMACCOUNTID");
						int toAccountID = accountTransferDetail.getInteger("TOACCOUNTID");
						double transferAmount = accountTransferDetail.getDouble("AMOUNT");
						String transferCurrency = accountTransferDetail.getString("CURRENCY");
						
						assertEquals( 1, accountTransferID);
						assertEquals( 1, fromAccountID );
						assertEquals( 2,  toAccountID  );
						assertEquals( 1.0, transferAmount, 0.0 );
						assertEquals( "TRL", transferCurrency );
						
				        client.close();
						async.complete();
						  
					}));
		        });
		      })
		      .write(json)
		      .end();
		
		async.awaitSuccess();
	}
	
}