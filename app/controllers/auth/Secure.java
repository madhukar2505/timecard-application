package controllers.auth;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import javax.persistence.EntityManager;

import models.EmailToUserDbo;
import models.UserDbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.libs.Crypto;
import play.libs.Time;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Util;
import play.mvc.Scope.Session;
import play.utils.Java;
import controllers.Application;
import controllers.OtherStuff;
public class Secure extends Controller {

	private static final Logger log = LoggerFactory.getLogger(Secure.class);
	
    @Before(unless={"login", "authenticate", "logout"})
    static void checkAccess() throws Throwable {
        // Authent
        if(!session.contains("username")) {
            flash.put("url", "GET".equals(request.method) ? request.url : Play.ctxPath + "/"); // seems a good default
            login();
        }
        log.info("user is already authenticated");
        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if(check != null) {
            check(check);
        }

    	//String username = session.get("username");
    }

    private static void check(Check check) throws Throwable {
        for(String profile : check.value()) {
            boolean hasProfile = (Boolean)Security.invoke("check", profile);
            if(!hasProfile) {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

    // ~~~ Login

    public static void login() throws Throwable {
        Http.Cookie remember = request.cookies.get("rememberme");
        if(remember != null) {
        	log.info("remembering user");
            int firstIndex = remember.value.indexOf("-");
            int lastIndex = remember.value.lastIndexOf("-");
            if (lastIndex > firstIndex) {
                String sign = remember.value.substring(0, firstIndex);
                String restOfCookie = remember.value.substring(firstIndex + 1);
                String username = remember.value.substring(firstIndex + 1, lastIndex);
                String time = remember.value.substring(lastIndex + 1);
                Date expirationDate = new Date(Long.parseLong(time)); // surround with try/catch?
                Date now = new Date();
                if (expirationDate == null || expirationDate.before(now)) {
                    logout();
                }
                if(Crypto.sign(restOfCookie).equals(sign)) {
                    addUserToSession(username);
                    redirectToOriginalURL();
                }
            }
        }
        
        log.info("hitting login page");
        flash.keep("url");
        Application.signin();
    }

    public static void authenticate(@Required String username, String password, boolean remember) throws Throwable {
    	log.info("trying to login with username="+username);
        // Check tokens
        Boolean allowed = (Boolean)Security.invoke("authenticate", username, password);
        if(!allowed) {
        	flash.error("Login access is denied(username or password is incorrect)");
        }
        if(validation.hasErrors() || !allowed) {
            flash.keep("url");
            params.flash(); // add http parameters to the flash scope
	        validation.keep(); // keep the errors for the next request
            login();
        }

        //Session temp = session;
        
        // Mark user as connected
        addUserToSession(username);
        
        // Remember if needed
        if(remember) {
            Date expiration = new Date();
            String duration = "30d";  // maybe make this override-able 
            expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
            response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-" + username + "-" + expiration.getTime(), duration);
        }
        
      OtherStuff.dashboard();
    }

    @Util
	public static void addUserToSession(String username) {
		session.put("username", username);
		//CreatePhone.setupKeyPhone(username);
	}

    public static void logout() throws Throwable {
        log.info("logging out, redirect to login page");
        Security.invoke("onDisconnect");
        String sid = session.get("sid");
        session.clear();
        session.put("sid", sid); //we only want the sid leftover
        response.removeCookie("rememberme");
        Security.invoke("onDisconnected");
        flash.success("Next time, you can stay logged in pretty much forever and just lock your computer instead ;)");
        Application.index();
    }

    // ~~~ Utils

    static void redirectToOriginalURL() throws Throwable {
        Security.invoke("onAuthenticated");
        String url = flash.get("url");
        if(url == null) {
            url = Play.ctxPath + "/";
        }
        redirect(url);
    }

    public static class Security extends Controller {

        /**
         * @Deprecated
         * 
         * @param username
         * @param password
         * @return
         */
        static boolean authentify(String username, String password) {
            throw new UnsupportedOperationException();
        }

        /**
         * This method is called during the authentication process. This is where you check if
         * the user is allowed to log in into the system. This is the actual authentication process
         * against a third party system (most of the time a DB).
         *
         * @param username
         * @param password
         * @return true if the authentication process succeeded
         */
        static boolean authenticate(String username, String password) {
        	EntityManager em = JPA.em();
        	EmailToUserDbo email = em.find(EmailToUserDbo.class, username);
        	if(email != null) {
        		UserDbo user = em.find(UserDbo.class, email.getValue());
        		if(user != null && user.getPassword().equals(password))
        			return true;
        	}
        	
            return false;
        }

        /**
         * This method checks that a profile is allowed to view this page/method. This method is called prior
         * to the method's controller annotated with the @Check method. 
         *
         * @param profile
         * @return true if you are allowed to execute this controller method.
         */
        static boolean check(String profile) {
            return true;
        }

        /**
         * This method returns the current connected username
         * @return
         */
        static String connected() {
            return session.get("username");
        }

        /**
         * Indicate if a user is currently connected
         * @return  true if the user is connected
         */
        static boolean isConnected() {
            return session.contains("username");
        }

        /**
         * This method is called after a successful authentication.
         * You need to override this method if you with to perform specific actions (eg. Record the time the user signed in)
         */
        static void onAuthenticated() {
        }

         /**
         * This method is called before a user tries to sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the name of the user who signed off)
         */
        static void onDisconnect() {
        }

         /**
         * This method is called after a successful sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the time the user signed off)
         */
        static void onDisconnected() {
        }

        /**
         * This method is called if a check does not succeed. By default it shows the not allowed page (the controller forbidden method).
         * @param profile
         */
        static void onCheckFailed(String profile) {
            forbidden();
        }

        private static Object invoke(String m, Object... args) throws Throwable {

            try {
                return Java.invokeChildOrStatic(Security.class, m, args);       
            } catch(InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

    }

}

