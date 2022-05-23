package dev.damaso.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarketApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(MarketApplication.class, args);
		} catch (Exception ex) {
			if (!ex.getClass().toString().contains("SilentExitException")) {
				ex.printStackTrace();
				// Ensure status code is detected by whoever calls the application
				System.exit(1);
			}
		}
	}
}
