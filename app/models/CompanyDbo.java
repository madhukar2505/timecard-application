package models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class CompanyDbo {
	@Id
	@GeneratedValue
	private int key;

	@OneToMany
	private List<UserDbo> users = new ArrayList<UserDbo>();

	private String phoneNumber;

	private String address;

	private String name;

	private String description;

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<UserDbo> getUsers() {
		return users;
	}

	public void setUsers(List<UserDbo> users) {
		this.users = users;
	}

	public void addUser(UserDbo userDbo) {
		this.users.add(userDbo);
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
