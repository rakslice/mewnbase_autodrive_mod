package net.rakslice.mewnbase_mod;

import java.lang.reflect.Field;

public class FieldPatchHelper<T extends Object, V> {

	protected String contextDesc;
	
	public FieldPatchHelper(String contextDesc) {
		this.contextDesc = contextDesc;
	}

	public Field getField(Class<? extends T> classObj, String fieldName) {
		Field fieldObj;
		try {
			fieldObj = classObj.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e1) {
			throw new PatchingException("Found no field called " + fieldName + " in " + contextDesc);
		} catch (SecurityException e1) {
			throw new PatchingException("Got security error while getting field called " + fieldName + " in " + contextDesc);
		}
		fieldObj.setAccessible(true);
		return fieldObj;
	}
	
	public V getValueFromField(Field field, T object) {
		V fieldValue;
		if (field == null) {
			throw new IllegalArgumentException("Call to get a field value in " + contextDesc + " was passed a null Field object");
		}
		try {
			fieldValue = (V) field.get(object);
		} catch (IllegalArgumentException e) {
			throw new PatchingException("Got illegal argument error while getting value of field " + field.getName() + " in " + contextDesc);
		} catch (IllegalAccessException e) {
			throw new PatchingException("Got illegal access error while getting value of field " + field.getName() + " in " + contextDesc);
		}
		return fieldValue;
	}	
	
	public void setValueToField(Field field, T object, V newValue) {
		try {
			field.set(object, newValue);
		} catch (IllegalArgumentException e) {
			throw new PatchingException("Got illegal argument error while setting value of field " + field.getName() + " in " + contextDesc);
		} catch (IllegalAccessException e) {
			throw new PatchingException("Got illegal access error while setting value of field " + field.getName() + " in " + contextDesc);
		}
	}
	
	public V getFieldValue(T object, String fieldName) {
		Class<? extends T> objClass = (Class<? extends T>) object.getClass();
		Field field = getField(objClass, fieldName);
		return getValueFromField(field, object);
	}
	
	public V getStaticFieldValue(Class<T> objClass, String fieldName) {
		Field field = getField(objClass, fieldName);
		return getValueFromField(field, null);
	}
	
	/** Set the value of a protected field with the given name in the given instance of an object */
	public void setFieldValue(T object, String fieldName, V newValue) {
		if (object == null) {
			throw new PatchingException("Can't set instance field " + fieldName + " on null object in " + contextDesc);
		}
		Class<? extends T> objClass = (Class<? extends T>) object.getClass();
		Field field = getField(objClass, fieldName);
		setValueToField(field, object, newValue);
	}
	
	public void setStaticFieldValue(Class<T> objClass, String fieldName, V newValue) {
		Field field = getField(objClass, fieldName);
		setValueToField(field, null, newValue);		
	}

}
