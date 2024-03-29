package com.example.scrumhelp.scrum.service;

import com.example.scrumhelp.scrum.model.Chat;
import com.example.scrumhelp.scrum.model.ChatMember;
import com.example.scrumhelp.scrum.model.Member;
import com.example.scrumhelp.telegram.client.exception.NotFoundException;
import com.example.scrumhelp.telegram.client.service.ScrumHelpBotService;
import eye2web.modelmapper.ModelMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.Optional;

import static com.example.scrumhelp.telegram.client.enums.DailyReminderState.*;
import static com.example.scrumhelp.telegram.client.enums.Emoji.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
public class ScrumHelpBotServiceTest {
    @Mock
    private ChatService chatService;
    @Mock
    private ChatMemberService chatMemberService;
    @Mock
    private MemberService memberService;
    @Mock
    private ModelMapper modelMapper;
    @InjectMocks
    private ScrumHelpBotService scrumHelpBotService;

    @Test
    void registerNewUserForNewChatShouldBeSuccess() {
        Member member = new Member();
        member.setId(1L);
        member.setUserName("Test User");

        when(memberService.findOrCreate(any())).thenReturn(member);
        when(chatMemberService.findChatMemberForChat(any(), any())).thenReturn(Optional.empty());
        when(chatService.findOrCreate(any())).thenReturn(new Chat(1L));

        SendMessage sendMessage = scrumHelpBotService.sendRegisterUserMessage(1L, new User());

        assertEquals("Пользователь Test User успешно зарегистрирован!" + PartyingFace, sendMessage.getText());
    }

    @Test
    void registerExistingUserShouldBeError() {
        Member member = new Member();
        member.setUserName("Test Member");
        Optional<ChatMember> memberOptional =
                Optional.of(new ChatMember(member, new Chat(1L), false));

        when(memberService.findOrCreate(any())).thenReturn(member);
        when(chatMemberService.findChatMemberForChat(any(), any())).thenReturn(memberOptional);

        SendMessage sendMessage = scrumHelpBotService.sendRegisterUserMessage(1L, new User());

        assertEquals("Пользователь Test Member уже зарегистрирован!" + OkHad, sendMessage.getText());
    }

    @Test
    void remindDailyMessageWithEmptyFacilitatorShouldBeNotAssign() {
        when(chatMemberService.findFacilitatorForChat(1L, true)).thenReturn(Optional.empty());
        SendMessage sendMessage = scrumHelpBotService.sendRemindDailyMessage(1L);
        assertTrue(sendMessage.getText().contains("Не назначен!"));
    }

    @Test
    void remindDailyMessageWithExistingFacilitatorShouldContainsName() {
        Member member = new Member();
        member.setUserName("Test Member");
        Optional<ChatMember> chatMemberOptional = Optional.of(
                new ChatMember(member, new Chat(1L), true)
        );

        when(chatMemberService.findFacilitatorForChat(1L, true)).thenReturn(chatMemberOptional);
        SendMessage sendMessage = scrumHelpBotService.sendRemindDailyMessage(1L);
        assertTrue(sendMessage.getText().contains("Test Member"));
    }

    @Test
    void whenMembersExistsUserListMessageShouldNotBeEmpty() {
        Member member1 = new Member();
        member1.setUserName("UserName1");
        Member member2 = new Member();
        member2.setUserName("UserName2");
        Member member3 = new Member();
        member3.setUserName("UserName3");

        Chat chat = new Chat(1L);

        List<ChatMember> chatMemberList = List.of(
                new ChatMember(member1, chat, false),
                new ChatMember(member2, chat, false),
                new ChatMember(member3, chat, false)
        );

        when(chatMemberService.findChatMembers(any())).thenReturn(chatMemberList);

        SendMessage sendMessage = scrumHelpBotService.sendUserListMessage(1L);
        assertEquals("Зарегистрированные пользователи:\nUserName1\nUserName2\nUserName3\n", sendMessage.getText());
    }

    @Test
    void whenMembersDoesntExistsUserListMessageShouldNotBeEmpty() {
        when(chatMemberService.findChatMembers(any())).thenReturn(List.of());

        SendMessage sendMessage = scrumHelpBotService.sendUserListMessage(1L);
        assertEquals("Зарегистрированные пользователи:\nСписок пуст", sendMessage.getText());
    }

    @Test
    void sendHelpMessageShouldBeSuccessful() {
        SendMessage sendMessage = scrumHelpBotService.sendHelpMessage(1L);
        assertEquals("Список возможных команд:\n" +
                        "/register - регистрация пользователя\n" +
                        "/getUserList - список участников\n" +
                        "/setFacilitator - выбор фасилитатора\n" +
                        "/luckyFacilitator - случайный выбор фасилитатора\n" +
                        "/enableDailyReminder - включить напоминание о дейли\n" +
                        "/disableDailyReminder- выключить напоминание о дейли\n",
                sendMessage.getText()
        );
    }

    @Test
    void removeMarkupFromPreviousMessageShouldBeSuccessful() {
        EditMessageReplyMarkup editMessageReplyMarkup = scrumHelpBotService.removeMarkupFromPreviousMessage(1L, 1);
        assertAll(
                () -> assertEquals("1", editMessageReplyMarkup.getChatId()),
                () -> assertEquals(1, editMessageReplyMarkup.getMessageId()),
                () -> assertNull(editMessageReplyMarkup.getReplyMarkup())
        );
    }

    @Test
    void sendDailyReminderMessageTurnedOffCase() {
        SendMessage sendMessage = scrumHelpBotService.sendDailyReminderMessage(1L, TurnedOff);
        assertEquals("Напоминание о дейли выключено!" + CrossMark, sendMessage.getText());
    }

    @Test
    void sendDailyReminderMessageNotSetCase() {
        SendMessage sendMessage = scrumHelpBotService.sendDailyReminderMessage(1L, NotSet);
        assertEquals("Напоминание о дейли еще не установлено!" + RedExclamation +
                        "\nДля включения напоминания /enableDailyReminder",
                sendMessage.getText()
        );
    }

    @Test
    void sendDailyReminderMessageTurnedOnCase() {
        SendMessage sendMessage = scrumHelpBotService.sendDailyReminderMessage(1L, TurnedOn);
        assertEquals("Напоминание о дейли включено!" + CheckMarkButton + "\n" +
                        "Для выключения напоминания:\n/disableDailyReminder",
                sendMessage.getText());
    }

    @Test
    void sendDailyReminderMessageAlreadySetCase() {
        SendMessage sendMessage = scrumHelpBotService.sendDailyReminderMessage(1L, AlreadySet);
        assertEquals("Напоминание о дейли уже установлено!" + RedExclamation,
                sendMessage.getText()
        );
    }

    @Test
    void selectFacilitatorMessageShouldHaveChatMembers() {
        Member member1 = new Member();
        member1.setId(1L);
        member1.setUserName("member1");
        Member member2 = new Member();
        member2.setId(2L);
        member2.setUserName("member2");
        Member member3 = new Member();
        member3.setId(3L);
        member3.setUserName("member3");

        Chat chat = new Chat(1L);

        when(chatMemberService.findChatMembers(any())).thenReturn(List.of(
                new ChatMember(member1, chat, true),
                new ChatMember(member2, chat, false),
                new ChatMember(member3, chat, false)
        ));

        SendMessage sendMessage = scrumHelpBotService.sendSelectFacilitatorMessage(1L);

        InlineKeyboardMarkup inlineKeyboardMarkup = (InlineKeyboardMarkup) sendMessage.getReplyMarkup();
        assertAll(
                () -> assertEquals("Выбери следующего фасилитатора:" + PoliceOfficer, sendMessage.getText()),
                () -> assertEquals("/newFacilitator 1", inlineKeyboardMarkup.getKeyboard().get(0).get(0).getCallbackData()),
                () -> assertEquals("/newFacilitator 2", inlineKeyboardMarkup.getKeyboard().get(0).get(1).getCallbackData()),
                () -> assertEquals("/newFacilitator 3", inlineKeyboardMarkup.getKeyboard().get(1).get(0).getCallbackData())
        );
    }

    @Test
    void selectFacilitatorMessageShouldBeEmpty() {
        when(chatMemberService.findChatMembers(any())).thenReturn(List.of());
        SendMessage sendMessage = scrumHelpBotService.sendSelectFacilitatorMessage(1L);
        assertEquals("Список зарегистрированных пользователей пуст", sendMessage.getText());
    }

    @Test
    void sendSetFacilitatorSelectedMessageWithNotEmptyCurrentFacilitatorShouldBeSuccess1arg() {
        Chat chat = new Chat(1L);
        Member member1 = new Member();
        member1.setId(1L);
        Member member2 = new Member();
        member2.setId(2L);

        when(chatMemberService.findChatMembersExceptFacilitator(any())).thenReturn(
                List.of(
                        new ChatMember(member1, chat, false),
                        new ChatMember(member2, chat, false)
                )
        );

        Member facilitator = new Member();
        facilitator.setId(10L);
        when(chatMemberService.changeAndGetNewFacilitatorForChat(any(), any())).thenReturn(
                Optional.of(new ChatMember(facilitator, chat, true))
        );

        SendMessage sendMessage = scrumHelpBotService.sendSetFacilitatorSelectedMessage(1L);

        assertFalse(sendMessage.getText().isEmpty());
    }

    @Test
    void sendSetFacilitatorSelectedMessageWithEmptyCurrentFacilitatorShouldBeException1arg() {
        Chat chat = new Chat(1L);
        Member member1 = new Member();
        member1.setId(1L);
        Member member2 = new Member();
        member2.setId(2L);

        when(chatMemberService.findChatMembersExceptFacilitator(any()))
                .thenReturn(List.of(
                                new ChatMember(member1, chat, false),
                                new ChatMember(member2, chat, false)
                        )
                );

        when(chatMemberService.changeAndGetNewFacilitatorForChat(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> scrumHelpBotService.sendSetFacilitatorSelectedMessage(1L));
    }

    @Test
    void sendSetFacilitatorSelectedMessageWithNotEmptyCurrentFacilitatorShouldBeSuccess2arg() {
        CallbackQuery callbackQueryMock = mock(CallbackQuery.class);
        when(callbackQueryMock.getData()).thenReturn("id 1");
        Update updateMock = mock(Update.class);
        when(updateMock.getCallbackQuery()).thenReturn(callbackQueryMock);

        Member memberFrom = new Member();
        memberFrom.setId(1L);
        when(memberService.findOrCreate(any())).thenReturn(memberFrom);

        Member facilitator = new Member();
        facilitator.setId(10L);
        when(chatMemberService.changeAndGetNewFacilitatorForChat(1L, 1L))
                .thenReturn(Optional.of(new ChatMember(facilitator, new Chat(1L), true))
                );

        SendMessage sendMessage = scrumHelpBotService.sendSetFacilitatorSelectedMessage(1L, updateMock);

        assertEquals("1 выбрал следующего фасилитатора: 10", sendMessage.getText());
    }

    @Test
    void sendSetFacilitatorSelectedMessageWithEmptyCurrentFacilitatorShouldBeException2arg() {
        CallbackQuery callbackQueryMock = mock(CallbackQuery.class);
        when(callbackQueryMock.getData()).thenReturn("id 1");
        Update updateMock = mock(Update.class);
        when(updateMock.getCallbackQuery()).thenReturn(callbackQueryMock);

        Member memberFrom = new Member();
        memberFrom.setId(1L);
        when(memberService.findOrCreate(any())).thenReturn(memberFrom);

        when(chatMemberService.changeAndGetNewFacilitatorForChat(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> scrumHelpBotService.sendSetFacilitatorSelectedMessage(1L, updateMock));
    }
}
