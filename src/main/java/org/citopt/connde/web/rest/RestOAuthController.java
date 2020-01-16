package org.citopt.connde.web.rest;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;

import org.apache.commons.codec.binary.Base64;
import org.citopt.connde.RestConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

@RestController
@RequestMapping(RestConfiguration.BASE_PATH)
public class RestOAuthController {

	private static final Logger LOGGER = Logger.getLogger(RestOAuthController.class.getName());

	@Value("server.address")
	private String serverAddress;

	@RequestMapping(value = "/getAccessCode", method = RequestMethod.GET)
	public String getDeviceCode(@RequestParam("code") String code) {
		return code;
	}

	@RequestMapping(value = "/testOauth", method = RequestMethod.GET)
	public String testOAuth() {
		return randomNumeric(4);
	}

	@RequestMapping(value = "/checkOauthTokenUser", method = RequestMethod.POST)
	public HttpStatus checkOauthTokenUser(@RequestHeader("authorization") String authorizationHeader) {
		LOGGER.log(Level.INFO, "############################### Authorization header : " + authorizationHeader);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<Json> response = restTemplate.getForEntity(serverAddress + "/oauth/check_token?token=" + authorizationHeader, Json.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			LOGGER.log(Level.INFO, "CHECK OAUTH TOKEN FOR USER RETURNED OK ################");
			return HttpStatus.OK;
		}
		return HttpStatus.FORBIDDEN;
	}

	@RequestMapping(value = "/checkOauthTokenSuperuser", method = RequestMethod.POST)
	public HttpStatus checkOauthTokenSuperuser(@RequestHeader("authorization") String authorizationHeader) {
		LOGGER.log(Level.INFO, "CHECK OAUTH TOKEN FOR SUPERUSER RETURNED 403 ################");
		return HttpStatus.FORBIDDEN;
	}

	@RequestMapping(value = "/checkOauthTokenAcl", method = RequestMethod.POST)
	public HttpStatus checkOauthTokenAcl(@RequestHeader("authorization") String authorizationHeader) {
		LOGGER.log(Level.INFO, "CHECK OAUTH TOKEN FOR ACL RETURNED 403 ################");
		return HttpStatus.FORBIDDEN;
	}

	private String getBearerTokenFromAuthHeader(String authorizationHeader) {
		if (!authorizationHeader.isEmpty() && authorizationHeader.startsWith("Bearer")) {
			String[] authorizationHeaderSplit = authorizationHeader.split(" ");
			return authorizationHeaderSplit[1];
		}
		return null;
	}

	private HttpHeaders createHeaders(String username, String password) {
		return new HttpHeaders() {{
			String auth = username + ":" + password;
			byte[] encodedAuth = Base64.encodeBase64(
					auth.getBytes(StandardCharsets.US_ASCII));
			String authHeader = "Basic " + new String(encodedAuth);
			set("Authorization", authHeader);
		}};
	}
}
