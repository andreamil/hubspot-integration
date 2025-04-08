package com.meetime.hubspot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HubspotIntegrationApplication {

	public static void main(String[] args) {
		SpringApplication.run(HubspotIntegrationApplication.class, args);
	}

}
