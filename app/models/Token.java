package models;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Token {
	@Id
	private String token;

	private String email;

	private long time;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
