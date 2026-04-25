package com.example.Controller;

import com.example.world.GameWorld;
import com.example.model.Player;
import com.example.model.Cell;
import org.springframework.web.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class GameController {

    private final GameWorld gameWorld;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public GameController() {
        this.gameWorld = new GameWorld();
        initGame();
    }

    private void initGame() {
        gameWorld.createWorld(10, 10);
        Player player1 = new Player("Красный Лорд");
        Player player2 = new Player("Синий Барон");
        gameWorld.addPlayer(player1, 2, 2);
        gameWorld.addPlayer(player2, 7, 7);
        gameWorld.start();
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("players", gameWorld.getPlayers().values());
        return "index";
    }

    @GetMapping("/api/map")
    @ResponseBody
    public Map<String, Object> getMap() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> cells = new ArrayList<>();

        for (Cell cell : gameWorld.getCells().values()) {
            Map<String, Object> cellData = new HashMap<>();
            cellData.put("x", cell.getX());
            cellData.put("y", cell.getY());
            cellData.put("owner", cell.getOwnerId());
            cellData.put("troops", cell.getTroopsCount());
            cellData.put("terrain", cell.getTerrain().name());
            cells.add(cellData);
        }

        result.put("cells", cells);
        result.put("players", gameWorld.getPlayers().values());
        return result;
    }

    @PostMapping("/api/attack")
    @ResponseBody
    public Map<String, String> attack(@RequestBody AttackRequest request) {
        String fromId = request.fromX + ":" + request.fromY;
        String toId = request.toX + ":" + request.toY;

        Cell fromCell = gameWorld.getCell(fromId);
        if (fromCell != null && fromCell.getOwnerId() != null) {
            gameWorld.scheduleAttack(fromId, toId, request.troops, fromCell.getOwnerId());
            return Map.of("status", "ok", "message", "Атака запланирована!");
        }
        return Map.of("status", "error", "message", "Неверная клетка!");
    }

    @GetMapping("/api/events")
    public SseEmitter subscribeToEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(UUID.randomUUID().toString(), emitter);
        return emitter;
    }

    static class AttackRequest {
        public int fromX, fromY, toX, toY, troops;
    }
}