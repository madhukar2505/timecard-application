package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;

import play.db.jpa.Model;

@Entity
public class UserDbo {

	@Id
	@GeneratedValue
	private int id;

	@Column(unique = true)
	private String email;

	private String password;

	private String firstName;

	private String lastName;

	private String phone;

	private boolean isAdmin;

	@ManyToOne
	private CompanyDbo company;

	@ManyToOne
	private UserDbo manager;

	public List<TimeCardDbo> getTimecards() {
		return timecards;
	}

	public void setTimecards(List<TimeCardDbo> timecards) {
		this.timecards = timecards;
	}

	@OneToMany(mappedBy = "manager")
	private List<UserDbo> employees = new ArrayList<UserDbo>();

	@OneToMany
	private List<TimeCardDbo> timecards = new ArrayList<TimeCardDbo>();

	public int getId() {
		return id;
	}

	public List<UserDbo> getEmployees() {
		return employees;
	}

	public void setEmployees(List<UserDbo> employees) {
		this.employees = employees;
	}

	public void addEmployee(UserDbo employee) {
		this.employees.add(employee);
	}

	public void deleteEmployee(UserDbo employee) {
		this.employees.remove(employee);
	}
	public void deleteTimeCard(TimeCardDbo timecards) {
		this.timecards.remove(timecards);
	}

	public void setId(int id) {
		this.id = id;
	}

	public CompanyDbo getCompany() {
		return company;
	}

	public void setCompany(CompanyDbo company) {
		this.company = company;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public UserDbo getManager() {
		return manager;
	}

	public void setManager(UserDbo manager) {
		this.manager = manager;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}

	public void addTimecards(TimeCardDbo timecard) {
		this.timecards.add(timecard);
	}
}
