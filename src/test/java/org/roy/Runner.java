package org.roy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import static org.mockito.Mockito.*;

public class Runner {
    public static void main(String[] args) {
        Context mockContext = mock(Context.class);
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);

        // Run the CreateUserAccountUsingEmailHandler
        CreateUserAccountUsingEmailHandler handler = new CreateUserAccountUsingEmailHandler();
        CreateUserAccountUsingEmailHandler.ResponseCreateUserWithEmail responseCreateUserWithEmail = handler.handleRequest(
            new CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail("test2","test2@jist.com"),
            mockContext
        );

        // Print the response
        System.out.println("Response: " + responseCreateUserWithEmail.message() + " with status: " + responseCreateUserWithEmail.status());
    }
}