package net.bigpoint.jira.plugins.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.bigpoint.jira.plugins.data.KpiDataProvider;
import net.bigpoint.jira.plugins.transport.ErrorCollection;
import net.bigpoint.jira.plugins.transport.KPIRepresentation;
import net.bigpoint.jira.plugins.transport.KpiAtTimeRepresentation;
import net.bigpoint.jira.plugins.transport.KpiTimelineRepresentation;
import net.bigpoint.jira.plugins.transport.ValidationError;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

/**
 * Java Resource class providing a REST resource in JSON format This class is initialized by the rest module in
 * atlassian-plugin.xml.
 *
 * @author jschweizer
 *
 *
 */
@Path("/key-performance")
@AnonymousAllowed
@Produces({ MediaType.APPLICATION_JSON })
public class KeyPerformanceResource {

	/**
	 * Constant specifying the maximum period duration
	 */
	private final long PERIOD_MAXIMUM = 7300; //20 years

	/**
	 * Constant specifying maximum number of requested datasets.
	 */
	private final int MAXIMUM_NUMBER_DATASETS = 5000;
	//TODO: work in progress, evaluate witch caching, without caching.

	private final String STRING_ALL_PROJECTS = "allprojects";

	private final String STRING_ALL_CATEGORIES = "catallCategories";

	private final String CATEGORY_PREFIX = "cat";

	private final String STRING_TODAY = "today";

	private final String STRING_DAILY = "daily";

	private final String STRING_WEEKLY = "weekly";

	private final String STRING_MONTHLY = "monthly";

	private static final String PARAM_STRING_PROJECT_CATEGORY = "projectId";

	private static final String PARAM_STRING_PERIOD = "period";

	private static final String PARAM_STRING_INTERVAL = "interval";

	private static final String PARAM_STRING_END = "end";

	//24 * 60 * 60 * 1000L, 1 day in milliseconds
	private final long DAYS_TO_MILLISECONDS_MULTIPLIER = 86400000L;

	private ProjectManager m_projectManager;
	private IssueManager m_issueManager;
	private CustomFieldManager m_cfManager;

	private KpiDataProvider m_dataProvider;

	protected static final Logger LOGGER = Logger.getLogger(KeyPerformanceResource.class);

	public KeyPerformanceResource(ProjectManager proManager, IssueManager issueManager, CustomFieldManager cfm) {
		this.m_projectManager = proManager;
		this.m_issueManager = issueManager;
		this.m_cfManager = cfm;
		m_dataProvider = new KpiDataProvider(m_projectManager, m_issueManager, m_cfManager);
	}

	/**
	 * Rest path to validate method. Validates the incoming params and returns error messages for the visual gadget.
	 * @param projectIdString The project or category ids
	 * @param period the requested period.
	 * @param interval the requested step interval
	 * @param end string specifying the las requested date (default: today).
	 * @return HTTPResponse OK if  params are valid, a collection of error messages in every other case.
	 */
	@GET
	@Path("/validate")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response validate(
			@QueryParam(PARAM_STRING_PROJECT_CATEGORY) String projectIdString,
			@QueryParam(PARAM_STRING_PERIOD) String period,
			@QueryParam(PARAM_STRING_INTERVAL) String interval,
			@QueryParam(PARAM_STRING_END) String end) {

		Collection<ValidationError> errors = new ArrayList<ValidationError>();
		Collection<String> errMessages = new ArrayList<String>();

		if(projectIdString == null || projectIdString.equals("")) {
			errors.add(new ValidationError(PARAM_STRING_PROJECT_CATEGORY, "Please select al least one project or category"));
			ErrorCollection errCol = new ErrorCollection(errMessages, errors);
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(errCol).build();
		}

		try {
			if(Long.parseLong(period) > PERIOD_MAXIMUM) {
				errors.add(new ValidationError(PARAM_STRING_PERIOD, "Please do not specify a date more than 20 years ago"));
				ErrorCollection errCol = new ErrorCollection(errMessages, errors);
				return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(errCol).build();
			}
		} catch(Exception nfe) {
			errors.add(new ValidationError(PARAM_STRING_PERIOD, "Pleas specify the period in days"));
			ErrorCollection errCol = new ErrorCollection(errMessages, errors);
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(errCol).build();
		}
		Collection<Timestamp> stamps = getTimestampsFromParam(period, interval, STRING_TODAY);
		Collection<Project> projects = parseProjectParams(projectIdString);
		if(stamps.size() * projects.size() > MAXIMUM_NUMBER_DATASETS) {
			errors.add(new ValidationError(PARAM_STRING_INTERVAL,
					"You requested too many datasets, please reduce the period, interval or the number of projects"));
			ErrorCollection errCol = new ErrorCollection(errMessages, errors);
			return Response.status(HttpServletResponse.SC_BAD_REQUEST).entity(errCol).build();
		}

		return Response.ok().build();

	}

	/**
	 * GET resource, providing information about KPI numbers of given projects for a given time and interval steps
	 *
	 * @param projectIdString
	 *            The project or category ids
	 * @param period
	 *            the period given in days
	 * @param interval
	 *            the interval steps, i.e. daily, weekly
	 * @param end
	 *            the end day, default "today"
	 * @return a GET Response in JSON format
	 */
	@GET
	@AnonymousAllowed
	@Path("/getKpis")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getKpis(
			@QueryParam(PARAM_STRING_PROJECT_CATEGORY) String projectIdString,
			@QueryParam(PARAM_STRING_PERIOD) String period,
			@QueryParam(PARAM_STRING_INTERVAL) String interval,
			@QueryParam(PARAM_STRING_END) String end) {

		KeyPerformanceResource.LOGGER.info(new Date() + "New Request incoming");
		// prepare the return representations
		List<KpiAtTimeRepresentation> kpiCollection = new ArrayList<KpiAtTimeRepresentation>();

		Collection<Project> allProjects = parseProjectParams(projectIdString);

		Collection<Timestamp> stamps = getTimestampsFromParam(period, interval, end);
		// get the right data
		if(stamps != null && allProjects.isEmpty() == false) {
			for(Timestamp tsEnd : stamps) {
				String date = getLabelFromTimestamp(tsEnd, interval);
				List<KPIRepresentation> kpisAtTime = new ArrayList<KPIRepresentation>();
				for(Project p : allProjects) {
					long id = p.getId();
					kpisAtTime.add(new KPIRepresentation(p.getKey(), id, m_dataProvider.calculateKpiForProjectAtTime(id, tsEnd)));
				}
				kpiCollection.add(new KpiAtTimeRepresentation(date, kpisAtTime));
			}
		} else {
			kpiCollection = null;
			return Response.ok(null).build();
		}
		KeyPerformanceResource.LOGGER.info(new Date() + "Request done");
		// return a REST response
		return Response.ok(new KpiTimelineRepresentation(kpiCollection)).build();

	}

	private Collection<Project> parseProjectParams(String projectIdString) {

		if(projectIdString.equals(STRING_ALL_PROJECTS) || projectIdString.equals(STRING_ALL_CATEGORIES)) {
			return this.m_projectManager.getProjectObjects();
		}

		List<Project> allProjects = new ArrayList<Project>();
		// | is delimiter for REST request attributes
		String[] strings = projectIdString.split("[|]");

		try {
			for(int i = 0; i < strings.length; i++) {
				if(strings[i].startsWith(CATEGORY_PREFIX)) {
					String catString = strings[i].substring(CATEGORY_PREFIX.length(), strings[i].length());
					Long catId = Long.valueOf(catString);
					for(Project p : m_projectManager.getProjectObjectsFromProjectCategory(catId)) {
						allProjects.add(p);
					}
				} else {
					Long projectId = Long.valueOf(strings[i]);
					allProjects.add(m_projectManager.getProjectObj(projectId));
				}
			}
		} catch(NumberFormatException nfe) {
			LOGGER.warn("Wrong param: project id String: " + nfe.getMessage());
		}
		return allProjects;
	}

	/**
	 * Provides a normalized timestamp for each dataset request.
	 * Starting at start, it calculates a timestamp for each interval step in the given period.
	 * @param period the period of the request
	 * @param interval the step interval
	 * @param end the end of request timeline
	 * @return a collection of Timestamp, holding one timestamp for each dataset
	 */
	private Collection<Timestamp> getTimestampsFromParam(String period, String interval, String end) {

		Timestamp tsEnd = null;
		Long periodInMillis = -1l;
		if(end.equals(STRING_TODAY)) {
			tsEnd = new Timestamp(new Date().getTime());
		} else {
			return null;
		}

		// the parameter should be the number of days, so parse it...
		try {
			periodInMillis = Long.parseLong(period);
		} catch(Exception nfe) {
			return null;
		}
		// ... and convert it to milliseconds
		if(periodInMillis != null) {
			periodInMillis *= DAYS_TO_MILLISECONDS_MULTIPLIER;
		}
		// the begin of the period
		// for each interval step one Timestamp is added
		Timestamp start = new Timestamp(tsEnd.getTime() - periodInMillis);
		start = normalizeDate(start, interval);
		List<Timestamp> stamps = new ArrayList<Timestamp>();
		while(start.before(tsEnd)) {
			Timestamp curEnd = getNextDate(start, interval);
			stamps.add(curEnd);
			start = curEnd;
		}
		return stamps;

	}

	/**
	 * Normalizes a date, to make caching possible.
	 * @param ts the timestamp for normalization
	 * @param interval the current requested interval
	 * @return a normalized timestamp
	 */
	private Timestamp normalizeDate(Timestamp ts, String interval) {

		// pretty ugly method to normalize the time to 23:59:59:999 of a day
		// the advantage is the precalculation in the DataProvider
		Calendar c = Calendar.getInstance();
		c.setTime(ts);
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);

		//Normalize start date
		if(interval.equals(STRING_WEEKLY)) {
			c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		} else if(interval.equals(STRING_MONTHLY)) {
			c.set(Calendar.DAY_OF_MONTH, 0);
		}

		return new Timestamp(c.getTime().getTime());
	}

	/**
	 * Returns a timestamp for the next interval step.
	 * @param ts the current timestamp.
	 * @param interval the current interval
	 * @return the next timestamp
	 */
	private Timestamp getNextDate(Timestamp ts, String interval) {

		Calendar c = Calendar.getInstance();
		c.setTime(ts);
		long intervalInMillis = 0;

		if(interval.equals(STRING_DAILY)) {
			intervalInMillis = DAYS_TO_MILLISECONDS_MULTIPLIER;
			return new Timestamp(ts.getTime() + intervalInMillis);
		} else if(interval.equals(STRING_WEEKLY)) {
			intervalInMillis = 7 * DAYS_TO_MILLISECONDS_MULTIPLIER;
			return new Timestamp(ts.getTime() + intervalInMillis);
		} else if(interval.equals(STRING_MONTHLY)) {
			c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 1);
			return new Timestamp(c.getTimeInMillis());
		}
		return ts;
	}

	/**
	 * Return a string representation of the timestamp depending on the current interval.
	 * @param t the Timestamp
	 * @param interval the current interval
	 * @return A string representation for the timestamp.
	 */
	private String getLabelFromTimestamp(Timestamp t, String interval) {
		if(interval.equals(STRING_DAILY)
				||interval.equals(STRING_WEEKLY)
				||interval.equals(STRING_MONTHLY) ) {

			return t.toString().substring(0, "1970-01-01".length());
		}
		return "No date";
	}
}
