package net.bigpoint.jira.plugins.transport;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the KPI numbers for a given set of projects at one specific date.
 * This class wraps the data and provide the JAXB elements, so the data is delivered as XML or JSON.
 * @author jschweizer
 *
 */
@XmlRootElement
public class KpiAtTimeRepresentation {
	
	@XmlElement(name="Time")
	private String date;
	
	@XmlElement(name="ProjectKPI")
	private Collection<KPIRepresentation> projectsKpis;
	
	private KpiAtTimeRepresentation(){}
	
	public KpiAtTimeRepresentation(String date, Collection<KPIRepresentation> kpis){
		this.date = date;
		this.projectsKpis = kpis;
	}
	
	
}
