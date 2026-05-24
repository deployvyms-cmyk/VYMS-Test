package com.vyms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point of the Vehicle Yard Management System.
 *
 * Why this class is important:
 * - {@code @SpringBootApplication} tells Spring Boot to auto-configure the app,
 *   scan components, and start the embedded server.
 * - The {@code main} method starts the full Spring context.
 */
@SpringBootApplication
public class VymsApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * Under the hood, Spring creates all configured beans (controllers,
	 * services, repositories), then starts Tomcat and serves web requests.
	 */
	public static void main(String[] args) {
		SpringApplication.run(VymsApplication.class, args);
	}

}
