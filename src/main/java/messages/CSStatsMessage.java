package messages;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import model.ResponseData;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class CSStatsMessage extends ListenerAdapter {

    Properties properties;

    String requestedUser;

    public CSStatsMessage(Properties properties) {
        super();

        this.properties = properties;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        if ("stats".equals(event.getName())) {
            try {
                requestedUser = event.getOption("player").getAsString().toLowerCase();
                ResponseData responseData;

                switch (requestedUser) {
                    case "aatha":
                    case "aathavan":
                    case "doge":
                        responseData = getUserAndStats("76561198077352267");
                        break;
                    case "dario":
                    case "däse":
                        responseData = getUserAndStats("76561198213130649");
                        break;
                    case "janes":
                    case "jay":
                    case "grey":
                        responseData = getUserAndStats("76561198014462666");
                        break;
                    case "juan":
                    case "juanita":
                        responseData = getUserAndStats("76561198098219020");
                        break;
                    case "korunde":
                    case "koray":
                    case "ossas":
                        responseData = getUserAndStats("76561198071064798");
                        break;
                    case "nabil":
                    case "drifter":
                        responseData = getUserAndStats("76561198088520949");
                        break;
                    case "nassim":
                        responseData = getUserAndStats("76561198203636285");
                        break;
                    case "nici":
                    case "nigglz":
                    case "n'lölec":
                        responseData = getUserAndStats("76561198401419666");
                        break;
                    case "ravi":
                    case "vi24":
                        responseData = getUserAndStats("76561198071074164");
                        break;
                    case "pavi":
                    case "seraph":
                        responseData = getUserAndStats("76561198102224384");
                        break;
                    case "sani":
                    case "baka":
                    case "mugiwarabaka":
                        responseData = getUserAndStats("76561197984892194");
                        break;
                    case "vantriko":
                    case "v4ntr1ko":
                    case "enrico":
                        responseData = getUserAndStats("76561198316963738");
                        break;
                    default:
                        responseData = getUserAndStats(requestedUser);
                        break;
                }

                requestedUser = responseData.getSteamUserInfo().getPlayers().get(0).getPersonaname();
                event.reply(responseData.returnBasicInfo()).queue();

            } catch (InterruptedException ex) {
                //TODO: return a meaningful message.
            } catch (IOException ex) {
                //TODO: return a meaningful message.
            } catch (NullPointerException | JsonSyntaxException ex) {
                event.reply("Für " + requestedUser + " können keine Stats geladen werden. (Steam Privacy Settings?)").queue();
            }
        }
    }

    private ResponseData getUserAndStats(String steamID) throws InterruptedException, IOException {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request;
        ResponseData responseData;

        request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" + properties.get("steam.api") + "&steamids=" + steamID ))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        responseData = new Gson().fromJson(response.body(), ResponseData.class);

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v0002/?key=" + properties.get("steam.api") + "&appid=730&steamid=" + steamID ))
                .build();
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        responseData.setPlayerstats(new Gson().fromJson(response.body(), ResponseData.class).getPlayerstats());

        return responseData;
    }
}
