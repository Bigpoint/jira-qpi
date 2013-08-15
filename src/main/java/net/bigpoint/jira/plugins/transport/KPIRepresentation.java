package net.bigpoint.jira.plugins.transport;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a KPI, containing information about the project and the KPI.
 * This class wraps the data and provide the JAXB elements, so the data is delivered as XML or JSON.
 * @author jschweizer
 *
 */
@XmlRootElement(name = "KPI")
public class KPIRepresentation {

	@XmlElement
	private String projectKey;

	@XmlElement
	private long projectId;
	
	@XmlElement(name="KpiNumber")
	private double kpiNumber;
	
	
	public KPIRepresentation(){}
	
	public KPIRepresentation(String projectKey, long id, double kpiNumber){
		this.projectKey = projectKey;
		this.kpiNumber = kpiNumber;
		this.projectId = id;
	}
	
	
	
}
