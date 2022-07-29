package dev.damaso.market;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class MarketApplication {

	public static void main(String[] args) {
		// Command line arguments disable server
		WebApplicationType webApplicationType;
		if (args.length==0) {
			webApplicationType = WebApplicationType.SERVLET;
		} else {
			webApplicationType = WebApplicationType.NONE;
		}

		try {
			new SpringApplicationBuilder(MarketApplication.class)
				.web(webApplicationType)
				.run(args);
			// SpringApplication.run(MarketApplication.class, args);
		} catch (Throwable ex) {
			if (!ex.getClass().toString().contains("SilentExitException")) {
				ex.printStackTrace();
				// Ensure status code is detected by whoever calls the application
				System.exit(1);
			}
		}
	}
}
