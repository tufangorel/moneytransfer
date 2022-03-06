package com.company.moneytransfer;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(VertxExtension.class)
public class JUnitTestSuite {

    private static final Logger logger = LoggerFactory.getLogger(JUnitTestSuite.class);

    @Test
    public void testSomething(VertxTestContext context) {
        context.completeNow();
    }
}