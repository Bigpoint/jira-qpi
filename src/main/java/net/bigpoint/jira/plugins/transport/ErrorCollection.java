package net.bigpoint.jira.plugins.transport;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.jcip.annotations.Immutable;


/**
 * Wrap element for the error validation erros and general error messages.
 * This class wraps the data and provide the JAXB elements, so the data is delivered as XML or JSON.
 * @author jschweizer
 *
 */
@Immutable
@XmlRootElement
public class ErrorCollection
{
    /**
     * Generic error messages
     */
    @XmlElement
    private Collection<String> errorMessages = new ArrayList<String>();

    /**
     * Errors specific to a certain field.
     */
    @XmlElement
    private Collection<ValidationError> errors = new ArrayList<ValidationError>();

    private ErrorCollection(){}
    
    public ErrorCollection(Collection<String> errMessages, Collection<ValidationError> valErrors){
    	this.errorMessages = errMessages;
    	this.errors = valErrors;
    }
}
