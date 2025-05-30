package org.orcid.core.utils;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Base64;
import org.orcid.core.manager.EncryptionManager;
import org.orcid.core.manager.impl.OrcidUrlManager;
import org.orcid.core.togglz.Features;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class VerifyEmailUtils {

    @Resource(name = "messageSource")
    private MessageSource messages;

    @Resource
    private OrcidUrlManager orcidUrlManager;

    @Resource
    private EncryptionManager encryptionManager;

    public Map<String, Object> createParamsForVerificationEmail(String emailFriendlyName, String orcid, String email, String primaryEmail, Locale locale) {
        Map<String, Object> templateParams = new HashMap<String, Object>();

        String subject = getSubject(primaryEmail.equalsIgnoreCase(email) ? "email.subject.verify_reminder_primary" : "email.subject.verify_reminder", locale);

        templateParams.put("primaryEmail", primaryEmail);
        templateParams.put("isPrimary", primaryEmail.equalsIgnoreCase(email));
        templateParams.put("emailName", emailFriendlyName);
        String verificationUrl = createVerificationUrl(email, orcidUrlManager.getBaseUrl());
        templateParams.put("verificationUrl", verificationUrl);
        templateParams.put("orcid", orcid);
        templateParams.put("email", email);
        templateParams.put("subject", subject);
        templateParams.put("baseUri", orcidUrlManager.getBaseUrl());
        templateParams.put("baseUriHttp", orcidUrlManager.getBaseUriHttp());
        addMessageParams(templateParams, locale);
        return templateParams;
    }

    public String getSubject(String code, Locale locale) {
        return messages.getMessage(code, null, locale);
    }

    public void addMessageParams(Map<String, Object> templateParams, Locale locale) {
        Map<String, Boolean> features = getFeatures();
        templateParams.put("messages", this.messages);
        templateParams.put("messageArgs", new Object[0]);
        templateParams.put("locale", locale);
        templateParams.put("features", features);
    }

    private Map<String, Boolean> getFeatures() {
        Map<String, Boolean> features = new HashMap<String, Boolean>();
        for (Features f : Features.values()) {
            features.put(f.name(), f.isActive());
        }
        return features;
    }

    public String createVerificationUrl(String email, String baseUri) {
        return createEmailBaseUrl(email, baseUri, "verify-email");
    }

    public String createClaimVerificationUrl(String email, String baseUri) {
        return createEmailBaseUrl(email, baseUri, "claim");
    }

    public String createUpdateEmailFrequencyUrl(String email) {
        return createEmailBaseUrl(email, orcidUrlManager.getBaseUrl(), "notifications/frequencies");
    }

    public String createResetEmail(String userEmail, String baseUri) {
        XMLGregorianCalendar date = DateUtils.convertToXMLGregorianCalendarNoTimeZoneNoMillis(new Date());
        String resetParams = MessageFormat.format("email={0}&issueDate={1}", new Object[] { userEmail, date.toXMLFormat() });
        return createEmailBaseUrl(resetParams, baseUri, "reset-password-email");
    }

    public String createReactivationUrl(String userEmail, String baseUri) {
        XMLGregorianCalendar date = DateUtils.convertToXMLGregorianCalendarNoTimeZoneNoMillis(new Date());
        String resetParams = MessageFormat.format("email={0}&issueDate={1}", new Object[] { userEmail, date.toXMLFormat() });
        return createEmailBaseUrl(resetParams, baseUri, "reactivation");
    }

    private String createEmailBaseUrl(String unencryptedParams, String baseUri, String path) {
        // Encrypt and encode params
        String encryptedUrlParams = encryptionManager.encryptForExternalUse(unencryptedParams);
        String base64EncodedParams = null;
        try {
            base64EncodedParams = Base64.encodeBase64URLSafeString(encryptedUrlParams.getBytes("UTF-8"));

        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
        return String.format("%s/%s/%s", baseUri, path, base64EncodedParams);
    }
}
