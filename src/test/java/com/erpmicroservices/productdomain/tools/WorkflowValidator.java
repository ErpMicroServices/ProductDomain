package com.erpmicroservices.productdomain.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Command-line tool for validating GitHub Actions workflow files.
 */
public class WorkflowValidator {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: WorkflowValidator <workflow-file> [workflow-file...]");
            System.exit(1);
        }
        
        GitHubActionsVersionChecker checker = new GitHubActionsVersionChecker();
        boolean hasErrors = false;
        
        for (String filename : args) {
            Path workflowFile = Paths.get(filename);
            System.out.println("Validating " + filename + "...");
            
            GitHubActionsVersionChecker.WorkflowValidationResult result = 
                checker.validateWorkflowFile(workflowFile);
            
            if (result.hasIssues()) {
                hasErrors = true;
                System.err.println("✗ Issues found in " + filename + ":");
                
                for (GitHubActionsVersionChecker.Issue issue : result.getIssues()) {
                    printIssue(issue);
                    
                    // Suggest fixes for deprecated versions
                    if (issue.getType() == GitHubActionsVersionChecker.IssueType.DEPRECATED_VERSION 
                        && issue.getSuggestedFix() != null) {
                        System.err.println("  → Suggested fix: " + issue.getAction() 
                            + " → " + issue.getAction().replaceAll("@[^@]+$", "@" + issue.getSuggestedFix()));
                        System.err.println("  → Fix command: " + 
                            checker.generateFixCommand(filename, issue.getAction(), 
                                issue.getAction().replaceAll("@[^@]+$", "@" + issue.getSuggestedFix())));
                    }
                }
                System.err.println();
            } else {
                System.out.println("✓ " + filename + " validation passed");
            }
        }
        
        if (hasErrors) {
            System.err.println("Workflow validation failed!");
            System.err.println("Fix the issues above before committing.");
            System.exit(1);
        } else {
            System.out.println("All workflow validations passed!");
        }
    }
    
    private static void printIssue(GitHubActionsVersionChecker.Issue issue) {
        String prefix = "  ";
        String location = issue.getLine() > 0 ? " (line " + issue.getLine() + ")" : "";
        
        switch (issue.getType()) {
            case DEPRECATED_VERSION:
                System.err.println(prefix + "[DEPRECATED]" + location + " " + issue.getMessage());
                break;
            case MISSING_VERSION:
                System.err.println(prefix + "[MISSING VERSION]" + location + " " + issue.getMessage());
                break;
            case INVALID_VERSION:
                System.err.println(prefix + "[INVALID VERSION]" + location + " " + issue.getMessage());
                break;
            case YAML_SYNTAX:
                System.err.println(prefix + "[YAML SYNTAX]" + location + " " + issue.getMessage());
                break;
            case MISSING_PROPERTY:
                System.err.println(prefix + "[MISSING PROPERTY]" + location + " " + issue.getMessage());
                break;
            default:
                System.err.println(prefix + "[ERROR]" + location + " " + issue.getMessage());
        }
    }
}