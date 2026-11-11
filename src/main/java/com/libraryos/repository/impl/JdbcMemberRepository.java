package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;
import com.libraryos.repository.MemberRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMemberRepository implements MemberRepository {

    @Override
    public List<Member> findAll() {
        String sql = "SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "ORDER BY m.id DESC";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all members", e);
        }
        return list;
    }

    @Override
    public List<Member> search(String query, Integer memberTypeId, MemberStatus status) {
        StringBuilder sql = new StringBuilder("SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(m.full_name_en) LIKE ? OR LOWER(m.full_name_bn) LIKE ? " +
                    "OR LOWER(m.membership_number) LIKE ? OR LOWER(m.institutional_id) LIKE ? OR m.phone LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (memberTypeId != null && memberTypeId > 0) {
            sql.append("AND m.member_type_id = ? ");
            params.add(memberTypeId);
        }

        if (status != null) {
            sql.append("AND m.status = ? ");
            params.add(status.name());
        }

        sql.append("ORDER BY m.id DESC");

        List<Member> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search members", e);
        }
        return list;
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "WHERE m.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find member by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> findByMembershipNumber(String membershipNumber) {
        if (membershipNumber == null || membershipNumber.isBlank()) return Optional.empty();
        String sql = "SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "WHERE m.membership_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, membershipNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find member by number: " + membershipNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) return Optional.empty();
        String sql = "SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "WHERE m.phone = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find member by phone: " + phone, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Member> findByInstitutionalId(String institutionalId) {
        if (institutionalId == null || institutionalId.isBlank()) return Optional.empty();
        String sql = "SELECT m.id, m.membership_number, m.member_type_id, mt.name as member_type_name, " +
                "m.full_name_en, m.full_name_bn, m.institutional_id, m.department, m.program, " +
                "m.semester_or_session, m.phone, m.email, m.address, m.photo_path, " +
                "m.membership_start_date, m.membership_expiry_date, m.status, m.notes, " +
                "m.created_at, m.updated_at " +
                "FROM members m " +
                "LEFT JOIN member_types mt ON m.member_type_id = mt.id " +
                "WHERE m.institutional_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, institutionalId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find member by institutional ID: " + institutionalId, e);
        }
        return Optional.empty();
    }

    @Override
    public Member create(Member member) {
        String sql = "INSERT INTO members (membership_number, member_type_id, full_name_en, full_name_bn, " +
                "institutional_id, department, program, semester_or_session, phone, email, address, " +
                "photo_path, membership_start_date, membership_expiry_date, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getMembershipNumber());
            ps.setInt(2, member.getMemberTypeId());
            ps.setString(3, member.getFullNameEn());
            ps.setString(4, member.getFullNameBn());
            ps.setString(5, member.getInstitutionalId());
            ps.setString(6, member.getDepartment());
            ps.setString(7, member.getProgram());
            ps.setString(8, member.getSemesterOrSession());
            ps.setString(9, member.getPhone());
            ps.setString(10, member.getEmail());
            ps.setString(11, member.getAddress());
            ps.setString(12, member.getPhotoPath());
            ps.setDate(13, Date.valueOf(member.getMembershipStartDate()));
            ps.setDate(14, Date.valueOf(member.getMembershipExpiryDate()));
            ps.setString(15, member.getStatus() != null ? member.getStatus().name() : MemberStatus.ACTIVE.name());
            ps.setString(16, member.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    member.setId(rs.getLong(1));
                }
            }
            return member;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert member: " + member.getMembershipNumber(), e);
        }
    }

    @Override
    public void update(Member member) {
        String sql = "UPDATE members SET member_type_id = ?, full_name_en = ?, full_name_bn = ?, " +
                "institutional_id = ?, department = ?, program = ?, semester_or_session = ?, phone = ?, " +
                "email = ?, address = ?, photo_path = ?, membership_expiry_date = ?, status = ?, notes = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, member.getMemberTypeId());
            ps.setString(2, member.getFullNameEn());
            ps.setString(3, member.getFullNameBn());
            ps.setString(4, member.getInstitutionalId());
            ps.setString(5, member.getDepartment());
            ps.setString(6, member.getProgram());
            ps.setString(7, member.getSemesterOrSession());
            ps.setString(8, member.getPhone());
            ps.setString(9, member.getEmail());
            ps.setString(10, member.getAddress());
            ps.setString(11, member.getPhotoPath());
            ps.setDate(12, Date.valueOf(member.getMembershipExpiryDate()));
            ps.setString(13, member.getStatus().name());
            ps.setString(14, member.getNotes());
            ps.setLong(15, member.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update member: " + member.getId(), e);
        }
    }

    @Override
    public void updateStatus(Long memberId, MemberStatus status) {
        String sql = "UPDATE members SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, memberId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update member status for id: " + memberId, e);
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getLong("id"));
        m.setMembershipNumber(rs.getString("membership_number"));
        m.setMemberTypeId(rs.getInt("member_type_id"));
        m.setMemberTypeName(rs.getString("member_type_name"));
        m.setFullNameEn(rs.getString("full_name_en"));
        m.setFullNameBn(rs.getString("full_name_bn"));
        m.setInstitutionalId(rs.getString("institutional_id"));
        m.setDepartment(rs.getString("department"));
        m.setProgram(rs.getString("program"));
        m.setSemesterOrSession(rs.getString("semester_or_session"));
        m.setPhone(rs.getString("phone"));
        m.setEmail(rs.getString("email"));
        m.setAddress(rs.getString("address"));
        m.setPhotoPath(rs.getString("photo_path"));

        Date sDate = rs.getDate("membership_start_date");
        if (sDate != null) m.setMembershipStartDate(sDate.toLocalDate());

        Date eDate = rs.getDate("membership_expiry_date");
        if (eDate != null) m.setMembershipExpiryDate(eDate.toLocalDate());

        String statusStr = rs.getString("status");
        if (statusStr != null) m.setStatus(MemberStatus.valueOf(statusStr));

        m.setNotes(rs.getString("notes"));

        Timestamp cTs = rs.getTimestamp("created_at");
        if (cTs != null) m.setCreatedAt(cTs.toLocalDateTime());

        Timestamp uTs = rs.getTimestamp("updated_at");
        if (uTs != null) m.setUpdatedAt(uTs.toLocalDateTime());

        return m;
    }
}

