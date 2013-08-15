package net.bigpoint.jira.plugins.data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.ProjectManager;

/**
 * Internal DataProvider for the Key performance indicator plugin.
 *
 * @author jschweizer
 *
 */
public class KpiDataProvider {

	/**
	 * The name of the custom filed, used by the plugin.
	 */
	private final String CUSTOM_FIELD_SEVERITY = "Severity";

	/**
	 * status description of the issue status: closed.
	 */
	private final String STRING_ISSUE_CLOSED = "Closed";

	protected static final Logger LOGGER = Logger.getLogger(KpiDataProvider.class);

	// multiplicator determined by PO fregel
	private final double MULTIPLICATOR_TYPE_ONE = 0.5; // 1 * 0.5
	private final double MULTIPLICATOR_TYPE_TWO = 1.2; // 2 * 0.6
	private final double MULTIPLICATOR_TYPE_THREE = 3; // 3 * 1
	private final double MULTIPLICATOR_TYPE_FOUR = 6; // 4 * 1.5
	private final double MULTIPLICATOR_TYPE_FIVE = 9; // 5*1.8

	private IssueManager issueManager;
	private CustomFieldManager cfManager;

	private KpiCacheDBMapper m_cacheDB;

	public KpiDataProvider(ProjectManager pm, IssueManager im, CustomFieldManager cfManager) {
		this.issueManager = im;
		this.cfManager = cfManager;

		this.m_cacheDB = new KpiCacheDBMapper();

	}

	/**
	 * Calculates or returns the cached kpi value for one project at one specific time.
	 *
	 * @param id
	 *            the project id
	 * @param end
	 *            the timestamp, the value is requested for
	 * @return the kpi value of the project
	 */
	public double calculateKpiForProjectAtTime(long id, Timestamp end) {
		double cachedVlaue = this.m_cacheDB.getCachedValue(id, end);
		if(cachedVlaue == -1) {
			KpiDataProvider.LOGGER.info("No cached value: Calculating and caching for project: " + id + " at " + end);
			double kpiNumber = -1;
			kpiNumber = this.calculateKpi(getUnclosedIssuesFromManager(id, end));
			this.m_cacheDB.cacheValue(id, end, kpiNumber);
			return kpiNumber;
		} else {
			return cachedVlaue;
		}
	}

	/**
	 * Returns all issues, not closed. These influence the KPI.
	 *
	 * @param id
	 *            the project id
	 * @param tsEnd
	 *            the timestamp of the last date considered
	 * @return a list containing all MutableIssues objects, which are not closed
	 */
	private List<Long> getUnclosedIssuesFromManager(long id, Timestamp tsEnd) {

		try {
			Collection<Long> issueIds = issueManager.getIssueIdsForProject(id);
			List<Long> unclosedAtTime = new ArrayList<Long>();
			for(Long longId : issueIds) {
				MutableIssue issue = issueManager.getIssueObject(longId);

				if(issue.getStatusObject().getName().equals(STRING_ISSUE_CLOSED) == false && issue.getCreated().before(tsEnd)) {
					unclosedAtTime.add(issue.getId());
				} else {
					if(issue.getResolutionDate() != null && issue.getCreated().before(tsEnd) && issue.getResolutionDate().after(tsEnd)) {
						unclosedAtTime.add(issue.getId());
					}
				}
			}
			return unclosedAtTime;

		} catch(GenericEntityException gee) {
			KpiDataProvider.LOGGER.error("Exception while searching issues form Manager " + gee.getMessage());
			return null;
		}
	}

	/**
	 * Calculating the KPI for a given set of issues
	 *
	 * @param unclosedIssues
	 *            the list of issues
	 * @return the kpi based on predefined mulitplicator
	 */
	private double calculateKpi(Collection<Long> unclosedIssues) {
		double kpi = 0.0;
		if(unclosedIssues != null && unclosedIssues.size() > 0) {
			for(long l : unclosedIssues) {

				double multiplicator = parseMultiplicatorForIssueSeverity(l);
				if(multiplicator != -1) {
					kpi += multiplicator;
				}
			}
		}
		return kpi;
	}

	/**
	 * Returns KPI values for one specific issue and its corresponding severity.
	 *
	 * @param issueId
	 *            the id of the issue
	 * @return a double value representing the multiplicator for this issue, -1 if none can be found
	 */
	private double parseMultiplicatorForIssueSeverity(long issueId) {

		MutableIssue issue = issueManager.getIssueObject(issueId);
		List<CustomField> customFields = cfManager.getCustomFieldObjects(issue);
		// TODO use cfManager.getCFObj(String)
		for(CustomField cf : customFields) {
			if(cf.getName().equals(CUSTOM_FIELD_SEVERITY)) {
				String value = cf.getValueFromIssue(issue);
				if(value != null) {

					String[] strings = value.split("-");
					int valueNumber = 0;

					try {

						valueNumber = Integer.parseInt(strings[0].trim());

					} catch(NumberFormatException e) {
						KpiDataProvider.LOGGER.error("Exception while parsing severity: " + e.getMessage());
						return -1;
					}

					// return predefined multiplicators
					switch(valueNumber) {
						case 1:
							return MULTIPLICATOR_TYPE_ONE;
						case 2:
							return MULTIPLICATOR_TYPE_TWO;
						case 3:
							return MULTIPLICATOR_TYPE_THREE;
						case 4:
							return MULTIPLICATOR_TYPE_FOUR;
						case 5:
							return MULTIPLICATOR_TYPE_FIVE;
						default:
							return 0;
					}

				} else {
					// no String value found for this issue and the custom field Severity
					return -1;
				}
			}
		}
		// no custom field "Severity" found
		return -1;
	}
}
