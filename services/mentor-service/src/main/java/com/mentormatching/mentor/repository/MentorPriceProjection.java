package com.mentormatching.mentor.repository;
import java.math.BigDecimal;
/** Internal Spring Data projection, not an API DTO. */
public interface MentorPriceProjection {
    /**
     * Lấy ID mentor của nhóm tổng hợp.
     * @return ID hồ sơ mentor
     */
    Long getMentorId();
    /**
     * Lấy giá thấp nhất của các môn active có giá trong nhóm.
     * @return giá tối thiểu, truy vấn hiện tại đã loại giá null
     */
    BigDecimal getMinPrice();
}
