package test.com.moneytransfer.vertices;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		vertx = Vertx.vertx();
		final DeploymentOptions options = new DeploymentOptions().setConfig(
				new JsonObject().put("http.port", PORT));

		// default config
		MoneyTransferVerticle moneyTransferVerticle = new MoneyTransferVerticle();
		vertx.deployVerticle(moneyTransferVerticle, options, context.asyncAssertSuccess());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testGetUserByUserID(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users/1", response -> response.bodyHandler(body -> {

			int ID = body.toJsonObject().getInteger("ID");
			String name = body.toJsonObject().getString("name");
			assertNotNull(body.toJsonObject());
			assertEquals( 1, ID);
			assertEquals( "user1", name );
			assertEquals( HttpResponseStatus.OK.code(), response.statusCode() );
			
			client.close();
			async.complete();
		}));
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetAccountsByUserId(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/accounts/1", response -> response.bodyHandler(body -> {

			JsonArray jsonArray = body.toJsonArray();
			assertEquals( HttpResponseStatus.OK.code(), response.statusCode() );
			assertNotNull(jsonArray);
			assertTrue(jsonArray.size() == 1);
			
			JsonObject account = jsonArray.getJsonObject(0);
			int accountID = account.getInteger("ID");
			int userID = account.getInteger("USERID");
			double balance = account.getDouble("BALANCE");
			
			assertEquals( 1, accountID);
			assertEquals( 1, userID );
			assertEquals( 100.0, balance, 0.0);
			
			client.close();
			async.complete();
		}));
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetUsers(TestContext context) throws Exception {
		HttpClient client = vertx.createHttpClient();
		Async async = context.async();
		client.getNow(PORT, "localhost", "/api/users", response -> response.bodyHandler(body -> {

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
	}

}