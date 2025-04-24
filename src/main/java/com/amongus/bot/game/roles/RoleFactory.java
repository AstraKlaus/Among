package com.amongus.bot.game.roles;

import com.amongus.bot.models.Config;
import com.amongus.bot.models.Player;
import com.amongus.bot.utils.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory class for creating and distributing roles.
 */
public class RoleFactory {
    private static final Logger log = LoggerFactory.getLogger(RoleFactory.class);
    private final SecurityManager securityManager;
    
    public RoleFactory(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    /**
     * Creates a role by its ID.
     */
    public Role createRole(String roleId) {
        switch (roleId) {
            case "crewmate":
                return new Crewmate(securityManager);
            case "impostor":
                return new Impostor(securityManager);
            default:
                throw new IllegalArgumentException("Unknown role ID: " + roleId);
        }
    }
    
    /**
     * Decodes a role callback and creates the corresponding role.
     */
    public Role createRoleFromCallback(String encodedCallback) {
        String roleId = securityManager.decodeRoleCallback(encodedCallback);
        return createRole(roleId);
    }
    
    /**
     * Assigns roles to players.
     * 
     * @param players List of players to assign roles to
     * @param impostorCount Number of impostors to assign
     * @return Map of player IDs to their assigned roles
     */
    public Map<Long, Role> assignRoles(List<Player> players, int impostorCount) {
        if (players.size() < Config.MIN_PLAYERS) {
            throw new IllegalArgumentException("Not enough players to assign roles");
        }
        
        if (impostorCount < Config.MIN_IMPOSTOR_COUNT || impostorCount > Config.MAX_IMPOSTOR_COUNT) {
            throw new IllegalArgumentException("Invalid impostor count: " + impostorCount);
        }
        
        // Adjust impostor count if there are too many impostors for player count
        if (impostorCount >= players.size() / 2) {
            impostorCount = Math.max(Config.MIN_IMPOSTOR_COUNT, players.size() / 3);
            log.info("Adjusted impostor count to {} based on player count", impostorCount);
        }
        
        // Shuffle players to randomize role assignment
        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);
        
        Map<Long, Role> roleAssignments = new HashMap<>();
        
        // Assign impostor roles first
        for (int i = 0; i < impostorCount && i < shuffledPlayers.size(); i++) {
            Player player = shuffledPlayers.get(i);
            Role role = new Impostor(securityManager);
            player.assignRole(role);
            roleAssignments.put(player.getUserId(), role);
        }
        
        // Assign crewmate roles to the rest
        for (int i = impostorCount; i < shuffledPlayers.size(); i++) {
            Player player = shuffledPlayers.get(i);
            Role role = new Crewmate(securityManager);
            player.assignRole(role);
            roleAssignments.put(player.getUserId(), role);
        }
        
        // Log role assignments (without revealing to players)
        logRoleAssignments(roleAssignments, players);
        
        return roleAssignments;
    }
    
    /**
     * Logs role assignments for debugging purposes.
     */
    private void logRoleAssignments(Map<Long, Role> roleAssignments, List<Player> players) {
        Map<String, List<String>> roleGroups = new HashMap<>();
        
        for (Player player : players) {
            Role role = roleAssignments.get(player.getUserId());
            if (role != null) {
                String roleName = role.getDisplayName();
                roleGroups.computeIfAbsent(roleName, k -> new ArrayList<>())
                        .add(player.getDisplayName());
            }
        }
        
        StringBuilder sb = new StringBuilder("Role assignments:\n");
        roleGroups.forEach((role, playerNames) -> {
            sb.append(role).append(": ")
                    .append(String.join(", ", playerNames))
                    .append("\n");
        });
        
        log.info(sb.toString());
    }
    
    /**
     * Gets the list of impostor players.
     */
    public List<Player> getImpostors(List<Player> players) {
        return players.stream()
                .filter(player -> player.getRole() != null && player.getRole().isImpostor())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the list of crewmate players.
     */
    public List<Player> getCrewmates(List<Player> players) {
        return players.stream()
                .filter(player -> player.getRole() != null && !player.getRole().isImpostor())
                .collect(Collectors.toList());
    }
} 