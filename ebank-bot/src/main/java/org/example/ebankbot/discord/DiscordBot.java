package org.example.ebankbot.discord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.ebankbot.agents.EbankAIAgent;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiscordBot extends ListenerAdapter {

    @Value("${discord.token:}")
    private String discordToken;

    private final EbankAIAgent aiAgent;
    private JDA jda;

    public DiscordBot(EbankAIAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @PostConstruct
    public void startBot() {
        if (discordToken != null && !discordToken.isBlank() && !discordToken.startsWith("${")) {
            try {
                this.jda = JDABuilder.createDefault(discordToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                    .addEventListeners(this)
                    .build();
                System.out.println(">>> [DiscordBot] Discord bot connected successfully!");
            } catch (Exception e) {
                System.err.println(">>> [DiscordBot] Could not connect to Discord: " + e.getMessage());
            }
        } else {
            System.out.println(">>> [DiscordBot] DISCORD_TOKEN is not configured. Discord bot is inactive.");
        }
    }

    @PreDestroy
    public void stopBot() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages sent by bots (including our own bot)
        if (event.getAuthor().isBot()) {
            return;
        }

        String userMessage = event.getMessage().getContentDisplay();
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        MessageChannel channel = event.getChannel();

        // Send Discord typing indicator
        channel.sendTyping().queue();

        try {
            // Forward prompt to Spring AI agent
            String answer = aiAgent.chat(new Prompt(userMessage), event.getAuthor().getId());
            if (answer == null || answer.isBlank()) {
                answer = "JE NE SAIS PAS";
            }

            // Respect Discord 2000-character limit per message
            if (answer.length() > 2000) {
                for (int i = 0; i < answer.length(); i += 2000) {
                    channel.sendMessage(answer.substring(i, Math.min(answer.length(), i + 2000))).queue();
                }
            } else {
                channel.sendMessage(answer).queue();
            }
        } catch (Exception e) {
            channel.sendMessage("Erreur: " + e.getMessage()).queue();
        }
    }
}
