package net.frozenorb.potpvp.profile;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Data;
import net.frozenorb.potpvp.killeffects.effects.KillEffect;
import net.frozenorb.potpvp.util.MongoUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Data
public class Profile {

    private final UUID uniqueId;

    private KillEffect killEffect = KillEffect.none;

	private int gamesWon = 0, winStreak = 0, highestWinStreak = 0, gamesPlayed = 0, kills = 0, deaths = 0, matchKills = 0, highestCombo = 0, loses = 0, stars = 0;

    public Profile(UUID uniqueId) {
        this.uniqueId = uniqueId;

        load();
    }

    public void load() {
        Document document = MongoUtils.getCollection("profiles").find(Filters.eq("uuid", uniqueId.toString())).first();

        if (document == null) return;

        gamesPlayed = document.getInteger("gamesPlayed");
        gamesWon = document.getInteger("gamesWon");
        kills = document.getInteger("kills");
        deaths = document.getInteger("deaths");
        loses = document.getInteger("loses");
        stars = document.getInteger("stars");
        highestWinStreak = document.getInteger("highestWinStreak");
        winStreak = document.getInteger("winStreak");
        highestCombo = document.getInteger("highestCombo");
        killEffect = KillEffect.valueOf(document.getString("killEffect"));

    }

    public void save() {
        CompletableFuture.runAsync(() -> {
            Document document = new Document();

            document.put("uuid", this.uniqueId.toString());
            document.put("gamesPlayed", gamesPlayed);
            document.put("gamesWon", gamesWon);
            document.put("kills", kills);
            document.put("deaths", deaths);
            document.put("highestCombo", highestCombo);
            document.put("loses", loses);
            document.put("stars", stars);
            document.put("highestWinStreak", highestWinStreak);
            document.put("winStreak", winStreak);
            document.put("killEffect", killEffect.getName());

            Bson filter = Filters.eq("uuid", uniqueId.toString());

            MongoUtils.getCollection("profiles").replaceOne(filter, document, new ReplaceOptions().upsert(true));
        });
    }
}
