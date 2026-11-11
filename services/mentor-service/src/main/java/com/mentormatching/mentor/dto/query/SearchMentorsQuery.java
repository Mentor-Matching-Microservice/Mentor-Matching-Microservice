package com.mentormatching.mentor.dto.query;

import java.math.BigDecimal;
import java.util.List;

import com.mentormatching.mentor.entity.MeetingType;
import com.mentormatching.mentor.entity.MentorGender;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * Bộ lọc tìm kiếm mentor. Trường tùy chọn null nghĩa là không áp bộ lọc tương ứng.
 *
 * @param page               số trang client bắt đầu từ 1; null hoặc 0 dùng trang đầu, tối đa 100000
 * @param size               số bản ghi mỗi trang, mặc định 20, từ 1 đến 100
 * @param search             chuỗi con tìm trên fullName/headline, được trim; rỗng thì không lọc
 * @param gender             giới tính
 * @param meetingType        hình thức học
 * @param hometownCityId     ID thành phố quê quán
 * @param cityId             alias cho city filter
 * @param currentDistrictId  ID quận/huyện hiện tại
 * @param districtId         alias cho district filter
 * @param subjectId          ID môn học thuộc catalog
 * @param gradeId            ID khối lớp thuộc catalog
 * @param subjectGradeId     ID tổ hợp môn–lớp thuộc catalog
 * @param minPrice           giá tối thiểu của môn active phù hợp, tính cả biên
 * @param maxPrice           giá tối đa của cùng môn active phù hợp, tính cả biên
 * @param minExperienceYears số năm kinh nghiệm tối thiểu, tính cả biên
 * @param maxExperienceYears số năm kinh nghiệm tối đa, tính cả biên
 * @param personalityIds     các personality option phải có đầy đủ, tối đa 20 ID
 * @param highlightIds       các highlight option phải có đầy đủ, tối đa 20 ID
 * @param sortBy             createdAt, fullName, experienceYears hoặc averageRating; null dùng createdAt
 * @param sortDirection      ASC hoặc DESC; null dùng DESC
 */
@Builder
public record SearchMentorsQuery(
        @Min(0) @Max(100000) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 200) String search,
        MentorGender gender,
        MeetingType meetingType,
        @Positive Long hometownCityId,
        @Positive Long cityId,
        @Positive Long currentDistrictId,
        @Positive Long districtId,
        @Positive Long subjectId,
        @Positive Long gradeId,
        @Positive Long subjectGradeId,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal minPrice,
        @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal maxPrice,
        @PositiveOrZero Integer minExperienceYears,
        @PositiveOrZero Integer maxExperienceYears,
        @Size(max = 20) List<@NotNull @Positive Long> personalityIds,
        @Size(max = 20) List<@NotNull @Positive Long> highlightIds,
        @Pattern(regexp = "createdAt|fullName|experienceYears|averageRating") String sortBy,
        @Pattern(regexp = "ASC|DESC") String sortDirection) {

    public SearchMentorsQuery {
        page = page == null ? 0 : page > 0 ? page - 1 : page;
        size = size == null ? 20 : size;
        search = search == null ? null : search.trim();
        if (hometownCityId == null && cityId != null) {
            hometownCityId = cityId;
        }
        if (currentDistrictId == null && districtId != null) {
            currentDistrictId = districtId;
        }
        personalityIds = personalityIds != null ? personalityIds.stream().distinct().toList() : null;
        highlightIds = highlightIds != null ? highlightIds.stream().distinct().toList() : null;
    }
}
