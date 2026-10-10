package api.techhatch.com.service;

import api.techhatch.com.model.JobApplication;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationMailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Tell the candidate that the recruiter changed the application status or notes.
     * A mail problem is only logged, it must never undo the status update.
     */
    public void sendApplicationStatusEmail(JobApplication application) {
        //everything is read here, the mail itself is sent in the background so the recruiter does not wait for it
        final String toEmail = application.getCandidateProfile().getUser().getEmail();
        final String subject = "TechHatch - Update on your application for " + application.getJob().getTitle();

        final Context ctx = new Context();
        ctx.setVariable("appName", "TechHatch");
        ctx.setVariable("candidateName", application.getCandidateProfile().getFullName());
        ctx.setVariable("jobTitle", application.getJob().getTitle());
        ctx.setVariable("companyName", application.getJob().getRecruiterProfile().getCompanyName());
        ctx.setVariable("status", statusLabel(application.getStatus()));
        ctx.setVariable("notes", application.getRecruiterNotes());

        CompletableFuture.runAsync(() -> send(toEmail, subject, ctx));
    }

    private void send(String toEmail, String subject, Context ctx) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String htmlContent = templateEngine.process("status-update-mail", ctx);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.addInline("logo", new ClassPathResource("/static/techhatch_logo.png"), "image/png");
            mailSender.send(message);

            log.info("Application status email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Could not send application status email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String statusLabel(JobApplication.Status status) {
        return switch (status) {
            case APPLIED -> "Applied";
            case UNDER_REVIEW -> "Under Review";
            case SHORTLISTED -> "Shortlisted";
            case INTERVIEW -> "Interview";
            case OFFERED -> "Offered";
            case REJECTED -> "Rejected";
        };
    }
}
