package services;

import com.google.gson.JsonSyntaxException;
import http.ConnectionBuilder;
import model.bot.GregflixEntry;
import model.omdb.OMDBMovieResponse;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

public class GregflixService {

    private MessageService messageService;
    ConnectionBuilder connectionBuilder;
    DataService dataService;
    ResourceBundle resourceBundle;

    public GregflixService(Properties properties, MessageService messageService, DataService dataService) {
        this.messageService = messageService;
        this.dataService = dataService;
        connectionBuilder = new ConnectionBuilder(properties);
    }

    public String handleButtonEvent(ButtonInteractionEvent buttonInteractionEvent, String locale) {
        resourceBundle = ResourceBundle.getBundle("localization", new Locale(locale));
        String buttonId = buttonInteractionEvent.getButton().getId();

        if ("falseItem".equals(buttonId)) {
            buttonInteractionEvent.getMessage().delete().queue();
            return resourceBundle.getString("gregflix.cancel");
        } else {
            buttonInteractionEvent.getMessage().delete().queue();
            try {
                dataService.addGregflixEntry(buttonId.split("--")[1], buttonId.split("--")[2], buttonId.split("--")[3]);
                messageService.contactGreg(buttonInteractionEvent.getButton().getId(), dataService.getDiscordIdForUsername("jay_th"), buttonInteractionEvent.getJDA());
                return resourceBundle.getString("gregflix.confirm");
            } catch (SQLException ex) {
                System.out.println("[CSBot - CsStatsService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] SQLException thrown: " + ex.getMessage());
                return resourceBundle.getString("error.majorerror");
            }
        }
    }

    public void handleGregflixEvent(MessageReceivedEvent event, String locale, PrivateChannel privateChannel) {
        resourceBundle = ResourceBundle.getBundle("localization", new Locale(locale));

        try {
            if(dataService.hasGregflix(privateChannel.getUser().getName(), privateChannel.getUser().getId())) {
                OMDBMovieResponse omdbMovieResponse = connectionBuilder.fetchMovieDetails(event.getMessage().getContentDisplay());
                EmbedBuilder embedBuilder = new EmbedBuilder();

                if ("True".equals(omdbMovieResponse.getResponse())) {
                    GregflixEntry gregflixEntry = dataService.getGregflixEntryForOMDBMovieResponse(omdbMovieResponse);
                    if (gregflixEntry == null) {
                        embedBuilder.setTitle(omdbMovieResponse.getTitle());
                        embedBuilder.setDescription(resourceBundle.getString("gregflix.description"));
                        embedBuilder.addField(new MessageEmbed.Field("Type", omdbMovieResponse.getType(), true));
                        if ("series".equals(omdbMovieResponse.getType())) {
                            embedBuilder.addField(new MessageEmbed.Field("Total Seasons", Integer.toString(omdbMovieResponse.getTotalSeasons()), false));
                        } else {
                            embedBuilder.addField(new MessageEmbed.Field("Runtime", omdbMovieResponse.getRuntime(), false));
                        }
                        embedBuilder.addField(new MessageEmbed.Field("Genre", omdbMovieResponse.getGenre(), false));
                        embedBuilder.addField(new MessageEmbed.Field("IMDB ID", omdbMovieResponse.getImdbID(), false));
                        if(!StringUtils.isEmpty(omdbMovieResponse.getPoster())) {
                            embedBuilder.setImage(omdbMovieResponse.getPoster());
                        }
                        messageService.sendGregflixEmbedMessage(privateChannel, embedBuilder, locale, false, omdbMovieResponse);
                    } else {
                        if("series".equals(omdbMovieResponse.getType())) {
                            if(!gregflixEntry.isUploaded()) {
                                messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("info.showrequested")).addField("IMDB ID", omdbMovieResponse.getImdbID(), false), locale, true, null);
                            } else {
                                messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("info.showexists")).setDescription(resourceBundle.getString("info.showexists.description")).addField("IMDB ID", omdbMovieResponse.getImdbID(), false), locale, true, omdbMovieResponse);
                            }
                        } else {
                            messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("info.movieexists")).addField("IMDB ID", omdbMovieResponse.getImdbID(), false), locale, true, null);
                        }
                    }
                } else {
                    messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("info.nomoviefound")), locale, true, null);
                }
            }
        } catch (IOException ex) {
            System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] IOException thrown: " + ex.getMessage());
            messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("error.majorerror")), locale, true, null);
        } catch (InterruptedException ex) {
            System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] InterruptedException thrown: " + ex.getMessage());
            messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("error.majorerror")), locale, true, null);
        } catch (SQLException ex) {
            System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] SQLException thrown: " + ex.getMessage());
            messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("error.majorerror")), locale, true, null);
        } catch (JsonSyntaxException ex) {
            System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] JsonSyntaxException thrown: " + ex.getMessage());
            messageService.sendGregflixEmbedMessage(privateChannel, new EmbedBuilder().setTitle(resourceBundle.getString("error.majorerror")), locale, true, null);
        }
    }

    public void handleGregflixReactionEvent(MessageReactionAddEvent messageReactionAddEvent, String locale) {
        resourceBundle = ResourceBundle.getBundle("localization", new Locale(locale));

        try {
            if (dataService.isGreg(messageReactionAddEvent.getUser().getId())) {
                messageReactionAddEvent.getChannel().retrieveMessageById(messageReactionAddEvent.getMessageId()).queue((message -> {
                    try {
                        String[] splitMessage = message.getContentDisplay().split("--");

                        switch (messageReactionAddEvent.getReaction().getEmoji().getAsReactionCode()) {
                            case "\uD83E\uDD47":
                                dataService.updateGregflixEntryToUploaded(splitMessage[3]);
                                messageService.sendPrivateMessageToUser(messageReactionAddEvent.getJDA(), resourceBundle.getString("gregflix.requestedDone").replace("%s", splitMessage[1]), dataService.getDiscordIdForUsername(splitMessage[0]));
                                System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] Informed " + splitMessage[0] + " about successful " + splitMessage[3] + ".");
                                message.delete().queue();
                                break;
                            case "\uD83E\uDD48":
                                dataService.updateGregflixEntryToUploaded(splitMessage[3]);
                                messageService.sendPrivateMessageToUser(messageReactionAddEvent.getJDA(), resourceBundle.getString("gregflix.requestedPartial").replace("%s", splitMessage[1]), dataService.getDiscordIdForUsername(splitMessage[0]));
                                System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] Informed " + splitMessage[0] + " about partial " + splitMessage[3] + ".");
                                message.delete().queue();
                                break;
                            case "\uD83E\uDD49":
                                dataService.removeGregflixEntry(splitMessage[3]);
                                messageService.sendPrivateMessageToUser(messageReactionAddEvent.getJDA(), resourceBundle.getString("gregflix.requestedFailed").replace("%s", splitMessage[1]), dataService.getDiscordIdForUsername(splitMessage[0]));
                                System.out.println("[CSBot - GregflixService - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy - HH:mm:ss")) + "] Informed " + splitMessage[0] + " about failed " + splitMessage[3] + ".");
                                message.delete().queue();
                                break;
                        }
                    } catch (SQLException ex) {
                        messageReactionAddEvent.getChannel().sendMessage("error in sending private message").queue();
                    }
                }));
            }
        } catch (SQLException ex) {
            messageReactionAddEvent.getChannel().sendMessage("error").queue();
        }
    }
}