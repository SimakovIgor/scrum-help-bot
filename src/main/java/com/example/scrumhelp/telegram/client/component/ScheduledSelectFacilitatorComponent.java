package com.example.scrumhelp.telegram.client.component;

import com.example.scrumhelp.scrum.service.ChatMemberService;
import com.example.scrumhelp.scrum.service.ChatMemberServiceImpl;
import com.example.scrumhelp.telegram.client.service.ScrumHelpBot;
import com.example.scrumhelp.telegram.client.service.ScrumHelpBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@EnableScheduling
@Slf4j
public class ScheduledSelectFacilitatorComponent {
    private final ScrumHelpBot scrumHelpBot;
    private final ChatMemberServiceImpl chatMemberService;
    private final ScrumHelpBotService scrumHelpBotService;
    private final TaskExecutor taskExecutor;

    @Autowired
    public ScheduledSelectFacilitatorComponent(ScrumHelpBot scrumHelpBot,
                                               ChatMemberServiceImpl chatMemberService,
                                               ScrumHelpBotService scrumHelpBotService,
                                               TaskExecutor taskExecutor) {
        this.scrumHelpBot = scrumHelpBot;
        this.chatMemberService = chatMemberService;
        this.scrumHelpBotService = scrumHelpBotService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(cron = "0 45 10 ? * MON-FRI", zone = "Europe/Moscow")
    private void schedule() {
        chatMemberService.findChats().ifPresent(chats ->
                chats.forEach(chat ->
                        taskExecutor.execute(() -> {
                            try {
                                log.info("Running SelectFacilitatorTask for chat " + chat.getId());
                                scrumHelpBot.execute(scrumHelpBotService.sendSetFacilitatorSelectedMessage(chat.getId()));
                                log.info("Finished SelectFacilitatorTask for chat " + chat.getId());
                            } catch (TelegramApiException e) {
                                log.error(e.getMessage());
                            }
                        })
                )
        );

    }
}