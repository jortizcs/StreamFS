package local.json.javascript;

import java.util.Iterator;

import net.sf.json.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;

/**
 * Collection of JSON Utility methods.
 * 
 */

public class Js2JSONUtils
{
    /**
     * Converts a given JavaScript native object and converts it to the relevant JSON string.
     * 
     * @param object            JavaScript object
     * @return String           JSON      
     */
    public String toJSONString(Object object)
    {

        if (object instanceof NativeArray)
        {
            return nativeArrayToJSONString((NativeArray)object);
        }
        else if (object instanceof NativeObject)
        { 
           return nativeObjectToJSONString((NativeObject)object);
        }
	return null;
        
    }
    
    /**
     * Takes a JSON string and converts it to a native java script object
     * 
     * @param  jsonString       a valid json string
     * @return NativeObject     the created native JS object that represents the JSON object
     */
    public NativeObject toObject(String jsonString)
    {
        // TODO deal with json array stirngs
        
        // Parse JSON string
	try {
        	JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(jsonString);
        
		// Create native object 
		return toObject(jsonObject);
	} catch(Exception e){
		e.printStackTrace();
	}
	return null;
    }
    
    /**
     * Takes a JSON object and converts it to a native JS object.
     * 
     * @param jsonObject        the json object
     * @return NativeObject     the created native object
     */
    public NativeObject toObject(JSONObject jsonObject)
    {
        // Create native object 
        NativeObject object = new NativeObject();
        
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext())
        {
            String key = (String)keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject)
            {
                object.put(key, object, toObject((JSONObject)value));
            }
            else
            {
                object.put(key, object, value);
            }
        }
        
        return object;
    }
    
    /**
     * Build a JSON string for a native object
     * 
     * @param nativeObject
     * @param json
     */
    private String nativeObjectToJSONString(NativeObject nativeObject)
    {
	JSONObject json = new JSONObject();
        
        Object[] ids = nativeObject.getIds();
        for (Object id : ids)
        {
            String key = id.toString();
            Object value = nativeObject.get(key, nativeObject);
            json.put(key, valueToJSONString(value));
        }
    
    	return json.toString();	
    }
    
    /**
     * Build JSON string for a native array
     * 
     * @param nativeArray
     */
    private String nativeArrayToJSONString(NativeArray nativeArray)
    {

        Object[] propIds = nativeArray.getIds();
        if (isArray(propIds) == true)
        {      
            JSONArray jsonArray = new JSONArray();
	    for (int i=0; i<propIds.length; i++)
            {
                Object propId = propIds[i];
                if (propId instanceof Integer)
                {
                    Object value = nativeArray.get((Integer)propId, nativeArray);
                    jsonArray.add(valueToJSONString(value));
                }
            }
	    return jsonArray.toString();
        }
        else
        {
    	    JSONObject json = new JSONObject();
            for (Object propId : propIds)
            {
                Object value = nativeArray.get(propId.toString(), nativeArray);
                json.put(propId.toString(), valueToJSONString(value));
            }
	    return json.toString();
        }
    }
    
    /**
     * Look at the id's of a native array and try to determine whether it's actually an Array or a HashMap
     * 
     * @param ids       id's of the native array
     * @return boolean  true if it's an array, false otherwise (ie it's a map)
     */
    private boolean isArray(Object[] ids)
    {
        boolean result = true;
        for (Object id : ids)
        {
            if (id instanceof Integer == false)
            {
               result = false;
               break;
            }
        }
        return result;
    }
    
    /**
     * Convert value to JSON string
     * 
     * @param value
     */
    private String valueToJSONString(Object value)
    {
	JSONObject json = new JSONObject();
        if (value instanceof IdScriptableObject &&
            ((IdScriptableObject)value).getClassName().equals("Date") == true)
        {
            // Get the UTC values of the date
            Object year = NativeObject.callMethod((IdScriptableObject)value, "getUTCFullYear", null);
            Object month = NativeObject.callMethod((IdScriptableObject)value, "getUTCMonth", null);
            Object date = NativeObject.callMethod((IdScriptableObject)value, "getUTCDate", null);
            Object hours = NativeObject.callMethod((IdScriptableObject)value, "getUTCHours", null);
            Object minutes = NativeObject.callMethod((IdScriptableObject)value, "getUTCMinutes", null);
            Object seconds = NativeObject.callMethod((IdScriptableObject)value, "getUTCSeconds", null);
            Object milliSeconds = NativeObject.callMethod((IdScriptableObject)value, "getUTCMilliseconds", null);
            
            // Build the JSON object to represent the UTC date
            json.put("zone","UTC");
	    json.put("year",year);
	    json.put("month",month);
	    json.put("date",date);
	    json.put("hours",hours);
	    json.put("minutes",minutes);
	    json.put("seconds",seconds);
	    json.put("milliseconds",milliSeconds);
	    return json.toString();
        }
        else if (value instanceof NativeJavaObject)
        {
            Object javaValue = Context.jsToJava(value, Object.class);
            return javaValue.toString();
        }
        else if (value instanceof NativeArray)
        {
            // Output the native array
            return nativeArrayToJSONString((NativeArray)value);
        }
        else if (value instanceof NativeObject)
        {
            // Output the native object
            return nativeObjectToJSONString((NativeObject)value);
        }
        else
        {
           return value.toString(); 
        }
    }    
}
