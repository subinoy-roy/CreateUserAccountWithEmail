package org.roy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.roy.utils.MySqlUtil;
import org.roy.utils.RedisUtil;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * This class handles the creation of user accounts using email and username.
 * It validates the input, checks for existing users in Redis, and creates a new user if valid.
 */
@SuppressWarnings("UnstableApiUsage")
public class CreateUserAccountUsingEmailHandler
        implements RequestHandler<CreateUserAccountUsingEmailHandler.RequestCreateUserWithEmail, CreateUserAccountUsingEmailHandler.ResponseCreateUserWithEmail> {

    private final RedisUtil redisUtil = new RedisUtil();
    private final MySqlUtil mySqlUtil = new MySqlUtil();

    public record RequestCreateUserWithEmail(String username, String email) {
    }

    public record ResponseCreateUserWithEmail(int status, String message) {
    }

    // Constants
    public static final int EXPECTED_INSERTIONS = 100000000; // Expected number of elements
    public static final double FPP = 0.01; // False positive probability
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_KEY_USERNAMES = "usernames";
    private static final String REDIS_KEY_EMAIL = "emails";
    private static final int SUCCESS = 200;
    private static final int FAILURE = 400;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 300;

    private static final String SQL_INSERT = "INSERT INTO users (username, email) VALUES (?, ?)";
    private static final String SQL_GET_USER = "SELECT * FROM users WHERE username = ? OR email = ?";

    @Override
    public ResponseCreateUserWithEmail handleRequest(RequestCreateUserWithEmail requestCreateUserWithEmail, Context context) {
        // Validations Start
        // 1. Basic Validations
        if (requestCreateUserWithEmail.email() == null || requestCreateUserWithEmail.email().isEmpty() ||
                requestCreateUserWithEmail.username() == null || requestCreateUserWithEmail.username().isEmpty()) {
            return new ResponseCreateUserWithEmail(400, "Invalid request");
        }
        // Add check for the username. It can not be longer than 30 characters.
        if (requestCreateUserWithEmail.username().length() > MAX_USERNAME_LENGTH) {
            return new ResponseCreateUserWithEmail(400, "Username should not be longer than 30 characters");
        }
        // Add check for the email. It should be maximum 300 characters long
        if (requestCreateUserWithEmail.email().length() > MAX_EMAIL_LENGTH) {
            return new ResponseCreateUserWithEmail(400, "Email should not be longer than 300 characters");
        }
        // 2. Validate with redis for username and email reuse
        // Connect to redis
        try (JedisPooled jedis = redisUtil.connectToRedis(REDIS_HOST, REDIS_PORT)) {
            Connection mySqlConn = mySqlUtil.connect("root", "mysql", context);
            // Username bloom filter
            byte[] bytesUsernames = jedis.get(REDIS_KEY_USERNAMES.getBytes());
            // Bloom filter to check for username
            // Initialize
            BloomFilter<CharSequence> bloomFilterUsername = initializeBloomFilter(bytesUsernames);

            // Check if the username already exist in the bloom filter
            if (bloomFilterUsername.mightContain(requestCreateUserWithEmail.username)) {
                return new ResponseCreateUserWithEmail(FAILURE,
                        String.format("User with this username %s already exists", requestCreateUserWithEmail.username));
            }

            // Email bloom filter
            byte[] bytesEmails = jedis.get(REDIS_KEY_EMAIL.getBytes());
            // Initialize
            BloomFilter<CharSequence> bloomFilterEmail = initializeBloomFilter(bytesEmails);

            // Check if the username and email already exist in the bloom filter
            if (bloomFilterEmail.mightContain(requestCreateUserWithEmail.email)) {
                // Check if the username and email already exist in the database
                if(checkIfUserExists(requestCreateUserWithEmail.username, requestCreateUserWithEmail.email, mySqlConn, context)) {
                    return new ResponseCreateUserWithEmail(FAILURE,
                            String.format("User with this email %s already exists", requestCreateUserWithEmail.email));
                }
            }

            // Log the request
            context.getLogger().log("Creating user with email: " + requestCreateUserWithEmail.email());
            context.getLogger().log("Creating user with username: " + requestCreateUserWithEmail.username());

            // Insert the user into the database
            insertUserIntoDatabase(requestCreateUserWithEmail.username, requestCreateUserWithEmail.email(), mySqlConn, context);

            // Add the username and email to the bloom filter
            bloomFilterUsername.put(requestCreateUserWithEmail.username);
            bloomFilterEmail.put(requestCreateUserWithEmail.email);

            // Save the bloom filter to redis
            if (redisUtil.writeToRedis(context, jedis, bloomFilterUsername, REDIS_KEY_USERNAMES))
                return new ResponseCreateUserWithEmail(FAILURE, "Error creating user");

            // Save the bloom filter to redis
            if (redisUtil.writeToRedis(context, jedis, bloomFilterEmail, REDIS_KEY_EMAIL))
                return new ResponseCreateUserWithEmail(FAILURE, "Error creating user");
        } catch (Exception e) {
            context.getLogger().log("Error connecting to DB: " + e.getMessage());
            return new ResponseCreateUserWithEmail(FAILURE, "Error creating user");
        }

        // Log the successful creation
        context.getLogger().log("User " + requestCreateUserWithEmail.username + "with email "
                + requestCreateUserWithEmail.email() + " created successfully");

        // Return a success response
        return new ResponseCreateUserWithEmail(SUCCESS, String.format("User %s with email %s created successfully",
                requestCreateUserWithEmail.username, requestCreateUserWithEmail.email()));
    }

    /**
     * Initializes the bloom filter from Redis or creates a new one if it doesn't exist.
     *
     * @param bytes The byte array from Redis
     * @return The bloom filter
     * @throws IOException If there is an error reading the bloom filter
     */
    private BloomFilter<CharSequence> initializeBloomFilter(byte[] bytes) throws IOException {
        if (bytes != null) {
            return BloomFilter.readFrom(
                    new java.io.ByteArrayInputStream(bytes),
                    Funnels.stringFunnel(java.nio.charset.StandardCharsets.UTF_8));
        } else {
            return BloomFilter.create(
                    Funnels.stringFunnel(java.nio.charset.StandardCharsets.UTF_8),
                    EXPECTED_INSERTIONS,
                    FPP
            );
        }
    }

    /**
     * Method to insert the user into the database.
     */
    private void insertUserIntoDatabase(String username, String email, Connection connection, Context context) {
        try (var preparedStatement = connection.prepareStatement(SQL_INSERT)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            context.getLogger().log("Error inserting user into database: " + e.getMessage());
        }
    }

    /**
     * Method to check if the username or email already exists in the database.
     */
    private boolean checkIfUserExists(String username, String email, Connection connection, Context context) {
        try (var preparedStatement = connection.prepareStatement(SQL_GET_USER)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, email);
            var resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            context.getLogger().log("Error checking if user exists: " + e.getMessage());
            return false;
        }
    }
}