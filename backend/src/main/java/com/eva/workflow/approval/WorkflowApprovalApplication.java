package com.eva.workflow.approval;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorkflowApprovalApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowApprovalApplication.class, args);
	}

}
