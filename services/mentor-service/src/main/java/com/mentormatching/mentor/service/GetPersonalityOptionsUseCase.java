package com.mentormatching.mentor.service;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.repository.PersonalityOptionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Liệt kê các tính cách có thể chọn cho hồ sơ mentor. */
@Service
@Transactional(readOnly = true)
public class GetPersonalityOptionsUseCase {

    private static final Sort ORDER = Sort.by(Sort.Direction.ASC, "name", "id");

    private final PersonalityOptionRepository personalityOptionRepository;

    public GetPersonalityOptionsUseCase(PersonalityOptionRepository personalityOptionRepository) {
        this.personalityOptionRepository = personalityOptionRepository;
    }

    /**
     * @return các lựa chọn theo tên rồi ID tăng dần; danh sách rỗng nếu chưa có dữ liệu
     */
    public List<MentorOptionResponse> execute() {
        return personalityOptionRepository.findAll(ORDER).stream()
                .map(option -> MentorOptionResponse.builder()
                        .id(option.getId())
                        .name(option.getName())
                        .description(option.getDescription())
                        .build())
                .toList();
    }
}
