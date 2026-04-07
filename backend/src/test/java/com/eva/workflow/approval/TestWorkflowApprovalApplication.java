package com.eva.workflow.approval;

import org.springframework.boot.SpringApplication;

public class TestWorkflowApprovalApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkflowApprovalApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
