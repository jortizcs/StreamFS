/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 1.0
 */

package local.json.validator;

import net.sf.json.*;
import net.sf.json.processors.*;
import net.sf.json.util.*;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

public class JSONSchemaValidator{
	//Based on the schema defined at the ref below
	//	ref: http://json-schema.org/
	protected JSONObject masterSchema = null;

	public static final String TYPE = "type";
	public static final String ITEMS = "items";
	public static final String DESC = "description";
	public static final String ENUM = "enum";
	public static final String PROPS = "properties";
	public static final String ADDPROPS = "additionalProperties";
	public static final String DEFAULT = "default";
	public static final String OPTIONAL = "optional";
	public static final String SPEC = "specificity";
	public static final String UNIQUE = "unique";
	public static final String MIN = "minimum";
	public static final String MAX = "maximum";

	Vector<String> errorVec = new Vector<String>();

	public static transient Logger logger = Logger.getLogger(JSONSchemaValidator.class.getPackage().getName());

	public JSONSchemaValidator(){

		try {
			URL masterSchemaURL = new URL("http://www.eecs.berkeley.edu/~jortiz/schemas/schema.json");

			BufferedReader in = new BufferedReader(new InputStreamReader(masterSchemaURL.openStream()));
			String inputLine=null;
			StringBuffer schemaBufDoc = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				schemaBufDoc = schemaBufDoc.append(inputLine).append("\n");
			}

			//System.out.println(schemaBufDoc);
			if(JsonVerifier.isValidJsonValue(schemaBufDoc.toString())) {
				masterSchema = (JSONObject) JSONSerializer.toJSON(schemaBufDoc.toString());
			}
			else {
				System.out.println ("Could not parse schema schema");
			}


			in.close();
		} catch (Exception e){
			if(e instanceof JSONException)
				logger.warning("Could not parse schema schema; JSONSerializer Error");
			logger.log(Level.WARNING, "", e);
		}
	}

	public static void main(String[] args) {
		System.out.println("Testing Json Schema Validator");
		JSONSchemaValidator validator = new JSONSchemaValidator();
		//JSONObject schemaTest = validator.fetchJSONObj("http://192.168.1.105/schemas/object_stream_schema.json");
		JSONObject schemaTest = validator.fetchJSONObj("http://192.168.1.105/schemas/protocols/join_schema.json");
		validator.validate(null, schemaTest);
	}

	public static JSONObject fetchJSONObj(String docUrl){
		try {
			URL masterSchemaURL = new URL(docUrl);
			BufferedReader in = new BufferedReader(new InputStreamReader(masterSchemaURL.openStream()));
			String inputLine=null;
			StringBuffer schemaBufDoc = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				schemaBufDoc = schemaBufDoc.append(inputLine).append("\n");
			}

			in.close();
			if(JsonVerifier.isValidJsonValue(schemaBufDoc.toString())) {
				return (JSONObject) JSONSerializer.toJSON(schemaBufDoc.toString());
			}
			else {
				logger.warning("Could not parse schema schema");
				return null;
			}

		} catch (Exception e){
			if(e instanceof JSONException)
				logger.warning("Could not parse schema schema; JSONSerializer Error");
			logger.log(Level.WARNING, "", e);
			return null;
		}
	}

	public boolean validate(JSONObject jsonObj, JSONObject schemaObj) {
		if (!validateSchema(schemaObj) || !validateObj(jsonObj, schemaObj)) { 
			for (int i=0; i<errorVec.size(); i++){
				System.out.println(errorVec.elementAt(i));
			}
			return false;
		}
		return true;


	}

	private boolean validateObj(JSONObject jsonObj, JSONObject schemaObj){
		if(jsonObj !=null){
			JSONObject schemaPropsObj = schemaObj.getJSONObject("properties");
			//1.  check that all the properties are in the schema
			LinkedHashSet attributes = new LinkedHashSet(jsonObj.keySet());
			Iterator attrIterator = attributes.iterator();
			boolean allAttrKnown = true;
			while (attrIterator.hasNext() ){

				Object attrNameObj = attrIterator.next();
				if(JSONUtils.isString(attrNameObj)) {
					String attrName = (String) attrNameObj;
					allAttrKnown &= schemaPropsObj.has(attrName);
					if(!allAttrKnown) {
						addToErrorLog("Unknown attribute: " + attrName);
						return false;
					}
				}else{
					addToErrorLog("All keys must be strings");
					return false;
				}
			}
			
			//2.  check if any mandatory properties are missing
			//3.  check that all the properties are within specification
		}

		return true;
	}

	private boolean validateSchema(JSONObject schemaObj){
		LinkedHashSet keys = new LinkedHashSet(schemaObj.keySet());
		Iterator keysIterator = keys.iterator();
		//List values = schemaObj.values();

		//this schema must be of type object or array
		if(!schemaObj.has(TYPE) || (!schemaObj.getString(TYPE).equals("object") &&
			!schemaObj.getString(TYPE).equals("array"))){
			return false;
			}

		boolean anyErrors = false;
		while (keysIterator.hasNext()){
			anyErrors = anyErrors | checkProp(keysIterator.next(), schemaObj);
		}

		//print errors
		if(anyErrors) {
			for(int i=0; i<errorVec.size(); i++){
				System.out.println(i+1 + ".  " + (String)errorVec.elementAt(i));
			}
		}

		return true;
	}

	private boolean checkProp(Object thisKey, JSONObject schemaObj) {

		//Check that each key is a valid key type defined in the schema schema
		JSONObject propsObj = masterSchema.getJSONObject(PROPS);
		String keyStr = (String) thisKey;
		if(!propsObj.has(keyStr)) {
			String error = "Invalid key: " + keyStr;
			errorVec.addElement(error);
		}

		//Check that key value adhere's to schema schema spec
		Object propValue = propsObj.get(thisKey);
		if(JSONUtils.isObject(propValue)) {
			//System.out.println("object");
			JSONObject typeDescObj = propsObj.getJSONObject(keyStr);
			boolean proppropsOk = checkPropProps (thisKey, schemaObj, typeDescObj);
			/*if(proppropsOk)
				System.out.println("propprops ok");
			else
				System.out.println("propprops NOT ok");*/
			return proppropsOk;

		}else if (JSONUtils.isArray(propValue)) {
			JSONArray typeDescJArray = propsObj.getJSONArray(keyStr);
			//System.out.println("array");
		}else if (JSONUtils.isString(propValue)) {
			String typeDescStr = (String) propValue;
			//System.out.println("string");
		}

		return true;

	}

	private boolean checkPropProps(Object thisKey, JSONObject schemaObj, JSONObject msPropValObj) {
		try {
			Object msPropType = msPropValObj.get(TYPE);
			if(JSONUtils.isString(msPropType)){
				//System.out.println("Type STRING");
				if(msPropType.equals("string") && JSONUtils.isString(schemaObj.get(thisKey))){
					return true;
				}else if(msPropType.equals("boolean") && JSONUtils.isBoolean(schemaObj.get(thisKey))){
					return true;
				} else if(msPropType.equals("number") && JSONUtils.isNumber(schemaObj.get(thisKey))){
					return true;
				} else if(msPropType.equals("integer") ){
					int thisInt = Integer.parseInt((String) schemaObj.get(thisKey));
					return true;
				} else if(msPropType.equals("object") && JSONUtils.isObject(schemaObj.get(thisKey))){
					return true;
				} else if (msPropType.equals("array") && JSONUtils.isArray(schemaObj.get(thisKey))){
					return checkArrayProps(thisKey, schemaObj, msPropValObj);
				} else if (msPropType.equals("null") && JSONUtils.isNull(schemaObj.get(thisKey))){
					return true;
				} else if(msPropType.equals("any")){
					return true;
				}

				//add to error vector
				addToErrorLog((String) thisKey +" must be type " + (String) msPropType);
				return false;
			}
			else if (JSONUtils.isArray(msPropType)){
				//System.out.println("Type ARRAY");
				//JSONArray msTypeArray = msPropValObj.getJSONArray(TYPE);
				JSONArray msPropTypeArray = (JSONArray) msPropType;
				for(int i=0; i<msPropTypeArray.size(); i++){
					Object thisPropType = msPropTypeArray.get(i);
					if(JSONUtils.isString(thisPropType)){
						if(thisPropType.equals("string") && 
								JSONUtils.isString(schemaObj.get(thisKey))){
							return true;
						}else if(thisPropType.equals("boolean") && 
								JSONUtils.isBoolean(schemaObj.get(thisKey))){
							return true;
						} else if(thisPropType.equals("number") && 
								JSONUtils.isNumber(schemaObj.get(thisKey))){
							return true;
						} else if(thisPropType.equals("integer") ){
							int thisInt = Integer.parseInt((String) schemaObj.get(thisKey));
							return true;
						} else if(thisPropType.equals("object") && 
								JSONUtils.isObject(schemaObj.get(thisKey))){
							return true;
						} else if (thisPropType.equals("array") && 
								JSONUtils.isArray(schemaObj.get(thisKey))){
							return checkArrayProps(thisKey, schemaObj, msPropValObj);
						} else if (thisPropType.equals("null") && 
								JSONUtils.isNull(schemaObj.get(thisKey))){
							return true;
						} else if(thisPropType.equals("any")){
							return true;
						}
					} else {
						System.out.println("Master Schema Type Array element not a string");
					}
				}
				//add to error vector
				String errorStr = "";
				for(int j=0; j<msPropTypeArray.size(); j++){
					errorStr = errorStr + 
							(String) thisKey +" must be one of these types " + "["
							+ (String) msPropTypeArray.get(j) + ", ";
				}
				errorStr = errorStr.substring(0, errorStr.length()-2) + "]";
				return false;
			}else {
				System.out.println("Type OTHER");
			}

			return true;
		} catch(NumberFormatException e){
			//add to error vector
			String thisError = (String) thisKey +" must be type integer";
			addToErrorLog(thisError);
			return false;
		}
	}

	private void addToErrorLog(String error) {
		errorVec.addElement(error);
	}

	private boolean checkArrayProps(Object key, JSONObject schemaObj, JSONObject propertyValObj){
		if(propertyValObj.has("properties")) {
			return true;	
		} else {
			return true;
		}
	}

}
