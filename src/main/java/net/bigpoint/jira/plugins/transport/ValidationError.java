package net.bigpoint.jira.plugins.transport;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents a ValiationError, containing errors due to wrong params.
 * This class wraps the error data and provide the JAXB elements, so the data is delivered as XML or JSON.
 * @author jschweizer
 *
 */
@XmlRootElement
public class ValidationError
{
    // The field the error relates to
    @XmlElement
    private String field;
    // The Error key...
    @XmlElement
    private String error;

    @XmlElement
    private Collection<String> params;

    private ValidationError(){}
    
    public ValidationError(String field, String error){
    	this.field = field;
    	this.error = error;
    }
    
    public void addParams(Collection<String> params){
    	if(params == null ){
    		params = new ArrayList<String>();
    	}
    	for(String s : params){
    		params.add(s);
    	}
    	
    }


}
