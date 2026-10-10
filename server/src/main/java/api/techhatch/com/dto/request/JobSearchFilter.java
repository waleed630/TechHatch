package api.techhatch.com.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobSearchFilter {

    private String keyword;
    private String location;
    private String experienceLevel;
    private String JobType;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    //email of the logged in user, jobs they already applied to are left out
    private String candidateEmail;
    private int page=0;
}
