package com.company.moneytransfer;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class JUnitTestSuite {


    @Test
    public void testSomething(VertxTestContext context) {
        context.completeNow();
    }
}