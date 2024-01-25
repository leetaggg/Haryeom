package com.ioi.haryeom.matching.service;

import com.ioi.haryeom.chat.domain.ChatRoom;
import com.ioi.haryeom.chat.dto.CreateMatchingResponse;
import com.ioi.haryeom.chat.exception.ChatRoomNotFoundException;
import com.ioi.haryeom.chat.repository.ChatRoomRepository;
import com.ioi.haryeom.common.domain.Subject;
import com.ioi.haryeom.common.repository.SubjectRepository;
import com.ioi.haryeom.common.util.IdGenerator;
import com.ioi.haryeom.matching.dto.CreateMatchingRequest;
import com.ioi.haryeom.matching.dto.RespondToMatchingRequest;
import com.ioi.haryeom.matching.dto.RespondToMatchingResponse;
import com.ioi.haryeom.matching.manager.MatchingManager;
import com.ioi.haryeom.member.domain.Member;
import com.ioi.haryeom.member.exception.MemberNotFoundException;
import com.ioi.haryeom.member.exception.SubjectNotFoundException;
import com.ioi.haryeom.member.repository.MemberRepository;
import com.ioi.haryeom.tutoring.domain.Tutoring;
import com.ioi.haryeom.tutoring.repository.TutoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class MatchingService {


    private final SimpMessagingTemplate messagingTemplate;
    private MatchingManager matchingManager;
    private MemberRepository memberRepository;
    private ChatRoomRepository chatRoomRepository;
    private SubjectRepository subjectRepository;
    private TutoringRepository tutoringRepository;

    @Transactional
    public String createMatchingRequest(CreateMatchingRequest request, Long memberId) {

        ChatRoom chatRoom = findChatRoomById(request.getChatRoomId());

        Member member = findMemberById(memberId);

        Subject subject = findSubjectById(request.getSubjectId());

        String matchingId = IdGenerator.createMatchingId();
        matchingManager.addMatching(matchingId, request);

        CreateMatchingResponse response = CreateMatchingResponse.of(chatRoom, member, subject,
            request.getHourlyRate());

        // TODO: 알림 구현??
        log.info("[MATCHING REQUEST] chatRoomId : {}, matchingId : {}", chatRoom.getId(), matchingId);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId() + "/request", response);
        return matchingId;
    }

    @Transactional
    public Long respondToMatchingRequest(RespondToMatchingRequest request, Long memberId) {

        CreateMatchingRequest createdMatchingRequest = matchingManager.getMatchingRequestByMatchingId(
            request.getMatchingId());
        matchingManager.removeMatchingRequestByMatchingId(request.getMatchingId());

        ChatRoom chatRoom = findChatRoomById(createdMatchingRequest.getChatRoomId());

        Member member = findMemberById(memberId);

        Subject subject = findSubjectById(createdMatchingRequest.getSubjectId());

        // 과외 매칭 수락
        if (request.getIsAccepted()) {
            return processAcceptedMatching(chatRoom, member, subject, createdMatchingRequest, request);
        }

        // 과외 매칭 거절
        log.info("[MATCHING RESPONSE] REJECTED! chatRoomId : {}, matchingId : {}", chatRoom.getId(),
            request.getMatchingId());
        sendResponse(chatRoom, member, subject, request.getIsAccepted());
        return null;
    }

    private Long processAcceptedMatching(ChatRoom chatRoom, Member member, Subject subject,
        CreateMatchingRequest createdMatchingRequest, RespondToMatchingRequest request) {

        Tutoring tutoring = Tutoring.builder()
            .chatRoom(chatRoom)
            .subject(subject)
            .hourlyRate(createdMatchingRequest.getHourlyRate())
            .student(chatRoom.getStudentMember())
            .teacher(chatRoom.getTeacherMember())
            .build();
        Tutoring savedTutoring = tutoringRepository.save(tutoring);

        log.info("[MATCHING RESPONSE] ACCEPTED! chatRoomId : {}, matchingId : {}", chatRoom.getId(),
            request.getMatchingId());
        sendResponse(chatRoom, member, subject, request.getIsAccepted());

        return savedTutoring.getId();
    }

    private void sendResponse(ChatRoom chatRoom, Member member, Subject subject, Boolean isAccepted) {
        RespondToMatchingResponse response = RespondToMatchingResponse.of(chatRoom, member, subject, isAccepted);
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId() + "/response", response);
    }

    private Subject findSubjectById(Long subjectId) {
        return subjectRepository.findById(subjectId)
            .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));
    }

    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));
    }
}
