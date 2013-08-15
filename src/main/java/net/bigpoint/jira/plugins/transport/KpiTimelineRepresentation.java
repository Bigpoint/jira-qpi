package net.bigpoint.jira.plugins.transport;

import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.jcip.annotations.Immutable;

/**
 * Representation of a whole KPI Timeline containing several KpisAtTime.
 * This class wraps the data and provide the JAXB elements, so the data is delivered as XML or JSON.
 * @author jschweizer
 *
 */
@Immutable
@XmlRootElement(name="KpiCollection")
public class KpiTimelineRepresentation {

	@XmlElement(name="KpisAtTime")
	private Collection<KpiAtTimeRepresentation> kpisAtTime;
	
	private KpiTimelineRepresentation(){}
	
	public KpiTimelineRepresentation(Collection<KpiAtTimeRepresentation> kpis){
		this.kpisAtTime = kpis;
	}
	
	
	
	
}
