/*
 * jETeL/CloverETL - Java based ETL application framework.
 * Copyright (c) Javlin, a.s. (info@cloveretl.com)
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jetel.component.validator.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jetel.component.validator.GraphWrapper;
import org.jetel.component.validator.ReadynessErrorAcumulator;
import org.jetel.component.validator.ValidationErrorAccumulator;
import org.jetel.component.validator.params.BooleanValidationParamNode;
import org.jetel.component.validator.params.StringValidationParamNode;
import org.jetel.component.validator.params.ValidationParamNode;
import org.jetel.component.validator.utils.ValidatorUtils;
import org.jetel.data.DataRecord;
import org.jetel.metadata.DataRecordMetadata;
import org.jetel.util.string.StringUtils;

/**
 * @author drabekj (info@cloveretl.com) (c) Javlin, a.s. (www.cloveretl.com)
 * @created 4.12.2012
 */
@XmlRootElement(name="patternMatch")
@XmlType(propOrder={"ignoreCase", "pattern"})
public class PatternMatchValidationRule extends StringValidationRule {
	
	public static final int ERROR_INVALID_PATTERN = 601;
	public static final int ERROR_NO_MATCH = 602;
	
	@XmlElement(name="ignoreCase",required=true)
	private BooleanValidationParamNode ignoreCase = new BooleanValidationParamNode(false);
	@XmlElement(name="pattern",required=true)
	private StringValidationParamNode pattern = new StringValidationParamNode();
	
	public List<ValidationParamNode> initialize(DataRecordMetadata inMetadata, GraphWrapper graphWrapper) {
		ArrayList<ValidationParamNode> params = new ArrayList<ValidationParamNode>();
		pattern.setName("Pattern to match");
		pattern.setPlaceholder("Regular expression, for syntax see documentation");
		params.add(pattern);
		params.addAll(super.initialize(inMetadata, graphWrapper));
		ignoreCase.setName("Ignore case");
		params.add(ignoreCase);
		return params;
	}
	

	@Override
	public State isValid(DataRecord record, ValidationErrorAccumulator ea, GraphWrapper graphWrapper) {
		if(!isEnabled()) {
			logNotValidated("Rule not enabled.");
			return State.NOT_VALIDATED;
		}
		logParams(StringUtils.mapToString(getProcessedParams(record.getMetadata(), graphWrapper), "=", "\n"));
		
		String tempString = null;
		// FIXME: shouldn't be needed, remove?
		try {
			tempString = prepareInput(record, target.getValue());
		} catch (IllegalArgumentException ex) {
			logger.trace("Validation rule: " + getName() + " on '" + tempString + "' is " + State.INVALID + " (unknown field)");
			return State.INVALID;
		}
		Pattern pm;
		try {
			if(ignoreCase.getValue()) {
				pm = Pattern.compile(pattern.getValue(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			} else {
				pm = Pattern.compile(pattern.getValue(), Pattern.UNICODE_CASE);
			}
		} catch (PatternSyntaxException e) {
			logError("Pattern '" + pattern.getValue() + "' is invalid.");
			raiseError(ea, ERROR_INVALID_PATTERN, "The pattern is invalid.", target.getValue(), tempString);
			return State.INVALID;
		}
		if(pm.matcher(tempString).matches()) {
			logSuccess("Field '" + target.getValue() +  "' with value '" + tempString + "' has some match.");
			return State.VALID;
		} else {
			logError("Field '" + target.getValue() +  "' with value '" + tempString + "' has  no match.");
			raiseError(ea, ERROR_NO_MATCH, "No match.", target.getValue(), tempString);
			return State.INVALID;
		}
	}
	
	@Override
	public boolean isReady(DataRecordMetadata inputMetadata, ReadynessErrorAcumulator accumulator) {
		if(!isEnabled()) {
			return true;
		}
		boolean state = true;
		if(target.getValue().isEmpty()) {
			accumulator.addError(target, this, "Target is empty.");
			state = false;
		}
		if(pattern.getValue().isEmpty()) {
			accumulator.addError(pattern, this, "Match pattern is empty.");
			state = false;
		}
		if(!ValidatorUtils.isValidField(target.getValue(), inputMetadata)) { 
			accumulator.addError(target, this, "Target field is not present in input metadata.");
			state = false;
		}
		state &= super.isReady(inputMetadata, accumulator);
		return state;
	}

	/**
	 * @return the target
	 */
	public StringValidationParamNode getTarget() {
		return target;
	}


	/**
	 * @return the ignoreCase
	 */
	public BooleanValidationParamNode getIgnoreCase() {
		return ignoreCase;
	}


	/**
	 * @return the pattern
	 */
	public StringValidationParamNode getPattern() {
		return pattern;
	}
	
	@Override
	public TARGET_TYPE getTargetType() {
		return TARGET_TYPE.ONE_FIELD;
	}


	@Override
	public String getCommonName() {
		return "Pattern Match";
	}


	@Override
	public String getCommonDescription() {
		return "Checks whether chosen field matches regular expression provided by user.";
	}

}
