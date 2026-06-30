package com.example.aiwerewolf.aiinfra.prompt;

import com.example.aiwerewolf.game.view.GameView;
import com.example.aiwerewolf.game.view.MemoryView;
import com.example.aiwerewolf.game.view.PlayerView;
import com.example.aiwerewolf.game.view.SpeechView;
import com.example.aiwerewolf.game.view.VoteView;
import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 面向模型的稳定上下文。业务内部 UUID 不进入 Prompt，模型只使用 seat-N 引用玩家。
 */
public record AgentPromptContext(
        String roomName,
        String status,
        String phase,
        int roundNumber,
        String viewer,
        @Nullable String ownRole,
        @Nullable String ownCamp,
        List<PromptPlayer> players,
        List<PromptMemory> memories,
        List<PromptSpeech> speeches,
        List<PromptVote> votes
) {
    public static AgentPromptContext from(GameView view) {
        Map<String, PlayerView> playersById = view.players().stream()
                .collect(Collectors.toMap(PlayerView::id, Function.identity()));
        Function<String, String> ref = playerId -> playerRef(playersById.get(playerId));
        Function<String, String> label = playerId -> playerLabel(playersById.get(playerId));
        return new AgentPromptContext(
                view.roomName(),
                view.status().name(),
                view.phase().name(),
                view.roundNumber(),
                ref.apply(view.viewerPlayerId()),
                view.ownRole() == null ? null : view.ownRole().name(),
                view.ownCamp() == null ? null : view.ownCamp().name(),
                view.players().stream()
                        .sorted(Comparator.comparingInt(PlayerView::seatNumber))
                        .map(PromptPlayer::from)
                        .toList(),
                view.memories().stream()
                        .map(memory -> PromptMemory.from(memory, playersById))
                        .toList(),
                view.speeches().stream()
                        .sorted(Comparator.comparingInt(SpeechView::roundNumber)
                                .thenComparingInt(speech -> seatNumber(playersById.get(speech.playerId()))))
                        .map(speech -> PromptSpeech.from(speech, playersById.get(speech.playerId())))
                        .toList(),
                view.votes().stream()
                        .map(vote -> PromptVote.from(vote, ref, label))
                        .toList());
    }

    public static String playerRef(@Nullable PlayerView player) {
        return player == null ? "unknown" : "seat-" + player.seatNumber();
    }

    private static String playerLabel(@Nullable PlayerView player) {
        return player == null ? "未知玩家" : player.seatNumber() + "号 " + player.name();
    }

    private static int seatNumber(@Nullable PlayerView player) {
        return player == null ? Integer.MAX_VALUE : player.seatNumber();
    }

    private static String replaceIds(String content, Map<String, PlayerView> playersById) {
        String sanitized = content;
        for (Map.Entry<String, PlayerView> entry : playersById.entrySet()) {
            sanitized = sanitized.replace(entry.getKey(), playerLabel(entry.getValue()));
        }
        return sanitized;
    }

    public record PromptPlayer(
            String playerRef,
            int seatNumber,
            String name,
            boolean alive,
            boolean canSpeak,
            boolean canVote,
            @Nullable String visibleRole,
            @Nullable String visibleCamp
    ) {
        private static PromptPlayer from(PlayerView player) {
            return new PromptPlayer(
                    AgentPromptContext.playerRef(player),
                    player.seatNumber(),
                    player.name(),
                    player.alive(),
                    player.canSpeak(),
                    player.canVote(),
                    player.role() == null ? null : player.role().name(),
                    player.camp() == null ? null : player.camp().name());
        }
    }

    public record PromptMemory(int roundNumber, String phase, String scope, String eventType, String content) {
        private static PromptMemory from(MemoryView memory, Map<String, PlayerView> playersById) {
            return new PromptMemory(
                    memory.roundNumber(),
                    memory.phase().name(),
                    memory.scope().name(),
                    memory.eventType(),
                    replaceIds(memory.content(), playersById));
        }
    }

    public record PromptSpeech(
            String playerRef,
            int seatNumber,
            String playerName,
            int roundNumber,
            String speech,
            @Nullable String claimedRole
    ) {
        private static PromptSpeech from(SpeechView speech, @Nullable PlayerView player) {
            return new PromptSpeech(
                    AgentPromptContext.playerRef(player),
                    AgentPromptContext.seatNumber(player),
                    player == null ? "未知玩家" : player.name(),
                    speech.roundNumber(),
                    speech.content(),
                    speech.claimedRole());
        }
    }

    public record PromptVote(String voter, String target, String reason) {
        private static PromptVote from(VoteView vote,
                                       Function<String, String> ref,
                                       Function<String, String> label) {
            return new PromptVote(
                    ref.apply(vote.voterPlayerId()) + " (" + label.apply(vote.voterPlayerId()) + ")",
                    ref.apply(vote.targetPlayerId()) + " (" + label.apply(vote.targetPlayerId()) + ")",
                    vote.reason());
        }
    }
}
