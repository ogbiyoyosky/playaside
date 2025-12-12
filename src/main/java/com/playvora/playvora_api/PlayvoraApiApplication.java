package com.playvora.playvora_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class PlayvoraApiApplication {

	public static void main(String[] args) {
		// Load .env file before Spring Boot starts
		loadEnvFile();
		
		SpringApplication.run(PlayvoraApiApplication.class, args);
	}

	private static void loadEnvFile() {
		Path envPath = Paths.get(".env");
		
		if (!Files.exists(envPath)) {
			System.out.println("No .env file found, using system environment variables");
			return;
		}
		
		try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				// Skip empty lines and comments
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				
				// Parse KEY=value format
				int equalIndex = line.indexOf('=');
				if (equalIndex > 0) {
					String key = line.substring(0, equalIndex).trim();
					String value = line.substring(equalIndex + 1).trim();
					
					// Remove quotes if present
					if (value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length() - 1);
					} else if (value.startsWith("'") && value.endsWith("'")) {
						value = value.substring(1, value.length() - 1);
					}
					
					// Set as system property (so Spring Boot can use it)
					System.setProperty(key, value);
				}
			}
			System.out.println("Successfully loaded .env file");
		} catch (IOException e) {
			System.err.println("Failed to load .env file: " + e.getMessage());
		}
	}

}
