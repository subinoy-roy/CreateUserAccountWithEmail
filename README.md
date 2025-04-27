# Create User Account Using Email Handler

## Overview
This project is a Java-based AWS Lambda function that handles the creation of user accounts using email and username. It validates input, checks for existing users in Redis and MySQL, and creates a new user if valid.

## Features
- Input validation for username and email.
- Redis-based Bloom filter for efficient duplicate checks.
- MySQL integration for persistent user storage.
- Unit tests using JUnit and Mockito.

## Prerequisites
- Java 17 or higher
- Maven
- MySQL database
- Redis server
