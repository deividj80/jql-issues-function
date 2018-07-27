package com.deivid.jira.plugins;

import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.ServiceOutcome;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.plugin.jql.function.ClauseSanitisingJqlFunction;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;

import javax.annotation.Nonnull;
import java.util.*;

@Scanned
public class JqlIssuesFunction extends AbstractJqlFunction implements ClauseSanitisingJqlFunction {

    @JiraImport
    private final ProjectRoleManager projectRoleManager;
    @JiraImport
    private final ProjectService projectService;
    @JiraImport
    private final ProjectManager projectManager;
    @JiraImport
    private final PermissionManager permissionManager;
    @JiraImport
    private final JqlQueryParser jqlQueryParser;

    public JqlIssuesFunction( ProjectRoleManager projectRoleManager, ProjectService projectService,
                         PermissionManager permissionManager, ProjectManager projectManager, JqlQueryParser jqlQueryParser) {
        this.projectRoleManager = projectRoleManager;
        this.projectService = projectService;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.jqlQueryParser = jqlQueryParser;
    }

    @Nonnull
    @Override
    public MessageSet validate(final ApplicationUser searcher, @Nonnull final FunctionOperand operand, @Nonnull final TerminalClause terminalClause) {
        MessageSet messages = new MessageSetImpl();
        final List<String> arguments = operand.getArgs();

        //Make sure we have the correct number of arguments.
        if (arguments.isEmpty()) {
            messages.addErrorMessage(getI18n().getText("rolefunc.bad.num.arguments", operand.getName()));
            return messages;
        }


        Query q = null;
        try {
            q = jqlQueryParser.parseQuery(arguments.get(0));
        } catch (JqlParseException e) {
            e.printStackTrace();
        }
        if (q == null) {
                messages.addErrorMessage(getI18n().getText("rolefunc.validatequery", arguments.get(0)));
                return messages;
        }



//        //Make sure the role is valid.
//        final String requestedRole = arguments.get(0);
//        ProjectRole role = projectRoleManager.getProjectRole(requestedRole);
//        if (role == null) {
//            messages.addErrorMessage(getI18n().getText("rolefunc.role.not.exist", requestedRole));
//            return messages;
//        }

        //Make sure the project arguments are valid if provided.
//        if (arguments.size() > 1) {
//            for (String project : arguments.subList(1, arguments.size())) {
//                ProjectService.GetProjectResult result = getProjectResult(project);
//                if(!result.isValid()) {
//                    result.getErrorCollection().getErrorMessages().forEach(messages::addErrorMessage);
//                    return messages;
//                }
//            }
//        }

        return messages;
    }


    @Nonnull
    @Override
    public List<QueryLiteral> getValues(@Nonnull QueryCreationContext queryCreationContext, @Nonnull FunctionOperand functionOperand, @Nonnull TerminalClause terminalClause) {
        final List<String> arguments = functionOperand.getArgs();
        //Can't do anything when no argument is specified. This is an error so return empty list.
//        if (arguments.isEmpty()) {
//            return Collections.emptyList();
//        }

//       final ProjectRole projectRole = projectRoleManager.getProjectRole(arguments.get(0));
//        //Role not in system, then do nothing.
//        if (projectRole == null) {
//            return Collections.emptyList();
//        }

//        final Set<ApplicationUser> users = new HashSet<>();
//        //Projects are specified, then look at those projects.
//        if (arguments.size() > 1) {
//            for (String project : arguments.subList(1, arguments.size())) {
//                ProjectService.GetProjectResult result = getProjectResult(project);
//                users.addAll(projectRoleManager.getProjectRoleActors(projectRole, result.getProject()).getApplicationUsers());
//            }
//        } else {
//            ServiceOutcome<List<Project>> result = projectService.getAllProjects(queryCreationContext.getApplicationUser());
//            for (Project project: result.getReturnedValue()) {
//                users.addAll(projectRoleManager.getProjectRoleActors(projectRole, project).getApplicationUsers());
//            }
//        }

        //Convert all the users to query literals.
//        final List<QueryLiteral> literals = new ArrayList<>();
//        for (ApplicationUser user : users) {
//            literals.add(new QueryLiteral(functionOperand, user.getKey()));
//        }

        final List<QueryLiteral> literals = new ArrayList<>();


        try {

            String query = arguments.get(0);
            Query conditionQuery = jqlQueryParser.parseQuery(query);
            JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
            SearchService searchService = ComponentAccessor.getComponent(SearchService.class);


            SearchResults results = searchService.search(jiraAuthenticationContext.getLoggedInUser(), conditionQuery, PagerFilter.getUnlimitedFilter());

            Collection<Issue>  issues = results.getIssues();

            for(Issue issue : issues){
                literals.add(new QueryLiteral(functionOperand, issue.getId()));
            }

        } catch (JqlParseException e) {
            e.printStackTrace();
        } catch (SearchException e) {
            e.printStackTrace();
        }


        return literals;
    }

    private ProjectService.GetProjectResult getProjectResult(String project){
        ProjectService.GetProjectResult result = null;
        try {
            result = projectService.getProjectById(Long.parseLong(project));
        } catch (NumberFormatException e){
            result = projectService.getProjectByKey(project);
        }
        return result;
    }

    @Override
    public int getMinimumNumberOfExpectedArguments() {
        return 1;
    }

    @Nonnull
    @Override
    public JiraDataType getDataType() {
        return JiraDataTypes.USER;
    }

    public FunctionOperand sanitiseOperand(final ApplicationUser user, @Nonnull final FunctionOperand functionOperand) {
        final List<String> arguments = functionOperand.getArgs();

        //We only sanitise projects, so just return the original function if there are no projects.
        if (arguments.size() <= 1) {
            return functionOperand;
        }

        boolean argChanged = false;
        final List<String> newArgs = new ArrayList<>(arguments.size());
        newArgs.add(arguments.get(0));
        for (final String argument : arguments.subList(1, arguments.size())) {
            final Project project = projectManager.getProjectObjByKey(argument);
            if (project != null && !permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)) {
                newArgs.add(project.getId().toString());
                argChanged = true;
            } else {
                newArgs.add(argument);
            }
        }

        if (argChanged) {
            return new FunctionOperand(functionOperand.getName(), newArgs);
        } else {
            return functionOperand;
        }
    }
}
