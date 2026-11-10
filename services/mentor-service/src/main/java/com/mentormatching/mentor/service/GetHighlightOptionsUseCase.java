package com.mentormatching.mentor.service;

import java.util.List;

import com.mentormatching.mentor.dto.response.MentorOptionResponse;
import com.mentormatching.mentor.repository.MentorHighlightOptionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Liệt kê các điểm nổi bật có thể chọn cho hồ sơ mentor. */
@Service
@Transactional(readOnly = true)
public class GetHighlightOptionsUseCase {

    private static final Sort ORDER = Sort.by(Sort.Direction.ASC, "name", "id");

    private final MentorHighlightOptionRepository mentorHighlightOptionRepository;

    public GetHighlightOptionsUseCase(MentorHighlightOptionRepository mentorHighlightOptionRepository) {
        this.mentorHighlightOptionRepository = mentorHighlightOptionRepository;
    }

    /**
     * @return các lựa chọn theo tên rồi ID tăng dần; danh sách rỗng nếu chưa có dữ liệu
     */
    public List<MentorOptionResponse> execute() {
        return mentorHighlightOptionRepository.findAll(ORDER).stream()
                .map(option -> MentorOptionResponse.builder()
                        .id(option.getId())
                        .name(option.getName())
                        .description(option.getDescription())
                        .build())
                .toList();
    }
}
