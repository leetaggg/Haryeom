package com.ioi.haryeom.chat.domain;

import com.ioi.haryeom.common.domain.BaseTimeEntity;
import com.ioi.haryeom.member.domain.Member;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "teacher_member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member teacherMember;

    @JoinColumn(name = "student_member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member studentMember;


    @Builder
    public ChatRoom(Member teacherMember, Member studentMember) {
        this.teacherMember = teacherMember;
        this.studentMember = studentMember;
    }

}
