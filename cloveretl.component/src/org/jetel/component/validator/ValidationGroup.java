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
package org.jetel.component.validator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jetel.data.DataRecord;
import org.jetel.metadata.DataRecordMetadata;

/**
 * @author drabekj (info@cloveretl.com) (c) Javlin, a.s. (www.cloveretl.com)
 * @created 19.11.2012
 */
@XmlRootElement(name="group")
@XmlAccessorType(XmlAccessType.NONE)
public class ValidationGroup extends ValidationNode {
	
	@XmlElementWrapper(name="children")
	@XmlElementRef
	private List<ValidationNode> childs = new ArrayList<ValidationNode>();
	@XmlAttribute(required=true)
	private Conjunction conjunction = Conjunction.AND;
	
	/**
	 * Wrapper class for prelimitary condition. Needed for nesting in schema generated by JAXB.
	 * Other considered:
	 *  - Cannot be wrapped as its not collection.
	 *  - If it's collection than it can contain more prelimitary conditions (not wanted)
	 *  - Cannot be left out as it couldn't be ommited @XmlElementRef(required=false) requires JAXB 2.1+ (not enabled in Java 6 by default) 
	 */
	@XmlAccessorType(XmlAccessType.NONE)
	private static class PrelimitaryCondition {
		@XmlElementRef
		private AbstractValidationRule content;
		public AbstractValidationRule getContent() {return content;}
		public void setContent(AbstractValidationRule value) {content = value;}
	}
 
	@XmlElement(name="prelimitaryCondition")
	private PrelimitaryCondition prelimitaryCondition;
	@XmlAttribute
	private boolean laziness = true;
	
	@XmlType(name = "conjunction")
	@XmlEnum
	public enum Conjunction {
		AND, OR;
		
		public static State and(State left, State right) {
			if(left == State.INVALID || right == State.INVALID) {
				return State.INVALID;
			}
			if(left == State.VALID || right == State.VALID) {
				return State.VALID;
			}
			return State.NOT_VALIDATED;
		}
		
		public static State or(State left, State right) {
			if(left == State.VALID || right == State.VALID) {
				return State.VALID;
			}
			if(left == State.NOT_VALIDATED && right == State.NOT_VALIDATED) {
				return State.NOT_VALIDATED;
			}
			return State.INVALID;
		}
	}

	/**
	 * Sets conjunction
	 * @param conjunction Conjunction to be used by group, not null
	 */
	public void setConjunction(Conjunction conjunction) {
		if(conjunction != null) {
			this.conjunction = conjunction;
		}
	}
	
	/**
	 * @return Returns condition
	 */
	public Conjunction getConjunction() {
		return conjunction;
	}

	/**
	 * Sets new prelimitary condition overwriting the previous.
	 * @param prelimitaryCondition Group entrance condition
	 */
	public void setPrelimitaryCondition(AbstractValidationRule prelimitaryCondition) {
		if(prelimitaryCondition == null) {
			this.prelimitaryCondition = null;
			return;
		}
		if (this.prelimitaryCondition == null) {
			this.prelimitaryCondition = new PrelimitaryCondition();
		}
		this.prelimitaryCondition.setContent(prelimitaryCondition);
	}
	
	public AbstractValidationRule getPrelimitaryCondition() {
		if(prelimitaryCondition == null) {
			return null;
		}
		return prelimitaryCondition.getContent();
	}

	/**
	 * Sets whether group will be evaluated lazy
	 * @param laziness True if lazy evaluation is wanted, false otherwise
	 */
	public void setLaziness(boolean laziness) {
		this.laziness = laziness;
	}
	
	/**
	 * True if group should be evaluated lazy, false otherwise
	 * @return
	 */
	public boolean getLaziness() {
		return laziness;
	}

	/**
	 * Adds new child after the last child
	 * @param child Validation node to be added into group
	 */
	public void addChild(ValidationNode child) {
		childs.add(child);
	}
	
	/**
	 * Returns all children of group
	 * @return All children
	 */
	public List<ValidationNode> getChildren() {
		return childs;
	}

	@Override
	public State isValid(DataRecord record, ValidationErrorAccumulator ea, GraphWrapper graphWrapper) {
		if(!isEnabled()) {
			logNotValidated("Group not enabled.");
			return State.NOT_VALIDATED;
		}
		AbstractValidationRule prelimitaryCondition = getPrelimitaryCondition();
		logParams("Conjunction: " + conjunction + "\n" +
						"Lazy: " + laziness + "\n" +
						"Prelimitary condition: " + ((prelimitaryCondition == null)? null: prelimitaryCondition.getName()));
		
		if(prelimitaryCondition != null) { 
			if(prelimitaryCondition.isValid(record, null, graphWrapper) == State.INVALID) {
				logNotValidated("Prelimitary condition of group was invalid.");
				return State.NOT_VALIDATED;
			}
		}
		State currentState = State.NOT_VALIDATED;
		State childState;
		for(int i = 0; i < childs.size(); i++) {
			childState = childs.get(i).isValid(record,ea, graphWrapper);
			if(conjunction == Conjunction.AND) {
				currentState = Conjunction.and(currentState, childState);
				if(laziness && currentState == State.INVALID) {
					break;
				}
			}
			if(conjunction == Conjunction.OR) {
				currentState = Conjunction.or(currentState, childState);
				if(laziness && currentState == State.VALID) {
					break;
				}
			}
		}
		if(currentState == State.INVALID) {
			logError("");
			return State.INVALID;
		}
		if(currentState == State.VALID) {
			logSuccess("");
			return State.VALID;
		}
		logNotValidated("Group has no children.");
		return State.NOT_VALIDATED;
	}
	
	@Override
	public boolean isReady(DataRecordMetadata inputMetadata, ReadynessErrorAcumulator accumulator) {
		if(!isEnabled()) {
			return true;
		}
		boolean state = true;
		for(int i = 0; i < childs.size(); i++) {
			state &= childs.get(i).isReady(inputMetadata, accumulator);
		}
		return state;
	}

	@Override
	public String getCommonName() {
		return "Group";
	}

	@Override
	public String getCommonDescription() {
		return "Groups allow creating of complex rules created by joining multiple rules with AND/OR conjunction.";
	}

}
