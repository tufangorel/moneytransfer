package com.moneytransfer;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moneytransfer.verticles.MoneyTransferVerticle;

import io.vertx.core.Vertx;

// Used to run as a standalone java application.
public class AppMain {

	private static final Logger logger = LoggerFactory.getLogger(AppMain.class);

    public static void main(String... args) throws Exception {
    	
        Vertx vertx = Vertx.vertx(); 
        Consumer runner = clazz -> {
        	
            vertx.deployVerticle(((Class) clazz).getName(), stringAsyncResult -> {
                if (stringAsyncResult.succeeded()) {
                    logger.info("Succesfully deployed : " + ((Class) clazz).getSimpleName());
                } else {
                    logger.error("Failed to deploy : " + stringAsyncResult.cause());
                }
            });
            
        };

        Class[] clazzes = {MoneyTransferVerticle.class};
        Stream.of(clazzes).forEach(runner);
    } 

}