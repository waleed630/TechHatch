package api.techhatch.com.service;

import api.techhatch.com.dto.request.JobCreateRequest;
import api.techhatch.com.dto.response.JobResponse;
import api.techhatch.com.exception.BadRequestException;
import api.techhatch.com.exception.ResourceNotFoundException;
import api.techhatch.com.exception.UnauthorizedException;
import api.techhatch.com.model.Job;
import api.techhatch.com.model.RecruiterProfile;
import api.techhatch.com.model.Users;
import api.techhatch.com.repository.JobRepo;
import api.techhatch.com.repository.RecruiterProfileRepo;
import api.techhatch.com.repository.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    private static final String OWNER = "recruiter@example.com";

    @Mock
    private RecruiterProfileRepo recruiterRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private JobRepo jobRepo;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(recruiterRepo, userRepo, jobRepo, new ObjectMapper());
    }

    private RecruiterProfile recruiter(Long id, String email) {
        return RecruiterProfile.builder()
                .id(id)
                .user(Users.builder().email(email).role(Users.Role.RECRUITER).build())
                .companyName("Test Co")
                .build();
    }

    private Job job(RecruiterProfile owner, LocalDateTime expiry) {
        return Job.builder()
                .id(10L)
                .recruiterProfile(owner)
                .title("Backend Developer")
                .jobType(Job.JobType.FULL_TIME)
                .experienceLevel(Job.ExperienceLevel.MID)
                .jobStatus(Job.JobStatus.ACTIVE)
                .currency("PKR")
                .requiredSkills("[\"Java\"]")
                .postedDate(LocalDateTime.now().minusDays(1))
                .expiryDate(expiry)
                .viewCount(3)
                .applicationCount(0)
                .build();
    }

    private JobCreateRequest request(String min, String max) {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Backend Developer");
        request.setDescription("A long enough description of the job for the validation rules to accept it.");
        request.setLocation("Lahore");
        request.setJobType("FULL_TIME");
        request.setWorkMode("ONSITE");
        request.setExperienceLevel("MID");
        request.setSalaryMin(new BigDecimal(min));
        request.setSalaryMax(new BigDecimal(max));
        request.setCurrency("PKR");
        request.setRequiredSkills(List.of("Java"));
        return request;
    }

    @Test
    void postJobNeedsACompanyProfile() {
        when(recruiterRepo.findByUserEmail(OWNER)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> jobService.postJob(OWNER, request("100", "200")));

        assertTrue(ex.getMessage().contains("complete your company profile"));
        verify(jobRepo, never()).save(any());
    }

    @Test
    void postJobRejectsMinimumSalaryAboveMaximum() {
        when(recruiterRepo.findByUserEmail(OWNER)).thenReturn(Optional.of(recruiter(1L, OWNER)));

        assertThrows(BadRequestException.class, () -> jobService.postJob(OWNER, request("500", "200")));
        verify(jobRepo, never()).save(any());
    }

    @Test
    void updateJobSavesCurrencyAndExpiryDate() {
        RecruiterProfile owner = recruiter(1L, OWNER);
        Job job = job(owner, LocalDateTime.now().plusDays(10));
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));
        when(recruiterRepo.findByUserEmail(OWNER)).thenReturn(Optional.of(owner));
        when(jobRepo.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newExpiry = LocalDateTime.now().plusDays(90);
        JobCreateRequest request = request("100", "200");
        request.setCurrency("USD");
        request.setExpiryDate(newExpiry);

        JobResponse response = jobService.updateJob(OWNER, 10L, request);

        assertEquals("USD", response.getCurrency());
        assertEquals(newExpiry, job.getExpiryDate());
    }

    @Test
    void updateJobOfAnotherRecruiterIsRefused() {
        Job job = job(recruiter(1L, OWNER), LocalDateTime.now().plusDays(10));
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job));
        when(recruiterRepo.findByUserEmail("other@example.com")).thenReturn(Optional.of(recruiter(2L, "other@example.com")));

        assertThrows(UnauthorizedException.class,
                () -> jobService.updateJob("other@example.com", 10L, request("100", "200")));
        verify(jobRepo, never()).save(any());
    }

    @Test
    void myJobsIsEmptyWithoutACompanyProfile() {
        when(recruiterRepo.findByUserEmail(OWNER)).thenReturn(Optional.empty());

        Page<JobResponse> jobs = jobService.getMyJobs(OWNER, 0);

        assertTrue(jobs.isEmpty());
    }

    @Test
    void statsAreZeroWithoutACompanyProfile() {
        when(recruiterRepo.findByUserEmail(OWNER)).thenReturn(Optional.empty());

        Map<String, Long> stats = jobService.getMyJobStats(OWNER);

        assertEquals(0L, stats.get("totalJobs"));
        assertEquals(0L, stats.get("activeJobs"));
        assertEquals(0L, stats.get("totalApplications"));
    }

    @Test
    void viewFromAVisitorIsCounted() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job(recruiter(1L, OWNER), LocalDateTime.now().plusDays(10))));

        JobResponse response = jobService.getJobById(10L, null);

        verify(jobRepo).incrementViewCount(10L);
        assertEquals(4, response.getViewCount());
    }

    @Test
    void viewFromTheOwnerIsNotCounted() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job(recruiter(1L, OWNER), LocalDateTime.now().plusDays(10))));

        JobResponse response = jobService.getJobById(10L, OWNER);

        verify(jobRepo, never()).incrementViewCount(any());
        assertEquals(3, response.getViewCount());
    }

    @Test
    void activeJobPastItsExpiryDateIsShownAsExpired() {
        when(jobRepo.findById(10L)).thenReturn(Optional.of(job(recruiter(1L, OWNER), LocalDateTime.now().minusDays(1))));

        JobResponse response = jobService.getJobById(10L, OWNER);

        assertEquals("EXPIRED", response.getStatus());
    }
}
