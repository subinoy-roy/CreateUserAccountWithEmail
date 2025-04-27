package org.roy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateUserAccountUsingEmailHandlerTest {

    private static final String MAIL_PROVIDER = "@jist.com";
    private static final String userName1;
    private static final String userEmail1;
    private static final String userName2;
    private static final String userEmail2;

    static {
        Faker faker = new Faker();
        userName1 = faker.name().name().replace(" ", ".");
        userEmail1 = userName1 + MAIL_PROVIDER;

        userName2 = faker.name().name().replace(" ", ".");
        userEmail2 = userName2 + MAIL_PROVIDER;
    }

    @Test
    void testHandleRequestPositive() {
        // Arrange
        CreateUserAccountUsingEmailHandler handler = new CreateUserAccountUsingEmailHandler();
        CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail request =
                new CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail(userName1,userEmail1);
        Context mockContext = mock(Context.class);
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        // Act
        CreateUserAccountUsingEmailHandler.ResponseCreateUserWithEmail response = handler.handleRequest(request, mockContext);

        // Assert
        assertNotNull(response);
        assertEquals(String.format("User %s with email %s created successfully",userName1, userEmail1), response.message());
        assertEquals(200, response.status());
    }

    @Test
    void testHandleRequestNegativeSameUser() {
        // Arrange
        CreateUserAccountUsingEmailHandler handler = new CreateUserAccountUsingEmailHandler();
        CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail request =
                new CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail(userName1,userEmail2);
        Context mockContext = mock(Context.class);
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        // Act
        CreateUserAccountUsingEmailHandler.ResponseCreateUserWithEmail response = handler.handleRequest(request, mockContext);

        // Assert
        assertNotNull(response);
        assertEquals(String.format("User with this username %s already exists",userName1), response.message());
        assertEquals(400, response.status());
    }

    @Test
    void testHandleRequestNegativeSameEmail() {
        // Arrange
        CreateUserAccountUsingEmailHandler handler = new CreateUserAccountUsingEmailHandler();
        CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail request =
                new CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail(userName2,userEmail1);
        Context mockContext = mock(Context.class);
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        // Act
        CreateUserAccountUsingEmailHandler.ResponseCreateUserWithEmail response = handler.handleRequest(request, mockContext);

        // Assert
        assertNotNull(response);
        assertEquals(String.format("User with this email %s already exists",userEmail1), response.message());
        assertEquals(400, response.status());
    }

}