/*
 *  jETeL/Clover - Java based ETL application framework.
 *  Copyright (C) 2002  David Pavlis
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jetel.util;
import org.jetel.exception.NotFoundException;

import org.w3c.dom.NamedNodeMap;
/**
 *  Helper class (wrapper) around NamedNodeMap with possibility to parse string
 *  values into integers, booleans, doubles..<br>
 *  Used in conjunction with org.jetel.Component.*
 *
 * @author      dpavlis
 * @since       July 25, 2002
 * @revision    $Revision$
 * @created     26. March 2003
 */

public class ComponentXMLAttributes {

	private NamedNodeMap attributes;
	private org.w3c.dom.Node nodeXML;

	//private Map    childNodes;

	/**
	 *  Constructor for the ComponentXMLAttributes object
	 *
	 * @param  nodeXML  Description of the Parameter
	 */
	public ComponentXMLAttributes(org.w3c.dom.Node nodeXML) {
		attributes = nodeXML.getAttributes();
		this.nodeXML = nodeXML;
	}


	/**
	 *  Returns the String value of specified XML attribute
	 *
	 * @param  key  name of the attribute
	 * @return      The string value
	 */
	public String getString(String key) {
		try {
			return attributes.getNamedItem(key).getNodeValue();
		} catch (Exception ex) {
			throw new NotFoundException("Attribute " + key + " not found!");
		}
	}


	/**
	 *  Returns the String value of specified XML attribute
	 *
	 * @param  key           name of the attribute
	 * @param  defaultValue  default value to be returned when attribute can't be found
	 * @return               The string value
	 */
	public String getString(String key, String defaultValue) {
		try {
			return attributes.getNamedItem(key).getNodeValue();
		} catch (Exception ex) {
			return defaultValue;
		}
	}


	/**
	 *  Returns the int value of specified XML attribute
	 *
	 * @param  key  name of the attribute
	 * @return      The integer value
	 */
	public int getInteger(String key) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
		} catch (Exception ex) {
			throw new NotFoundException("Attribute " + key + " not found!");
		}
		return Integer.parseInt(value);
	}


	/**
	 *  Returns the int value of specified XML attribute
	 *
	 * @param  key           name of the attribute
	 * @param  defaultValue  default value to be returned when attribute can't be found
	 * @return               The integer value
	 */
	public int getInteger(String key, int defaultValue) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
			return Integer.parseInt(value);
		} catch (Exception ex) {
			return defaultValue;
		}
	}


	/**
	 *  Returns the boolean value of specified XML attribute
	 *
	 * @param  key  name of the attribute
	 * @return      The boolean value
	 */
	public boolean getBoolean(String key) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
		} catch (Exception ex) {
			throw new NotFoundException("Attribute " + key + " not found!");
		}
		return value.matches("^[tTyY].*");
	}


	/**
	 *  Returns the boolean value of specified XML attribute
	 *
	 * @param  key           name of the attribute
	 * @param  defaultValue  default value to be returned when attribute can't be found
	 * @return               The boolean value
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
			return value.matches("^[tTyY].*");
		} catch (Exception ex) {
			return defaultValue;
		}

	}


	/**
	 *  Returns the double value of specified XML attribute
	 *
	 * @param  key  name of the attribute
	 * @return      The double value
	 */
	public double getDouble(String key) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
		} catch (Exception ex) {
			throw new NotFoundException("Attribute " + key + " not found!");
		}
		return Double.parseDouble(value);
	}


	/**
	 *  Returns the double value of specified XML attribute
	 *
	 * @param  key           name of the attribute
	 * @param  defaultValue  default value to be returned when attribute can't be found
	 * @return               The double value
	 */
	public double getDouble(String key, double defaultValue) {
		String value;
		try {
			value = attributes.getNamedItem(key).getNodeValue();
			return Double.parseDouble(value);
		} catch (Exception ex) {
			return defaultValue;
		}
	}


	/**
	 *  Checks whether specified attribute exists (XML node has such attribute defined)
	 *
	 * @param  key  name of the attribute
	 * @return      true if exists, otherwise false
	 */
	public boolean exists(String key) {
		if (attributes.getNamedItem(key) != null) {
			return true;
		} else {
			return false;
		}
	}


	/**
	 *  Returns first TEXT_NODE child under specified XML Node
	 *
	 * @param  nodeXML  XML node from which to start searching
	 * @return          The TEXT_NODE value (String) if any exist or null
	 */
	public String getText(org.w3c.dom.Node nodeXML) {
		org.w3c.dom.Node childNode;
		org.w3c.dom.NodeList list;
		if (nodeXML.hasChildNodes()) {
			list = nodeXML.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				childNode = list.item(i);
				if (childNode.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
					return childNode.getNodeValue();
				}
			}
		}
		return null;
	}


	/**
	 *  Searches for specific child node name under specified XML Node
	 *
	 * @param  nodeXML        XML node from which to start searching
	 * @param  childNodeName  name of the child node to be searched for
	 * @return                childNode if exist under specified name or null
	 */
	public org.w3c.dom.Node getChildNode(org.w3c.dom.Node nodeXML, String childNodeName) {
		org.w3c.dom.Node childNode;
		org.w3c.dom.NodeList list;
		if (nodeXML.hasChildNodes()) {
			list = nodeXML.getChildNodes();
			for (int i = 0; i < list.getLength(); i++) {
				childNode = list.item(i);
				if (childNodeName.equals(childNode.getNodeName())) {
					return childNode;
				} else {
					childNode = getChildNode(childNode, childNodeName);
					if (childNode != null) {
						return childNode;
					}
				}

			}
		}
		return null;
	}

}
/*
 *  End class StringUtils
 */

