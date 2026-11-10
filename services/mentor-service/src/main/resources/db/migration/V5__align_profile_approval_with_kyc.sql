UPDATE mentor_profiles p
JOIN mentor_verifications v ON v.mentor_id = p.id
SET p.approval_status = 'APPROVED',
    p.approved_by = v.verified_by,
    p.approved_at = v.verified_at,
    p.approval_note = NULL
WHERE v.verification_status = 'VERIFIED'
  AND (p.approval_status IS NULL OR p.approval_status IN ('DRAFT', 'PENDING'));

UPDATE mentor_profiles p
JOIN mentor_verifications v ON v.mentor_id = p.id
SET p.approval_status = 'REJECTED',
    p.approved_by = NULL,
    p.approved_at = NULL,
    p.approval_note = v.rejection_reason
WHERE v.verification_status = 'REJECTED'
  AND (p.approval_status IS NULL OR p.approval_status IN ('DRAFT', 'PENDING'));
