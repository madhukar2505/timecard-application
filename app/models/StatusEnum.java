package models;

import java.util.HashMap;
import java.util.Map;

public enum StatusEnum {
	SUBMIT("submit"), APPROVED("approved"), CANCELLED("cancelled");

	public static Map<String, StatusEnum> mapForConversion = new HashMap<String, StatusEnum>();

	static {
		for (StatusEnum type : StatusEnum.values()) {
			mapForConversion.put(type.getDbValue(), type);
		}
	}

	private String dbValue;

	private StatusEnum(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}
}
