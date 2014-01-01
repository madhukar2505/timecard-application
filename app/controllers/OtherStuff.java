package controllers;

import java.util.List;

import models.CompanyDbo;
import models.DayCardDbo;
import models.EmailToUserDbo;
import models.StatusEnum;
import models.TimeCardDbo;
import models.Token;
import models.UserDbo;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Scope.Session;
import controllers.auth.Secure;

@With(Secure.class)
public class OtherStuff extends Controller {

	private static final Logger log = LoggerFactory.getLogger(OtherStuff.class);

	public static void company() {
		UserDbo user = Utility.fetchUser();
		if (user != null && !user.isAdmin()) {
			validation.addError("Access",
					"Oops, you do not have access to this page");
			dashboard();
		}
		CompanyDbo company = user.getCompany();
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		log.info("User = " + user + " and Company = " + company);
		List<UserDbo> employees = user.getEmployees();
		List<TimeCardDbo> timeCards = user.getTimecards();
		render(user, company, employees, timeCards,beginOfWeek);
	}

	public static void addCompany() {
		render();
	}


	public static void companyDetails() {
		UserDbo user = Utility.fetchUser();
		CompanyDbo company = user.getCompany();
		log.info("User = " + user.getEmail() + " and Company = " + company);
		List<UserDbo> users = null;
		if (company != null)
			users = company.getUsers();
		render(user, company, users);
	}

	public static void postAddition(String name, String address, String phone,
			String detail) throws Throwable {
		validation.required(name);
		UserDbo user = Utility.fetchUser();

		if (validation.hasErrors()) {
			params.flash(); // add http parameters to the flash scope
			validation.keep(); // keep the errors for the next request
			addCompany();
		}
		CompanyDbo company = new CompanyDbo();
		company.setName(name);
		company.setAddress(address);
		company.setPhoneNumber(phone);
		company.setDescription(detail);
		company.addUser(user);
		user.setCompany(company);
		JPA.em().persist(company);
		JPA.em().persist(user);
		JPA.em().flush();
		company();
	}

	public static void dashboard() {
		Integer id =null;
		UserDbo user = Utility.fetchUser();
		if (user != null && user.isAdmin())
			company();
		else
			employee(id);
	}

	public static void addUser() {
		UserDbo admin = Utility.fetchUser();
		CompanyDbo company = admin.getCompany();
		log.info("Adding users by Admin = " + admin.getEmail()
				+ " and Company = " + company.getName());
		List<UserDbo> users = company.getUsers();
		render(admin, company, users);
	}

	public static void postUserAddition(String useremail, String manager)
			throws Throwable {
		validation.required(useremail);

		if (!useremail.contains("@"))
			validation.addError("useremail", "This is not a valid email");

		EmailToUserDbo existing = JPA.em()
				.find(EmailToUserDbo.class, useremail);
		if (existing != null) {
			validation.addError("useremail", "This email already exists");
		}

		if (validation.hasErrors()) {
			params.flash(); // add http parameters to the flash scope
			validation.keep(); // keep the errors for the next request
			addUser();
		}
		UserDbo admin = Utility.fetchUser();
		CompanyDbo company = admin.getCompany();

		UserDbo user = new UserDbo();
		user.setEmail(useremail);
		user.setCompany(company);
		if (manager == null) {
			// If there is no manager, add the current user as Manager
			user.setManager(admin);
		} else {
			EmailToUserDbo ref = JPA.em().find(EmailToUserDbo.class, manager);
			UserDbo adminDbo = JPA.em().find(UserDbo.class, ref.getValue());
			user.setManager(adminDbo);
		}

		JPA.em().persist(user);

		EmailToUserDbo emailToUser = new EmailToUserDbo();
		emailToUser.setEmail(useremail);
		emailToUser.setValue(user.getId());
		JPA.em().persist(emailToUser);

		company.addUser(user);
		JPA.em().persist(company);

		JPA.em().flush();

		String key = Utility.generateKey();
		Token token = new Token();
		long timestamp = System.currentTimeMillis();
		token.setTime(timestamp);
		token.setToken(key);
		token.setEmail(useremail);
		JPA.em().persist(token);
		JPA.em().flush();
		Utility.sendEmail(useremail, company.getName(), key);
		companyDetails();
	}

	public static void rename(String useremail, String firstmanager,String manager) {

		EmailToUserDbo oldManagerRef = JPA.em().find(EmailToUserDbo.class,
				firstmanager);
		UserDbo oldManager = JPA.em().find(UserDbo.class,
				oldManagerRef.getValue());

		EmailToUserDbo newManagerRef = JPA.em().find(EmailToUserDbo.class,
				manager);
		UserDbo newManager = JPA.em().find(UserDbo.class,
				newManagerRef.getValue());

		EmailToUserDbo empRef = JPA.em().find(EmailToUserDbo.class, useremail);
		UserDbo emp = JPA.em().find(UserDbo.class, empRef.getValue());

		emp.setManager(newManager);
		JPA.em().persist(emp);

		newManager.addEmployee(emp);
		JPA.em().persist(newManager);

		oldManager.deleteEmployee(emp);
		JPA.em().persist(oldManager);

		JPA.em().flush();

		dashboard();
	}

	public static void employee(Integer id) {
		UserDbo employee = Utility.fetchUser();
		List<UserDbo> employees = employee.getEmployees();
		List<TimeCardDbo> timeCards = employee.getTimecards();
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		DateTimeFormatter fmt = DateTimeFormat.forPattern("MMM dd");
		if (id == null) {
			String email = employee.getEmail();
			String currentWeek = fmt.print(beginOfWeek);
			DayCardDbo[] dayCards = new DayCardDbo[7];
			int[] noofhours = new int[7];
			String[] details = new String[7];
			for (int i = 0; i < 7; i++) {
				noofhours[i] = 0;
				details[i] = "";
				dayCards[i] = new DayCardDbo();
				dayCards[i].setDate(beginOfWeek.plusDays(i));
			}
			render(timeCards, beginOfWeek, email, currentWeek, employee, dayCards, noofhours, details);
		
		} else {
			TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, id);
			StatusEnum status = timeCard.getStatus();
			boolean readOnly;
			if (status == StatusEnum.APPROVED)
				readOnly = true;
			else
				readOnly = false;
			List<DayCardDbo> dayCardDbo = timeCard.getDaycards();
			int[] noofhours = new int[7];
			String[] details = new String[7];
			int i = 0;
			for (DayCardDbo dayCard : dayCardDbo) {
				noofhours[i] = dayCard.getNumberOfHours();
				details[i] = dayCard.getDetail();
				i++;
			}
			render(timeCard, timeCards, dayCardDbo, noofhours, details,beginOfWeek,
					readOnly, status);
		}
	}
	
	public static void addEditTimeCardRender(Integer timeCardId){
		StatusEnum status = null;
		TimeCardDbo timeCard = null;
		DayCardDbo dayC=null; 
		boolean readOnly=false;
		LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
		if(timeCardId==null){
			 timeCard = new TimeCardDbo();
			 timeCard.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
			 for (int i = 0; i < 7; i++) {
					 dayC = new DayCardDbo();
					 timeCard.getDaycards().add(dayC);
					 dayC.setDate(beginOfWeek.plusDays(i));
			 }
		}else{
			timeCard = JPA.em().find(TimeCardDbo.class, timeCardId);
			 status = timeCard.getStatus();
				if (status == StatusEnum.APPROVED)
					readOnly = true;
				else
					readOnly = false;
		}
		render(readOnly,timeCard,beginOfWeek);
	}
	
	public static void deleteTimeCardRender(Integer timeCardId){
		Integer id=timeCardId;
		render(id);
	}
	public static void postDeleteTimeCard(Integer timeCardId){
		Integer id = null;
		String userName = Session.current().get("username");
		EmailToUserDbo emailToUserDbo= JPA.em().find(EmailToUserDbo.class, userName);
		TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, timeCardId);
		UserDbo user = JPA.em().find(UserDbo.class, emailToUserDbo.getValue());
		user.deleteTimeCard(timeCard);
		JPA.em().persist(user);
		JPA.em().flush();
		employee(id);
	}

	public static void postTimeAddition2(Integer timeCardId,Integer[] dayCardsid, int[] noofhours, String[] details) throws Throwable {
		Integer id = null;
		if (timeCardId ==null||timeCardId==0) {
			UserDbo user = Utility.fetchUser();
			CompanyDbo company = user.getCompany();
			UserDbo manager = user.getManager();
			TimeCardDbo timeCardDbo = new TimeCardDbo();
			timeCardDbo.setBeginOfWeek(Utility.calculateBeginningOfTheWeek());
			LocalDate beginOfWeek = Utility.calculateBeginningOfTheWeek();
			int totalhours = 0;
			for (int i = 0; i < 7; i++) {
				DayCardDbo dayC = new DayCardDbo();
				dayC.setDate(beginOfWeek.plusDays(i));
				if (noofhours[i] > 12) {
					validation.addError("noofhours[i]",
							"hours should be less than 12");
				} else {
					dayC.setNumberOfHours(noofhours[i]);
					totalhours = totalhours + noofhours[i];
					dayC.setDetail(details[i]);
					timeCardDbo.addDayCard(dayC);
					JPA.em().persist(dayC);
				}
			}
			if (validation.hasErrors()) {
				params.flash(); // add http parameters to the flash scope
				validation.keep(); // keep the errors for the next request
				employee(id);
			}
			JPA.em().flush();

			timeCardDbo.setNumberOfHours(totalhours);
			timeCardDbo.setApproved(false);
			timeCardDbo.setStatus(StatusEnum.SUBMIT);
			user.addTimecards(timeCardDbo);
			JPA.em().persist(timeCardDbo);
			JPA.em().persist(user);
			JPA.em().flush();
			Utility.sendEmailForApproval(manager.getEmail(), company.getName(),
					user.getEmail());
			employee(id);
		} else {
			TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, timeCardId);
			int sum = 0;
			for (int i = 0; i < 7; i++) {
				DayCardDbo dayC = JPA.em()
						.find(DayCardDbo.class, dayCardsid[i]);
				if (noofhours[i] > 12) {
					validation.addError("noofhours[i]",
							"hours should be less than 12");
				} else {
					dayC.setNumberOfHours(noofhours[i]);
					dayC.setDetail(details[i]);
					JPA.em().persist(dayC);

					sum += noofhours[i];
				}
				if (validation.hasErrors()) {
					params.flash();
					validation.keep();
					employee(id);
				}

			}
			timeCard.setNumberOfHours(sum);
			timeCard.setStatus(StatusEnum.SUBMIT);
			JPA.em().persist(timeCard);
			JPA.em().flush();
			employee(id);
		}
	}

	public static void detail(Integer id) {
		TimeCardDbo timeCard = JPA.em().find(TimeCardDbo.class, id);
		List<DayCardDbo> dayCardDbo = timeCard.getDaycards();
		StatusEnum status = timeCard.getStatus();
		render(dayCardDbo, timeCard, status);
	}

	public static void userCards(String email) {
		EmailToUserDbo ref = JPA.em().find(EmailToUserDbo.class, email);
		UserDbo user = JPA.em().find(UserDbo.class, ref.getValue());
		List<TimeCardDbo> timeCards = user.getTimecards();
		render(email, timeCards);
	}

	public static void cardsAction(Integer timeCardId, int status) {

		TimeCardDbo ref = JPA.em().find(TimeCardDbo.class, timeCardId);
		if (ref != null) {
			if (status == 1) {
				ref.setStatus(StatusEnum.APPROVED);
				ref.setApproved(true);
			} else {
				ref.setStatus(StatusEnum.CANCELLED);
				ref.setApproved(false);
			}

		}
		JPA.em().persist(ref);
		JPA.em().flush();
		company();
	}

	public static void success() {
		render();
	}

	public static void cancel() {
		render();
	}
}
